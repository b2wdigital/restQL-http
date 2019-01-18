(ns restql.http.request.queries
  (:require [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [restql.config.core :as config]
            [restql.http.database.core :as dbcore]
            [restql.http.cache :as cache]
            [slingshot.slingshot :refer [throw+]]))

(defonce FIND_QUERY_TTL 86400000)

(defn get-query-from-config [namespace id revision]
  (if-let [config (->
                   [:queries]
                   (conj (keyword namespace))
                   (conj (keyword id))
                   (config/get-config))]
    (get config (dec revision))
    (throw+ {:type :query-not-found})))

(defn get-query-from-db [namespace id revision]
  (try
    (->
     (dbcore/find-query-by-id-and-revision namespace id revision)
     :text)
    (catch Exception e
      (log/error "Error getting query from db" (.getMessage e))
      nil)))

(def get-query
  (cache/cached
   (fn [namespace id revision]
     (let
      [query-from-db (get-query-from-db namespace id revision)]
       (if-not (nil? query-from-db)
         query-from-db
         (get-query-from-config namespace id revision))))
   FIND_QUERY_TTL))