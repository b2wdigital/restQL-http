(ns restql.http.query.handler-test
  (:require [clojure.test :refer :all]
            [slingshot.test]
            [slingshot.slingshot :refer [throw+]]
            [clojure.core.async :refer [chan go >!]]
            [restql.http.query.handler :refer [parse]]
            [restql.http.request.queries :as request-queries]
            [restql.http.query.handler :as query-handler]))

(deftest parsing-result-from-request
    (testing "Parse query should work for valid EDN"
      (is
       (= [:foo {:from :foo :method :get} :bazinga {:from :bar :with {:id "123"} :method :get}]
          (:body (-> {:params {"id" 123}
                      :headers {}
                      :body "test/resources/sample_query.rql"}
                     (query-handler/parse)
                     (deref)))))))

(deftest test-query-no-found
  (testing "Is return for query not found"
    (with-redefs [request-queries/get-query (fn [_] (throw+ {:type :query-not-found}))]
      (is (= {:status  404
              :headers {"Content-Type" "application/json"}
              :body    "{\"error\":\"QUERY_NO_FOUND\"}"}
             (query-handler/saved {:params {:namespace "ns", :id "id-1", :rev "5"}}))))))

(deftest should-return-500-when-exception-processing-query

  (testing "Is return for run-saved-query with exception"
    (let [error-ch (chan)]
      (go (>! error-ch "Some error"))
      (with-redefs [request-queries/get-query (constantly {})
                    restql.http.query.handler/parse (constantly {})
                    restql.http.query.headers/query-with-foward-headers (constantly {})
                    restql.core.api.restql/execute-query-channel (constantly [(chan) error-ch])]
        (is (= {:status  500
                :headers {"Content-Type" "application/json"}
                :body    "{\"error\":\"UNKNOWN_ERROR\",\"message\":\"clojure.lang.PersistentArrayMap cannot be cast to java.lang.CharSequence\"}"}
               (-> {:params {:namespace "ns", :id "id", :rev "1"}}
                   (query-handler/saved)
                   (deref)))))))

  (testing "Is return for run-query with exception"
    (let [exception-ch (chan)]
      (go (>! exception-ch {:error "some error"}))
      (with-redefs [parse (constantly {})
                    restql.http.query.headers/query-with-foward-headers (constantly {})
                    restql.core.api.restql/execute-query-channel (constantly [(chan) exception-ch])]
        (is (= {:status  500
                :headers {"Content-Type" "application/json"}
                :body    "{\"error\":\"UNKNOWN_ERROR\",\"message\":null}"}
               (-> {:params {:namespace "ns", :id "id", :rev "1"}}
                   (query-handler/adhoc)
                   (deref))))))))
