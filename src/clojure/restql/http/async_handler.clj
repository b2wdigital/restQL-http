(ns restql.http.async-handler
  (:require [compojure.core :as c]
            [compojure.route :as route]
            [compojure.response :refer [Renderable]]
            [clojure.walk :refer [keywordize-keys stringify-keys]]
            [clojure.tools.logging :as log]
            [clojure.tools.reader.edn :as edn]
            [environ.core :refer [env]]
            [manifold.stream :as m-stream]
            [clojure.core.async :refer [chan go go-loop >! >!! <! alt! timeout]]
            [ring.middleware.params :refer [wrap-params]]
            [slingshot.slingshot :refer [try+]]
            [restql.core.api.restql :as restql]
            [restql.core.response.headers :as response-headers]
            [restql.core.encoders.core :refer [base-encoders]]
            [restql.http.request.mappings :as request-mappings]
            [restql.http.request.queries :as request-queries]
            [restql.http.request-util :as util]
            [restql.http.database.core :as dbcore]
            [restql.http.cache :as cache]
            [restql.http.exception-handler :refer [wrap-exception-handling]]
            [restql.http.plugin.core :as plugin]
            [restql.http.response :as resp]))

(def default-values {:query-global-timeout 30000})

(defn get-default [key]
  (if (contains? env key) (read-string (env key)) (default-values key)))

(defn process-query [query query-opts tenant]
  (restql/execute-query-channel :mappings (request-mappings/get-mappings tenant)
                                :encoders base-encoders
                                :query query
                                :query-opts query-opts))

(defn make-headers [interpolated-query result]
  (->
    (response-headers/get-response-headers interpolated-query result)
    (into {"Content-Type" "application/json"})
    (stringify-keys)))

(defn handle-request [req result-ch error-ch]
  (try+
    (let [headers {"Content-Type" "application/json"}
          req-headers (into {"restql-query-control" "ad-hoc"} (:headers req))
          params (-> req :query-params keywordize-keys)

          ; Retrieving tenant (env is always prioritized)
          env-tenant (some-> env :tenant)
          tenant (if (nil? env-tenant) (some-> params :tenant) env-tenant)

          query-entry (util/parse-req req)
          query (->> query-entry (util/merge-headers req-headers))
          debugging (-> req :query-params (get "_debug") boolean)

          base-opts {:debugging debugging
                     :tenant    tenant
                     :info      {:type :ad-hoc}}

          forward-params (if (nil? (:forward-prefix env))
                           {}
                           (into {} (filter (partial util/is-contextual? (:forward-prefix env)) params)))
          opts (into {:forward-params forward-params} base-opts)

          [query-ch exception-ch] (process-query query opts tenant)
          timeout-ch (timeout (get-default :query-global-timeout))]
      (log/debug "starting request handler")
      (go
        (alt!
          timeout-ch ([] (log/warn "request handler timed out") (>! error-ch {:status 500 :body "Request timed out"}))
          exception-ch ([err] (>! error-ch (util/error-output err)))
          query-ch ([result]
                       (log/debug " finishing request handler")
                       (if (nil? result)
                         (do (>! result-ch {:body    nil
                                            :headers nil
                                            :status  500}))
                         (do (>! result-ch {:body    (util/format-response-body result)
                                            :headers (make-headers query-entry result)
                                            :status  (util/calculate-response-status-code result)}))
                       )))))
    (catch [:type :validation-error] {:keys [message]}
      (go (>! error-ch (util/json-output 400 {:error "VALIDATION_ERROR" :message message}))))
    (catch [:type :parse-error] {:keys [line column]}
      (go (>! error-ch (util/json-output 400 {:error "PARSE_ERROR" :line line :column column}))))
    (catch Exception e (.printStackTrace e)
      (go (>! error-ch (util/json-output 400 {:error "UNKNOWN_ERROR" :message (.getMessage e)}))))))

(defn- parse-query [req]
  (try+
    {:status 200 :body (util/parse-req req)}
  (catch [:type :parse-error] {:keys [line column reason]}
    {:status 400 :body (str "Parsing error in line " line ", column " column "\n" reason)})))

