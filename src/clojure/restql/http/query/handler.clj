(ns restql.http.query.handler
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [manifold.stream :as manifold]
            [environ.core :refer [env]]
            [slingshot.slingshot :as slingshot]
            [restql.http.request.util :as request-util]
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

(defn- forward-params-from-query [env query-params]
  (-> env
      (:forward-prefix)
      (as-> prefix
            (if (nil? prefix)
              (identity {})
              (into {} (filter (partial request-util/is-contextual? prefix) query-params))))))

(defn- headers-from-req-info [req-info]
  (->> req-info
       (reduce (fn [headers [key val]] (if (= key :type)
                                         (merge headers {(str "restql-query-control") (name val)})
                                         (merge headers {(str "restql-query-" (name key)) (str val)}))) {})))

(defn- req-and-query-headers [req-info req]
  (-> req-info
      (headers-from-req-info)
      (into (:headers req))))

(defn- req->query-opts [req-info req]
  (let [query-params (-> req :query-params keywordize-keys)]
    {:debugging      (debugging-from-query query-params)
     :tenant         (tenant-from-env-or-query env query-params)
     :info           req-info
     :forward-params (forward-params-from-query env query-params)
     :forward-headers (req-and-query-headers req-info req)}))

(defn- req->query-ctx [req]
  (-> (:params req)
      (into (:headers req))))

(defn parse [req]
  (manifold/take!
   (manifold/->source
    (async/go
      (slingshot/try+
       {:status 200 :body (request-util/parse-req req)}
       (catch [:type :parse-error] {:keys [line column reason]}
        {:status 400 :body (str "Parsing error in line " line ", column " column "\n" reason)}))))))

(defn validate [req]
  (manifold/take!
   (manifold/->source
    (async/go
      (request-util/validate-request req)))))

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
      (request-util/json-output 500 {:error "UNKNOWN_ERROR" :message (.getMessage e)}))))

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
      (request-util/json-output 404 {:error "QUERY_NO_FOUND"}))
    (catch Exception e (.printStackTrace e)
      (request-util/json-output 500 {:error "UNKNOWN_ERROR" :message (.getMessage e)}))))