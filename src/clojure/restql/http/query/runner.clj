(ns restql.http.query.runner
  (:require [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [stringify-keys]]
            [slingshot.slingshot :as slingshot]
            [environ.core :refer [env]]
            [restql.http.request.util :as request-util]
            [restql.http.request.mappings :as request-mappings]
            [restql.parser.core :as parser]
            [restql.core.api.restql :as restql]
            [restql.core.response.headers :as response-headers]
            [restql.core.encoders.core :as encoders]))

(def default-values {:query-global-timeout 30000})

(defn- get-default [key]
  (if (contains? env key) (read-string (env key)) (default-values key)))

(defn- make-headers [interpolated-query result]
  (-> (response-headers/get-response-headers interpolated-query result)
      (into {"Content-Type" "application/json"})
      (stringify-keys)))

(defn- create-response [query result]
  (slingshot/try+
   {:body    (request-util/format-response-body result)
    :headers (make-headers query result)
    :status  (request-util/calculate-response-status-code result)}
   (catch Exception e (.printStackTrace e)
    (request-util/error-output e))))

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
           enhanced-query          (request-util/merge-headers (:forward-headers query-opts) parsed-query)
           mappings                (request-mappings/get-mappings (:tenant query-opts))
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
                     (request-util/json-output 408 {:status 408 :message "Query timed out"}))
         exception-ch ([err]
                       (log/warn "query runner with error" {:time (- (System/currentTimeMillis) time-before)
                                                            :success false})
                       (request-util/error-output err))
         query-ch ([resp]
                   (log/debug "query runner with success" {:time (- (System/currentTimeMillis) time-before)
                                                           :success true})
                   (create-response parsed-query resp))))
    (catch [:type :validation-error] {:keys [message]}
      (request-util/json-output 400 {:error "VALIDATION_ERROR" :message message}))
    (catch [:type :parse-error] {:keys [line column]}
      (request-util/json-output 400 {:error "PARSE_ERROR" :line line :column column}))
    (catch Exception e (.printStackTrace e)
      (request-util/json-output 500 {:error "UNKNOWN_ERROR" :message (.getMessage e)})))))