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