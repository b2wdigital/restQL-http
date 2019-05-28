(ns restql.http.server.cors
  (:require [environ.core :refer [env]]
            [restql.config.core :as config]))

(def default-values {:cors-allow-origin   "*"
                     :cors-allow-methods  "GET, POST, PUT, PATH, DELETE, OPTIONS"
                     :cors-allow-headers  "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
                     :cors-expose-headers "Content-Length,Content-Range"})

(defn- get-from-env [key]
  (let [val (env key)]
    (if (empty? val)
      ""
      (read-string val))))

(defn- config-file-cors-headers [key]
  (case key
    :cors-allow-origin   (config/get-config [:cors :allow-origin])
    :cors-allow-methods  (config/get-config [:cors :allow-methods])
    :cors-allow-headers  (config/get-config [:cors :allow-headers])
    :cors-expose-headers (config/get-config [:cors :expose-headers])
    ""))

(defn- get-from-config [key]
  (let [val (config-file-cors-headers key)]
    (cond
      (nil? val)   (default-values key)
      (empty? val) "" 
      :else        val)))

(defn- get-cors-headers [key]
  (if (contains? env key)
    (get-from-env key)
    (get-from-config key)))

(defn- assoc-existing-header [map header-name key]
  (let [val (get-cors-headers key)]
    (if (empty? val)
      map
      (assoc map header-name val))))

(defn fetch-cors-headers []
  (-> {}
      (assoc-existing-header "Access-Control-Allow-Origin" :cors-allow-origin)
      (assoc-existing-header "Access-Control-Allow-Methods" :cors-allow-methods)
      (assoc-existing-header "Access-Control-Allow-Headers" :cors-allow-headers)
      (assoc-existing-header "Access-Control-Expose-Headers" :cors-expose-headers)))
