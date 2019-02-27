(ns restql.http.query.headers-test
  (:require [clojure.test :refer :all]
            [restql.http.query.headers :as headers]))

(deftest test-headers-blacklist
  (let [header-allowed? #'headers/header-allowed?]
    (testing "Is blacklist filtering headers"
      (is (=
           [true false false false false false true]
           (map header-allowed? {"x-content" "bla"
                                 "host" "http://localhost:8080"
                                 "content-type" "foo"
                                 "content-length" "123456"
                                 "connection" "lol"
                                 "origin" "malicious"
                                 "bar" "baz"}))))))

(deftest test-add-headers-to-object
  (let [add-headers-to-object #'headers/add-headers-to-object]
    (testing "Is object merging headers if it doesn't have :with-headers"
      (is (=
           {:with-headers {"x-content" "bla"}}
           (add-headers-to-object {"x-content" "bla"
                                   "host" "http://localhost:8080"}
                                  {}))))

    (testing "Is object merging headers if it has :with-headers"
      (is (=
           {:with-headers {"x-content" "bla" "foo" "bar"}}
           (add-headers-to-object {"x-content" "bla"
                                   "host" "http://localhost:8080"}
                                  {:with-headers {"foo" "bar"}}))))))

(deftest test-query-with-foward-headers

  (testing "Is adding headers to object"
    (is (=
         [:foo {:from :foo, :with-headers {"x-content" "bla"}}]
         (headers/query-with-foward-headers {"x-content" "bla"}
                                            [:foo {:from :foo}]))))
  
  (testing "Is skipping blacklisted headers"
    (is (=
         [:foo {:from :foo, :with-headers {"x-content" "bla"}}]
         (headers/query-with-foward-headers {"x-content" "bla"
                                             "host" "http://localhost:8080"}
                                            [:foo {:from :foo}])))))