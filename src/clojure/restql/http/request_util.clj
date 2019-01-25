(ns restql.http.request-util
  (:require [cheshire.core :as json]
            [slingshot.slingshot :refer [try+]]
            [environ.core :refer [env]]
            [clojure.tools.reader.edn :as edn]
            [restql.core.validator.core :as validator]
            [restql.parser.core :as parser]
            [clojure.string :as str])
  (import [org.apache.commons.validator UrlValidator]))

(def headers-blacklist
  ["host"
   "content-type"
   "content-length"
   "connection"
   "origin"
   "accept-encoding"])

(defn header-allowed?
  "Filter to verify if the given header (k) is not on the headers-blacklist"
  [[k v]]

  (let [header (str/lower-case k)]
    (not-any? #(= header %) headers-blacklist)))

(defn add-headers-to-object
  "Adds or appends headers to a given query"
  [headers query-obj]
  (if (nil? (query-obj :with-headers))
    (into query-obj {:with-headers (into {} (filter header-allowed? headers))})
    (into query-obj {:with-headers (into (query-obj :with-headers) (into {} (filter header-allowed? headers)))})))

(defn merge-headers
  "Merge a given headers map with the query :with-headers"
  [headers query]

  (let [data (partition 2 query)
        binds (map first data)
        items (map second data)
        items-with-headers (map (partial add-headers-to-object headers) items)
        data-with-headers (flatten (map vector binds items-with-headers))]
    (binding [*print-meta* true]
      (into [] data-with-headers))))

(defn parse
  "Parse the given query text to EDN format"
  ([text context]
   (parse text context false))

  ([text context pretty]
   (parser/parse-query (str text "\n") :pretty pretty :context context)))

(defn extract-body
  "Extracts the body from a given request"
  [req]

  (->> req
       :body
       slurp))

(defn parse-req
  "Parses a given request, extracting it's headers, parameters and body
   and returns the request body and a context map"
  [req]

  (let [body (extract-body req)
        params (:params req)
        headers (:headers req)
        context (into headers params)]
    (parse body context)))

(defn status-code-ok
  "Verifies if the status code of a given response is an OK class
   status code"
  [query-response]

  (and
   (not (nil? (:status query-response)))
   (< (:status query-response) 300)))

(defn error-output
  "Creates an error output response from a given error"
  [err]

  (case (:type err)
    :expansion-error {:status 422 :body "There was an expansion error"}
    :invalid-resource-type {:status 422 :body "Request with :from string is no longer supported"}
    :invalid-parameter-repetition {:status 422 :body (:message err)}
    {:status 500 :body "internal server error"}))

(defn json-output
  "Creates a json output given it's status and body (message)"
  [status message]

  {:status  status
   :headers {"Content-Type" "application/json"}
   :body    (json/generate-string {:message message})})

(defn make-revision-link
  "Returns a revision route"
  [query-ns id rev]

  (str "/run-query/" query-ns "/" id "/" rev))

(defn make-revision-list-link
  "Returns a query list route"
  [query-ns id]

  (str "/ns/" query-ns "/query/" id))

(defn make-query-link
  "Return a query execution route"
  [query-ns id rev]

  (str "/ns/" query-ns "/query/" id "/revision/" rev))

(defn validate-request
  "Validates if a given request is valid"
  [req]
  (try+
   (let [query (parse-req req)]
     (if (validator/validate {:mappings env} query)
       (json-output 200 "valid")))
   (catch [:type :validation-error] {:keys [message]}
     (json-output 400 message))))

(defn should-ignore-errors
  "Searches for an ignore-errors meta on a given item
   and returns it's value"
  [item]

  (-> item
      :details
      :metadata
      :ignore-errors
      (= "ignore")))

(defn higher-value
  "Returns the max between a and b"
  [a b]

  (cond
    (nil? a) b
    (nil? b) a
    (> a b) a
    :else b))

(defn correct-status [num]
  (cond
    (= num 0) 503
    (= num 204) 200
    :else num))

(defn calculate-response-status-code
  "Calculates the response status code of a given result"
  [result]
  
  (let [statuses (->> result
                      (vals)
                      (flatten)
                      (filter (complement should-ignore-errors))
                      (map (comp :status :details))
                      (map #(correct-status %)))]
    (reduce higher-value 200 statuses)))

(defn map-values [f m]
  (reduce-kv (fn [r k v] (assoc r k (f v))) {} m))

(defn format-response-details
  "Formats the response details"
  [details]
  (if (sequential? details)
    (map format-response-details details)
    (dissoc details :headers)))

(defn format-response-item
  "Formats a response item"
  [item]
  (assoc item :details (format-response-details (:details item))))

(defn format-response-body
  "Formats the response body"
  [result]
  (as-> result x
    (map-values format-response-item x)
    (json/generate-string x)))

(defn is-contextual?
  "Filters if a given map key is contextual"
  [prefix [k v]]
  (->> k str (re-find (re-pattern prefix))))

