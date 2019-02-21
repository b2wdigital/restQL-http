(ns restql.http.request.mappings
  (:require [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [restql.config.core :as config]
            [restql.http.database.core :as dbcore]
            [restql.http.cache.core :as cache]))

(def MAPPINGS_TTL (if (contains? env :mappings-cache-ttl) (read-string (env :mappings-cache-ttl)) 60000))

(defn get-mappings-from-config []
  (->>
   [:mappings]
   (config/get-config)
   (into {})))

(defn get-mappings-from-db
  [tenant]
  (try
    (->>
     tenant
     (dbcore/find-tenant-by-id)
     (:mappings))
    (catch Exception e
      (log/error "Error getting mappings from db" (.getMessage e))
      nil)))

(def get-mappings
  (->
   (fn [tenant] (if (nil? tenant)
                  (merge
                   (get-mappings-from-config)
                   env)
                  (merge
                   (get-mappings-from-config)
                   (get-mappings-from-db tenant)
                   env)))
   (cache/cached MAPPINGS_TTL)))