(defn run-query [req]
  (let [time-before (System/currentTimeMillis)
        result-ch (chan)
        error-ch (chan)]
    (handle-request req result-ch error-ch)
    (m-stream/take!
      (m-stream/->source
        (go
          (alt!
            result-ch ([result]
                        (log/debug {:time    (- (System/currentTimeMillis) time-before)
                                :success true}
                          "restQL Query finished")
                        result)
            error-ch ([err]
                        (log/error {:time    (- (System/currentTimeMillis) time-before)
                                :success false}
                          "restQL Query finished")
                        err)))))))

(defn run-saved-query [req]
  (log/debug "Trying to retrieve query" (-> req :params :id))
  (try+
   (let [id (-> req :params :id)
         query-ns (-> req :params :namespace)
         rev (-> req :params :rev read-string)
         headers (-> req :headers)
         req-headers (into {"restql-query-control"   "saved"
                            "restql-query-namespace" query-ns
                            "restql-query-id"        id
                            "restql-query-revision"  rev} (:headers req))
         params (-> req :query-params keywordize-keys)
         debugging (-> params (get :_debug false) boolean)

          ; Retrieving tenant (env is always prioritized)
         env-tenant (some-> env :tenant)
         tenant (if (nil? env-tenant) (some-> params :tenant) env-tenant)
         forward-params (if (nil? (:forward-prefix env))
                          {}
                          (into {} (filter (partial util/is-contextual? (:forward-prefix env)) params)))
         opts {:debugging      debugging
               :tenant         tenant
               :forward-params forward-params
               :info           {:type      :saved
                                :namespace query-ns
                                :id        id
                                :revision  rev}}

         query-entry (request-queries/get-query query-ns id rev)
         context (into (:headers req) (:query-params req))
         interpolated-query (util/parse query-entry context)
         query (util/merge-headers req-headers interpolated-query)
         time-before (System/currentTimeMillis)
         [result-ch error-ch] (process-query query opts tenant)]
     (log/debug "Query" query-ns "/" id "rev" rev "retrieved")
     (m-stream/take!
      (m-stream/->source
       (go
         (alt!
           result-ch ([result]
                      (log/debug {:time    (- (System/currentTimeMillis) time-before)
                                  :success true}
                                  "restQL Query finished")
                                (if (nil? result)
                                  {:headers nil :status 500 :body nil}
                                  {:headers (make-headers interpolated-query result)
                                   :status  (util/calculate-response-status-code result)
                                   :body    (util/format-response-body result)}))
              error-ch ([err]
                          (log/error {:time    (- (System/currentTimeMillis) time-before)
                                  :success false}
                                "restQL Query finished")
                     err))))))
   (catch [:type :validation-error] {:keys [message]}
     (util/json-output 400 {:error "VALIDATION_ERROR" :message message}))
   (catch [:type :parse-error] {:keys [line column]}
     (util/json-output 400 {:error "PARSE_ERROR" :line line :column column}))
   (catch [:type :query-not-found] e
     (util/json-output 404 {:error "QUERY_NO_FOUND"}))
   (catch Exception e (.printStackTrace e)
          (util/json-output 400 {:error "UNKNOWN_ERROR" :message (.getMessage e)}))))

(extend-protocol Renderable
  clojure.lang.IDeref
  (render [d _] (manifold.deferred/->deferred d))
  manifold.deferred.IDeferred
  (render [d _] d)
)

(c/defroutes
  routes
  ; Routes to health checking
  (c/OPTIONS "*" [] {:status 204})
  (c/GET "/health" [] "I'm healthy! :)")
  (c/GET "/resource-status" [] "Up and running! :)")

  ; Route to validate a query
  (c/POST "/validate-query" [] util/validate-request)

  ; Route to run ad hoc queries
  (c/POST "/run-query" [] run-query)

  ; Route to check the parsing of the query
  (c/POST "/parse-query" [] parse-query)

  (c/GET "/run-query/:namespace/:id/:rev" [] run-saved-query)

  (route/not-found "route not found"))


(def app (-> routes
             wrap-exception-handling
             wrap-params))
