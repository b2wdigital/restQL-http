(ns restql.http.request.queries-test
  (:require [clojure.test :refer :all]
            [slingshot.test]
            [restql.config.core :as config]
            [restql.http.database.core :as dbcore])
  (:use restql.http.request.queries))

(deftest get-query-from-config-test
  (testing "Is getting from config data"
    (reset! config/config-data {:queries {:my-namespace {:my-query ["my query version 1" "my query version 2"]}}})
    (is
     (= "my query version 1"
        (get-query-from-config "my-namespace" "my-query" 1)))
    (is
     (= "my query version 2"
        (get-query-from-config "my-namespace" "my-query" 2)))
    (reset! config/config-data nil)))

(deftest throw-exception-if-query-not-found
  (testing "Is throwing exception if nil returned"
    (with-redefs [config/get-config (fn [_] nil)
                  dbcore/find-query-by-id-and-revision (fn [_ _ _] nil)]
      (is (thrown+-with-msg? map? #"\{:type :query-not-found\}" (get-query-from-config "my-namespace" "my-query" 1)))
      (is (thrown+-with-msg? map? #"\{:type :query-not-found\}" (get-query {:namespace "my-namespace"
                                                                            :query "my-query"
                                                                            :revision 1}))))))