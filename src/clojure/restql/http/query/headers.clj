(ns restql.http.query.headers
  (:require [clojure.string :as str]
            [environ.core :refer [env]]
            [restql.config.core :as config]))

(def headers-blacklist
  ["host"
   "content-type"
   "content-length"
   "connection"
   "origin"
   "accept-encoding"])

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

(def error-headers-blacklist
  ["cache-control"])

(defn- headers-from-req-info [req-info]
  (->> req-info
       (reduce (fn [headers [key val]] (if (= key :type)
                                         (merge headers {(str "restql-query-control") (name val)})
                                         (merge headers {(str "restql-query-" (name key)) (str val)}))) {})))

(defn- req-and-query-headers [req-info req]
  (-> req-info
      (headers-from-req-info)
      (into (:headers req))))

(defn header-allowed
  "Filter to verify if the given header (k) is not on the headers-blacklist"
  [req-info req]

  (apply dissoc (req-and-query-headers req-info req) headers-blacklist))

(defn filter-error-headers [response]
  (if (= 200 (:status response))
    (:headers response)
    (apply dissoc (:headers response) error-headers-blacklist)))