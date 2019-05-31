(ns restql.http.server.cors
  (:require [environ.core :refer [env]]
            [restql.config.core :as config]))

(def default-values {:cors-allow-origin   "*"
                     :cors-allow-methods  "GET, POST, PUT, PATH, DELETE, OPTIONS"
                     :cors-allow-headers  "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
                     :cors-expose-headers "Content-Length,Content-Range"})

(defn- config-file-cors-headers [key]
  (case key
    :cors-allow-origin   (config/get-config [:cors :allow-origin])
    :cors-allow-methods  (config/get-config [:cors :allow-methods])
    :cors-allow-headers  (config/get-config [:cors :allow-headers])
    :cors-expose-headers (config/get-config [:cors :expose-headers])))

(defn- get-from [function key]
  (let [val (function key)]
    (cond
      (nil? val)   nil
      (empty? val) ""
      :else        val)))

(defn- get-value-from-env-or-config [key]
  (cond
    (some? (get-from env key)) (get-from env key)
    (some? (get-from config-file-cors-headers key)) (get-from config-file-cors-headers key)
    :else (default-values key)))

(defn- assoc-header-if-not-empty [map header-name value]
  (if (empty? value)
    map
    (assoc map header-name value)))

(defn fetch-cors-headers []
  (-> {}
      (assoc-header-if-not-empty "Access-Control-Allow-Origin"   (get-value-from-env-or-config :cors-allow-origin))
      (assoc-header-if-not-empty "Access-Control-Allow-Methods"  (get-value-from-env-or-config :cors-allow-methods))
      (assoc-header-if-not-empty "Access-Control-Allow-Headers"  (get-value-from-env-or-config :cors-allow-headers))
      (assoc-header-if-not-empty "Access-Control-Expose-Headers" (get-value-from-env-or-config :cors-expose-headers))))
