(ns restql.http.query.headers-test
  (:require [clojure.test :refer :all]
            [restql.http.query.headers :as headers]))

(deftest header-allowed-test
  (is (= {"restql-query-control" "ad-hoc", "user-agent" "insomnia/6.3.2", "teste" "teste", "accept" "*/*"}
         (headers/header-allowed {:type :ad-hoc} {:headers {"host" "localhost:9000", "user-agent" "insomnia/6.3.2", "content-length" "96", "teste" "teste", "accept" "*/*"}}))))
