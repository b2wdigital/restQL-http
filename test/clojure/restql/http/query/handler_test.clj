(ns restql.http.query.handler-test
  (:require [clojure.test :refer :all]
            [slingshot.test]
            [slingshot.slingshot :refer [throw+]]
            [clojure.core.async :refer [chan go >!]]
            [clojure.string :refer [includes?]]
            [restql.config.core :as config]
            [environ.core :refer [env]]
            [restql.http.server.handler :as server-handler]
            [restql.http.server.cors :as cors]
            [restql.http.query.handler :refer [parse]]
            [restql.http.request.queries :as request-queries]
            [restql.http.query.handler :as query-handler]))

(defn- contains-many? [m & ks]
  (every? #(contains? m %) ks))

(deftest parsing-result-from-request
    (testing "Parse query should work for valid EDN"
      (is
       (= [:foo {:from :foo :method :get} :bazinga {:from :bar :with {:id "123"} :method :get}]
          (:body (-> {:params {"id" 123}
                      :headers {}
                      :body "test/resources/sample_query.rql"}
                     (query-handler/parse)
                     (deref)))))))

(deftest blocked-adhoc
  (testing ":allow-adhoc-queries environment variable is set to false should return 405"
    (with-redefs [server-handler/get-default-value (fn [_] false)]
                 (let [get-adhoc-behaviour #'server-handler/get-adhoc-behaviour]
                   (is
                    (= {:status 405
                        :headers {"Content-Type" "application/json"}
                        :body "{\"error\":\"FORBIDDEN_OPERATION\",\"message\":\"ad-hoc queries are turned off\"}"}
                       (get-adhoc-behaviour {})))))))

(deftest test-query-no-found
  (testing "Is return for query not found"
    (with-redefs [request-queries/get-query (fn [_] (throw+ {:type :query-not-found}))]
      (is (= {:status  404
              :headers {"Content-Type" "application/json"}
              :body    "{\"error\":\"QUERY_NOT_FOUND\"}"}
             (query-handler/saved {:params {:namespace "ns", :id "id-1", :rev "5"}}))))))

(deftest should-return-500-when-exception-processing-query

  (testing "Is return for run-saved-query with exception"
    (let [error-ch (chan)]
      (go (>! error-ch "Some error"))
      (with-redefs [request-queries/get-query (constantly {})
                    restql.http.query.handler/parse (constantly {})
                    restql.core.api.restql/execute-query-channel (constantly [(chan) error-ch])]
        (let [result (-> {:params {:namespace "ns", :id "id", :rev "1"}}
                         (query-handler/saved)
                         (deref))]
          (is (= 500 (:status result)))
          (is (= {"Content-Type" "application/json"} (:headers result)))
          (is (includes? (:body result) "{\"error\":\"UNKNOWN_ERROR\",\"message\":"))))))

  (testing "Is return for run-query with exception"
    (let [exception-ch (chan)]
      (go (>! exception-ch {:error "some error"}))
      (with-redefs [parse (constantly {})
                    restql.core.api.restql/execute-query-channel (constantly [(chan) exception-ch])]
        (is (= {:status  500
                :headers {"Content-Type" "application/json"}
                :body    "{\"error\":\"UNKNOWN_ERROR\",\"message\":null}"}
               (-> {:params {:namespace "ns", :id "id", :rev "1"}}
                   (query-handler/adhoc)
                   (deref))))))))

(deftest options-test
  (testing "Options should return 204"
    (is
     (= 204
        (get (server-handler/options {}) :status))))

  (testing "Should have CORS headers"
    (is
     (= true
        (contains-many? (get (server-handler/options {}) :headers) 
                        "Access-Control-Allow-Origin"
                        "Access-Control-Allow-Methods"
                        "Access-Control-Allow-Headers"
                        "Access-Control-Expose-Headers"))))
  
  (testing "Should follow CORS headers priority ENV > Config File > Default"
    (reset! config/config-data {:cors {:allow-origin "xyz"
                                       :allow-methods ""}})
    (is 
     (= {"Access-Control-Allow-Origin"  "abc"
         "Access-Control-Allow-Headers" "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
         "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
        (get (server-handler/options {}) :headers)))
    
    (reset! config/config-data {}))
  
  (testing "Should not add empty / null CORS headers"    
    (with-redefs-fn {#'cors/get-from (fn [function key] (if (= function env) (case key
                                                                               :cors-allow-origin ""
                                                                               (if-let [val (env key)] (read-string val) nil))))}
      #(is
        (= {"Access-Control-Allow-Methods" "GET, POST, PUT, PATH, DELETE, OPTIONS"
            "Access-Control-Allow-Headers" "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
            "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
           (get (server-handler/options {}) :headers))))))