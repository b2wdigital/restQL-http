(ns restql.http.query.status-code-test
  (:require [clojure.test :refer :all]
            [restql.http.query.calculate-response-status-code :as status-code]))

(deftest ignoring-errors-when-flagged
  (let [not-error-ignored? #'status-code/not-error-ignored?]
    (testing "Should return true for ignore"
      (is (false? (not-error-ignored? {:metadata {:ignore-errors "ignore"}}))))
    (testing "Should return false if no metadata provided"
      (is (true? (not-error-ignored? {}))))))

(deftest returning-higher-value
  (let [higher-value #'status-code/higher-value]
    (testing "Should return highers"
      (is (= 204 (higher-value [204 nil])))
      (is (= 408 (higher-value [200 204 nil 408])))
      (is (= 503 (higher-value [503 200 404 422 200 408]))))))

(deftest returning-fix-status
  (let [fix-status #'status-code/fix-status]
    (testing "Should return highers"
      (is (= [200 500] (fix-status [204 nil])))
      (is (= [200 200 500 408] (fix-status [200 204 nil 408])))
      (is (= [503 200 404 422 200 408] (fix-status [503 200 404 422 200 408]))))))

(deftest calculating-response-status
  (let [calculate-response-status-code #'status-code/calculate-response-status-code]
    (testing "Should return higher status "
      (is (= 404
             (calculate-response-status-code {:hero {:details {:status 200}}
                                              :villain {:details {:status 404}}}))))
    (testing "Should return higher status "
      (is (= 408
             (calculate-response-status-code {:hero {:details {:status 200}}
                                              :villain {:details [{:status 408}]}}))))
    (testing "Should return higher status"
      (is (= 408
             (calculate-response-status-code {:hero {:details {:status 200}}
                                              :villain {:details [[{:status 408}]]}}))))
    (testing "Should return higher status"
      (is (= 503
             (calculate-response-status-code {:hero {:details {:status 200}}
                                              :villain {:details [[{:status 408}] [{:status 503}]]}}))))
    (testing "Should correct status 204 to 200 and return higher status"
      (is (= 200
             (calculate-response-status-code {:villain {:details {:status 204}}
                                              :hero {:details {:status 200}}}))))
    (testing "Should correct status 0 to 503 and return higher status"
      (is (= 503
             (calculate-response-status-code {:villain {:details {:status 0}}
                                              :hero {:details {:status 200}}}))))
    (testing "Should correct status nil to 500 and return higher status"
      (is (= 500
             (calculate-response-status-code {:villain {:details {}}
                                              :hero {:details {:status 200}}}))))
    (testing "Should ignore errors"
      (is (= 200
             (calculate-response-status-code {:hero {:details {:status 200}}
                                              :villain {:details {:metadata {:ignore-errors "ignore"}
                                                                  :status 404}}})))
      (is (= 200
             (calculate-response-status-code {:hero {:details {:status 200}}
                                              :villain {:details [[{:metadata {:ignore-errors "ignore"} :status 408}]
                                                                  [{:metadata {:ignore-errors "ignore"} :status 503}]]}})))
      (is (= 200
             (calculate-response-status-code {:hero {:details {:metadata {:ignore-errors "ignore"} :status 503}}
                                              :villain {:details [[{:metadata {:ignore-errors "ignore"} :status 200}]
                                                                  [{:metadata {:ignore-errors "ignore"} :status 503}]]}}))))))