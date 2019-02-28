(ns restql.http.query.mappings
  (:require [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [restql.config.core :as config]
            [restql.http.database.core :as dbcore]
            [restql.http.cache.core :as cache]))

(def default-value {:mappings-ttl 60000
                    :tenant "DEFAULT"})

(defn- get-default-value [env-var]
  (if (contains? env env-var) (read-string (env env-var)) (get default-value env-var)))

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
     (or (get-default-value :tenant))
     (dbcore/find-tenant-by-id)
     (:mappings))
    (catch Exception e
      (log/error "Error getting mappings from db" (.getMessage e))
      nil)))

(def from-tenant
  (->
   (fn [tenant] (merge
                  (get-mappings-from-config)
                  (get-mappings-from-db tenant)
                  env))
   (cache/cached (get-default-value :mappings-ttl))))
