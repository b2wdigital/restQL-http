(ns restql.http.query.mappgins-test
  (:require [clojure.test :refer :all]
            [restql.http.query.mappings :as mappings]
            [restql.config.core :as config])
)

(deftest get-mappings-from-tenant-test
  (testing "Is getting from config data"
    (let [get-mappings-from-config #'mappings/get-mappings-from-config]
      (reset! config/config-data {:mappings {:test "test.com"}})
      (is
       (= {:test "test.com"}
          (get-mappings-from-config)))
      (reset! config/config-data nil))))
