(ns restql.http.query.runner-test
  (:require [clojure.test :refer :all]
            [restql.http.query.runner :as runner]))

(deftest test-map-values
  (let [map-values #'runner/map-values]
    (is (= {:foo 2 :bar 3}
           (map-values #(+ 1 %) {:foo 1 :bar 2})))))

(deftest formatting-restql-response
  (let [dissoc-headers-from-details #'runner/dissoc-headers-from-details
        assoc-item-details #'runner/assoc-item-details]
    (testing "Headers should not be in a response"
      (is (=
           {:metadata {:x 1}}
           (dissoc-headers-from-details {:headers {"foo" "bar"} :metadata {:x 1}}))))

    (testing "Format should remove headers"
      (is (=
           {:details {:metadata {:x 1}}}
           (assoc-item-details {:details {:headers {"foo" "bar"} :metadata {:x 1}}}))))))

(deftest creating-a-valid-json-response
  (testing "If it's a valid json response"
    (let [json-output #'runner/json-output]
      (is (=
           {:status  200
            :headers {"Content-Type" "application/json" 
                      "Access-Control-Allow-Origin"   "http://www.example.com"
                      "Access-Control-Allow-Methods"  "GET, POST, PUT, PATH, DELETE, OPTIONS"
                      "Access-Control-Allow-Headers"  "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
                      "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
            :body "{\"foo\":\"bar\"}"}
           (json-output {:status 200 :body {:foo "bar"}}))))))

(deftest identify-error-test
  (let [identify-error #'runner/identify-error]
    (testing "Expansion Error should return 422"
      (is (=
           {:status 422 :body "There was an expansion error"}
           (identify-error {:type :expansion-error}))))

    (testing "Expansion Error should return 422"
      (is (=
           {:status 500 :body "internal server error"}
           (identify-error {:type :unknown}))))))

(deftest remove-error-cache-control-test
  (let [json-output #'runner/json-output]
    (testing "Status 200 should have cache-control header"
      (is (= true
             (contains? (:headers (json-output {:body {}, :headers {"cache-control" "max-age=600"}, :status 200})) "cache-control"))))
    (testing "Any status other than 200 should not have cache-control header"
      (is (= false
             (contains? (:headers (json-output {:body {}, :headers {"cache-control" "max-age=600"}, :status 500})) "cache-control"))))))