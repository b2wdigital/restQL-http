(ns restql.http.query.mappings
  (:require [clojure.tools.logging :as log]
            [restql.config.core :as config]
            [restql.http.database.core :as dbcore]
            [restql.http.cache.core :as cache]))

(defn- get-mappings-from-config []
  (->>
   [:mappings]
   (config/get-config)
   (into {})))

(defn- get-mappings-from-db
  [tenant]
  (try
    (->
     tenant
     (or (config/get-config :tenant))
     (dbcore/find-tenant-by-id)
     (:mappings))
    (catch Exception e
      (log/error "Error getting mappings from db" (.getMessage e))
      nil)))

(def from-tenant
  (->>
   (fn [tenant] (merge
                 (get-mappings-from-config)
                 (get-mappings-from-db tenant)
                 (:env (config/get-config))))
   (cache/cached :ttl (config/get-config :mappings-ttl))))
