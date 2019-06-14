(ns restql.http.query.headers
  (:require [clojure.string :as str]
            [restql.config.core :as config]))

(def headers-blacklist
  ["host"
   "content-type"
   "content-length"
   "connection"
   "origin"
   "accept-encoding"])

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