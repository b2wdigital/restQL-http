(ns restql.http.query.handler
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [manifold.stream :as manifold]
            [environ.core :refer [env]]
            [slingshot.slingshot :as slingshot]
            [restql.core.validator.core :as validator]
            [restql.parser.core :as parser]
            [restql.http.query.headers :as headers]
            [restql.http.query.json-output :refer [json-output]]
            [restql.http.query.runner :as query-runner]
            [restql.http.request.queries :as request-queries]))

(defn- debugging-from-query [query-params]
  (-> query-params
      (:_debug)
      boolean))

(defn- tenant-from-env-or-query [env query-params]
  (-> env
      (:tenant)
      (as-> tenant
            (if (nil? tenant)
              (:tenant query-params)
              tenant))))

(defn- is-contextual?
  "Filters if a given map key is contextual"
  [prefix [k v]]
  (->> k str (re-find (re-pattern prefix))))

(defn- forward-params-from-query [env query-params]
  (-> env
      (:forward-prefix)
      (as-> prefix
            (if (nil? prefix)
              (identity {})
              (into {} (filter (partial is-contextual? prefix) query-params))))))

(defn- req->query-opts [req-info req]
  (let [query-params (-> req :query-params keywordize-keys)]
    {:debugging      (debugging-from-query query-params)
     :tenant         (tenant-from-env-or-query env query-params)
     :info           req-info
     :forward-params (forward-params-from-query env query-params)
     :forward-headers (headers/headers-allowed req-info req)}))

(defn- req->query-ctx [req]
  (-> (:params req)
      (into (:headers req))))

(defn parse 
  ([req]
   (parse req false))
  
  ([req pretty]
   (let [req-info {:type :parse-query}
         query-ctx (req->query-ctx req)
         query-string (some-> req :body slurp)]
     (manifold/take!
      (manifold/->source
       (async/go
         (slingshot/try+
          {:status 200 :body (parser/parse-query query-string :pretty pretty :context query-ctx)}
          (catch [:type :parse-error] {:keys [line column reason]}
            {:status 400 :body (str "Parsing error in line " line ", column " column "\n" reason)}))))))))

(defn validate [req]
  (manifold/take!
   (manifold/->source
    (async/go
      (slingshot/try+
       (let [query (parse req)]
         (if (validator/validate {:mappings env} query)
           (json-output {:status 200 :body "valid"})))
       (catch [:type :validation-error] {:keys [message]}
         (json-output {:status 400 :body message})))))))

(defn adhoc [req]
  (slingshot/try+
   (let [req-info {:type :ad-hoc}
         query-opts (req->query-opts req-info req)
         query-ctx (req->query-ctx req)
         query-string (some-> req :body slurp)]
     (manifold/take!
      (manifold/->source
       (query-runner/run query-string query-opts query-ctx))))
   (catch Exception e (.printStackTrace e)
          (json-output {:status 500 :body {:error "UNKNOWN_ERROR" :message (.getMessage e)}}))))

(defn saved [req]
  (slingshot/try+
    (let [req-info {:type      :saved
                    :id        (some-> req :params :id)
                    :namespace (some-> req :params :namespace)
                    :revision  (some-> req :params :rev read-string)}
          query-opts (req->query-opts req-info req)
          query-ctx (req->query-ctx req)
          query-string (request-queries/get-query req-info)]
      (manifold/take!
       (manifold/->source
        (query-runner/run query-string query-opts query-ctx))))
    (catch [:type :query-not-found] e
      (json-output {:status 404 :body {:error "QUERY_NO_FOUND"}}))
    (catch Exception e (.printStackTrace e)
      (json-output {:status 500 :body {:error "UNKNOWN_ERROR" :message (.getMessage e)}}))))
