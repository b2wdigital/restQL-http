(ns restql.http.query.json-output-test
  (:require [clojure.test :refer :all]
            [restql.http.query.json-output :refer [json-output]]))

(deftest creating-a-valid-json-response
  (testing "If it's a valid json response"
    (is (=
         {:status  200
          :headers {"Content-Type" "application/json"}
          :body "{\"foo\":\"bar\"}"}
         (json-output {:status 200 :body {:foo "bar"}})))))
