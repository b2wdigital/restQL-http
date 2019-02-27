(ns restql.http.query.headers
  (:require [clojure.string :as str]))

(def headers-blacklist
  ["host"
   "content-type"
   "content-length"
   "connection"
   "origin"
   "accept-encoding"])

(defn- header-allowed?
  "Filter to verify if the given header (k) is not on the headers-blacklist"
  [[k v]]

  (let [header (str/lower-case k)]
    (not-any? #(= header %) headers-blacklist)))

(defn- add-headers-to-object
  "Adds or appends headers to a given query"
  [headers query-obj]

  (if (nil? (query-obj :with-headers))
    (into query-obj {:with-headers (into {} (filter header-allowed? headers))})
    (into query-obj {:with-headers (into (query-obj :with-headers) (into {} (filter header-allowed? headers)))})))

(defn query-with-foward-headers
  "Merge a given headers map with the query :with-headers"
  [headers query]

  (let [data (partition 2 query)
        binds (map first data)
        items (map second data)
        items-with-headers (map (partial add-headers-to-object headers) items)
        data-with-headers (flatten (map vector binds items-with-headers))]
    (binding [*print-meta* true]
      (into [] data-with-headers))))