(ns restql.http.server.cors
  (:require [environ.core :refer [env]]
            [restql.config.core :as config]))

(def default-values {:cors-allow-origin   "*"
                     :cors-allow-methods  "GET, POST, PUT, PATH, DELETE, OPTIONS"
                     :cors-allow-headers  "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
                     :cors-expose-headers "Content-Length,Content-Range"})

(defn- get-default [key]
  (if (contains? env key) (read-string (env key)) (default-values key)))

(defn- config-file-cors-headers [key]
  (case key
    :cors-allow-origin   (config/get-config [:cors :allow-origin])
    :cors-allow-methods  (config/get-config [:cors :allow-methods])
    :cors-allow-headers  (config/get-config [:cors :allow-headers])
    :cors-expose-headers (config/get-config [:cors :expose-headers])))

(defn- get-from-config [key]
  (let [val (config-file-cors-headers key)]
    (if-not (nil? val)
      val
      (default-values key))))

(defn- get-cors-headers [key]
  (if (contains? env key)
    (read-string (env key))
    (get-from-config key)))

(defn fetch-cors-headers []
  {"Access-Control-Allow-Origin"   (get-cors-headers :cors-allow-origin)
   "Access-Control-Allow-Methods"  (get-cors-headers :cors-allow-methods)
   "Access-Control-Allow-Headers"  (get-cors-headers :cors-allow-headers)
   "Access-Control-Expose-Headers" (get-cors-headers :cors-expose-headers)})
