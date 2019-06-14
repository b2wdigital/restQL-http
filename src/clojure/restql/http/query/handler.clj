(ns restql.http.query.handler
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [manifold.stream :as manifold]
            [restql.config.core :as config]
            [slingshot.slingshot :as slingshot]
            [restql.core.validator.core :as validator]
            [restql.parser.core :as parser]
            [restql.http.query.headers :as headers]
            [restql.http.query.runner :as query-runner]
            [restql.http.request.queries :as request-queries]))

(defn- debugging-from-query [query-params]
  (-> query-params
      (:_debug)
      boolean))

(defn- tenant-from-config-data-or-query [config-data query-params]
  (-> config-data
      (:tenant)
      (as-> tenant
            (if (nil? tenant)
              (:tenant query-params)
              tenant))))

(defn- is-contextual?
  "Filters if a given map key is contextual"
  [prefix [k v]]
  (->> k str (re-find (re-pattern prefix))))

(defn- forward-params-from-query [config-data query-params]
  (-> config-data
      (:forward-prefix)
      (as-> prefix
            (if (nil? prefix)
              (identity {})
              (into {} (filter (partial is-contextual? prefix) query-params))))))

(defn- req->query-opts [req-info req]
  (let [query-params (-> req :query-params keywordize-keys)]
    {:debugging      (debugging-from-query query-params)
     :tenant         (tenant-from-config-data-or-query (:env (config/get-config)) query-params)
     :info           req-info
     :forward-params (forward-params-from-query (:env (config/get-config)) query-params)
     :forward-headers (headers/header-allowed req-info req)}))

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
         (if (validator/validate {:mappings (:env (config/get-config))} query)
           {:status 200 :headers {"Content-Type" "application/json"} :body "valid"}))
       (catch [:type :validation-error] {:keys [message]}
         {:status 400 :headers {"Content-Type" "application/json"} :body (str "\"" message "\"")}))))))

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
          {:status 500 :headers {"Content-Type" "application/json"} :body (str "{\"error\":\"UNKNOWN_ERROR\",\"message\":\"" (.getMessage e) "\"}")})))

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
     {:status 404 :headers {"Content-Type" "application/json"} :body "{\"error\":\"QUERY_NOT_FOUND\"}"})
   (catch Exception e
     (.printStackTrace e)
     {:status 500 :headers {"Content-Type" "application/json"} :body (str "{\"error\":\"UNKNOWN_ERROR\",\"message\":\"" (.getMessage e) "\"}")})
   (catch Object o
     (log/error "UNKNOWN_ERROR" o)
     {:status 500 :headers {"Content-Type" "application/json"} :body "{\"error\":\"UNKNOWN_ERROR\"}"})))