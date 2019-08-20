(ns restql.http.query.runner
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [stringify-keys]]
            [restql.parser.json :as json]
            [environ.core :refer [env]]
            [restql.config.core :as config]
            [slingshot.slingshot :as slingshot]
            [restql.http.server.cors :as cors]
            [restql.http.query.headers :as headers]
            [restql.http.query.calculate-response-status-code :refer [calculate-response-status-code]]
            [restql.http.query.mappings :as mappings]
            [restql.parser.core :as parser]
            [restql.core.api.restql :as restql]
            [restql.core.response.headers :as response-headers]
            [restql.core.encoders.core :as encoders]))

(def default-values {:query-global-timeout 30000
                     :max-query-overhead-ms 50})

(defn- get-default [key]
  (if (contains? env key) (read-string (env key)) (default-values key)))

(defn- get-response-headers [interpolated-query result]
  (-> (response-headers/get-response-headers interpolated-query result)
      (stringify-keys)))

(defn- identify-error
  "Creates an error output response from a given error"
  [err]

  (case (:type err)
    :expansion-error {:status 422 :body "There was an expansion error"}
    :invalid-resource-type {:status 422 :body "Request with :from string is no longer supported"}
    :invalid-parameter-repetition {:status 422 :body (:message err)}
    {:status 500 :body "internal server error"}))

(defn- map-values [f m]
  (reduce-kv (fn [r k v] (assoc r k (f v))) {} m))

(defn- dissoc-headers-from-details
  "Dissoc headers from the response details"
  [details]
  (if (sequential? details)
    (map dissoc-headers-from-details details)
    (dissoc details :headers)))

(defn- assoc-item-details
  "Formats a response item"
  [item]
  (if-let [details (:details item)]
    (assoc item :details (dissoc-headers-from-details details))
    item))

(defn- result-without-headers
  "Formats the response body"
  [result]

  (map-values assoc-item-details result))

(defn- parse-param-json-value [value]
  (slingshot/try+
   (json/parse-string value)
   (catch Exception e
     value)))

(defn- parse-param-value [value]
  (if (and (string? value) (re-matches #"[\{\[](.*)" value))
    (parse-param-json-value value)
    value))

(defn- create-response [query result]
  (if (= (:error result) :timeout)
    {:status 408 :headers {"Content-Type" "application/json"} :body {"message" "Query timed out"}}
    (slingshot/try+
     {:body    (result-without-headers result)
      :headers (get-response-headers query result)
      :status  (calculate-response-status-code result)}
     (catch Exception e (.printStackTrace e)
            (identify-error e)))))

(defn- execute-query [context query mappings encoders query-opts]
  (restql/execute-query-channel :context context
                                :mappings mappings
                                :encoders encoders
                                :query query
                                :query-opts query-opts))

(defn- make-headers [response]
  (->> (headers/filter-error-headers response)
       (stringify-keys)
       (merge (cors/fetch-cors-headers))
       (merge {"Content-Type" "application/json"})))

(defn- json-output
  "Creates a json output given it's status and body (message)"
  [response]

  {:status  (:status response)
   :headers (make-headers response)
   :body    (json/generate-string (:body response))})

(defn- get-error-msg [error]
  (let [errorMsg (.getMessage error)]
    (if (nil? errorMsg)
      "null"
      errorMsg)))

(defmacro timed-go
  [timeout & args]
  `(let [time# (System/currentTimeMillis)]
     (async/go
       (if (< (- (System/currentTimeMillis) time#) ~timeout)
         ~@args
         {:status 507 :headers {"Content-Type" "application/json"} :body "{\"error\":\"START_EXECUTION_TIMEOUT\"}"}))))

(defn run [context query-string query-opts query-ctx]
  (timed-go (get-default :max-query-overhead-ms)
            (slingshot/try+
             (let [time-before             (System/currentTimeMillis)
                   parsed-context          (map-values parse-param-value query-ctx)
                   query-type              (get-in query-opts [:info :type])
                   parsed-query            (parser/parse-query query-string :context parsed-context :query-type query-type)
                   mappings                (mappings/from-tenant (:tenant query-opts))
                   encoders                encoders/base-encoders
                   [query-ch exception-ch] (execute-query context parsed-query mappings encoders query-opts)]
               (log/debug "query runner start with" {:query-string query-string
                                                     :query-opts query-opts
                                                     :context context})
               (async/alt!
                 exception-ch ([err]
                               (log/warn "query runner with error" {:time (- (System/currentTimeMillis) time-before)
                                                                    :success false})
                               (identify-error err))
                 query-ch ([resp]
                           (log/debug "query runner with success" {:time (- (System/currentTimeMillis) time-before)
                                                                   :success true})
                           (json-output (create-response parsed-query resp)))))
             (catch [:type :validation-error] {:keys [message]}
               (log/error {:error "VALIDATION_ERROR" :message message})
               {:status 400 :headers {"Content-Type" "application/json"} :body (str "{\"error\":\"VALIDATION_ERROR\",\"message\":\"" message "\"}")})
             (catch [:type :parse-error] {:keys [line column]}
               (log/error {:error "PARSE_ERROR" :line line :column column})
               {:status 400 :headers {"Content-Type" "application/json"} :body (str "{\"error\":\"PARSE_ERROR\",\"line\":\"" line "\",\"column\":\"" column "\"}")})
             (catch Exception e
               (.printStackTrace e)
               {:status 500 :headers {"Content-Type" "application/json"} :body (str "{\"error\":\"UNKNOWN_ERROR\",\"message\":" (get-error-msg e) "}")})
             (catch Object o
               (log/error "UNKNOWN_ERROR" o)
               {:status 500 :headers {"Content-Type" "application/json"} :body "{\"error\":\"UNKNOWN_ERROR\"}"}))))
