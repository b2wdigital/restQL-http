(ns restql.http.query.status-code-test
  (:require [clojure.test :refer :all]
            [restql.http.query.calculate-response-status-code :as status-code]))

(deftest ignoring-errors-when-flagged
  
  (let [should-ignore-errors #'status-code/should-ignore-errors]
    (testing "Should return true for ignore"
      (is (true? (should-ignore-errors {:details {:metadata {:ignore-errors "ignore"}}}))))

    (testing "Should return false if no metadata provided"
      (is (false? (should-ignore-errors {:details {}}))))))


(deftest returning-higher-value
  (let [higher-value #'status-code/higher-value]
    (testing "Should return highers"
      (is (=
           [503 200 404 422 200 408]
           [(higher-value 503 200)
            (higher-value 200 200)
            (higher-value 404 301)
            (higher-value 200 422)
            (higher-value nil 200)
            (higher-value 408 nil)])))))

(deftest calculating-response-status
  (let [calculate-response-status-code #'status-code/calculate-response-status-code]
    (testing "Should return higher status "
      (is (=
           404
           (calculate-response-status-code {:hero {:details {:status 200}}
                                            :villain {:details {:status 404}}}))))

    (testing "Should correct status 204 to 200 and return higher status"
      (is (=
           200
           (calculate-response-status-code {:villain {:details {:status 204}}
                                            :hero {:details {:status 200}}}))))

    (testing "Should correct status 0 to 503 and return higher status"
      (is (=
           503
           (calculate-response-status-code {:villain {:details {:status 0}}
                                            :hero {:details {:status 200}}}))))))