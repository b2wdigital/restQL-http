(ns restql.http.async-handler-test
  (:require [clojure.test :refer :all]
            [slingshot.test]
            [slingshot.slingshot :refer [throw+]]
            [restql.http.request.queries :as request-queries])
  (:use restql.http.async-handler))

(deftest test-query-no-found
  (testing "Is return for query not found"
    (with-redefs [request-queries/get-query (fn [_ _ _] (throw+ {:type :query-not-found}))]
      (is (= {:status 404
              :headers {"Content-Type" "application/json"}
              :body "{\"message\":{\"error\":\"QUERY_NO_FOUND\"}}"}
             (run-saved-query {:params {:namespace "ns", :id "id-1", :rev "5"}}))))))
