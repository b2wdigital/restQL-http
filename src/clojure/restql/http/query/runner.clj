(ns restql.http.query.runner
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [stringify-keys]]
            [cheshire.core :as chesire]
            [environ.core :refer [env]]
            [slingshot.slingshot :as slingshot]
            [restql.http.query.headers :as headers]
            [restql.http.query.calculate-response-status-code :refer [calculate-response-status-code]]
            [restql.http.query.json-output :refer [json-output]]
            [restql.http.query.mappings :as mappings]
            [restql.parser.core :as parser]
            [restql.core.api.restql :as restql]
            [restql.core.response.headers :as response-headers]
            [restql.core.encoders.core :as encoders]))

(def default-values {:query-global-timeout 30000})

(defn- get-default [key]
  (if (contains? env key) (read-string (env key)) (default-values key)))

(defn- make-headers [interpolated-query result]
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
  (assoc item :details (dissoc-headers-from-details (:details item))))

(defn- result-without-headers
  "Formats the response body"
  [result]

  (map-values assoc-item-details result))

(defn- create-response [query result]
  (slingshot/try+
   {:body    (result-without-headers result)
    :headers (make-headers query result)
    :status  (calculate-response-status-code result)}
   (catch Exception e (.printStackTrace e)
    (identify-error e))))

(defn- execute-query [query mappings encoders query-opts]
  (restql/execute-query-channel :mappings mappings
                                :encoders encoders
                                :query query
                                :query-opts query-opts))

(defn run [query-string query-opts context]
  (async/go
    (slingshot/try+
     (let [time-before             (System/currentTimeMillis)
           parsed-query            (parser/parse-query query-string :context context)
           enhanced-query          (headers/query-with-foward-headers (:forward-headers query-opts) parsed-query)
           mappings                (mappings/from-tenant (:tenant query-opts))
           encoders                encoders/base-encoders
           [query-ch exception-ch] (execute-query enhanced-query mappings encoders query-opts)
           timeout-ch              (async/timeout (get-default :query-global-timeout))]
       (log/debug "query runner start with" {:query-string query-string
                                             :query-opts query-opts
                                             :context context})
       (async/alt!
         timeout-ch ([]
                     (log/warn "query runner timed out" {:time (- (System/currentTimeMillis) time-before)
                                                         :success false})
                     (json-output {:status 408 :body {:status 408 :message "Query timed out"}}))
         exception-ch ([err]
                       (log/warn "query runner with error" {:time (- (System/currentTimeMillis) time-before)
                                                            :success false})
                       (identify-error err))
         query-ch ([resp]
                   (log/debug "query runner with success" {:time (- (System/currentTimeMillis) time-before)
                                                           :success true})
                   (json-output (create-response parsed-query resp)))))
     (catch [:type :validation-error] {:keys [message]}
       (json-output {:status 400 :body {:error "VALIDATION_ERROR" :message message}}))
     (catch [:type :parse-error] {:keys [line column]}
       (json-output {:status 400 :body {:error "PARSE_ERROR" :line line :column column}}))
     (catch Exception e (.printStackTrace e)
            (json-output {:status 500 :body {:error "UNKNOWN_ERROR" :message (.getMessage e)}})))))