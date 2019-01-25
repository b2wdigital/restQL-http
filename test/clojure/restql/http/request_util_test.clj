(ns restql.http.request-util-test
  (:require [clojure.test :refer :all]
            [restql.http.request-util :refer :all]))

(deftest test-headers-blacklist

  (testing "Is blacklist filtering headers"
    (is (=
          [true false false false false false true]
          (map header-allowed? {"x-content" "bla"
                                "host" "http://localhost:8080"
                                "content-type" "foo"
                                "content-length" "123456"
                                "connection" "lol"
                                "origin" "malicious"
                                "bar" "baz"})))))

(deftest test-add-headers-to-object

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
                                 {:with-headers {"foo" "bar"}})))))

(deftest test-merge-headers

  (testing "Is query string with merged headers"
    (is (=
          [:foo {:from :foo, :with-headers {"x-content" "bla"}}]
          (merge-headers {"x-content" "bla"
                          "host" "http://localhost:8080"}
                         [:foo {:from :foo}])))))

(deftest parse-restql-language-query

  (testing "Parse query should work for valid EDN"
    (is
      (= [:foo {:from :foo :method :get} :bar {:from :bar :method :get}]
         (parse "from foo\nfrom bar" {}))
      (= [:foo {:from :foo :method :get} :bar {:from :bar :method :get}]
         (parse "from foo\nfrom bar" {} true)))))

(deftest extract-body-from-request

  (testing "Parse query should work for valid EDN"
    (is
      (= "{\"foo\": \"bar\", \"sample\": 11.99, \"x\": 2, \"bool\": true}"
         (extract-body {:body "test/resources/sample_response.json"})))))


(deftest parsing-result-from-request

  (testing "Parse query should work for valid EDN"
    (is
      (= [:foo {:from :foo :method :get} :bazinga {:from :bar :with {:id "123"} :method :get}]
         (parse-req {:params {"id" 123}
                     :headers {}
                     :body "test/resources/sample_query.rql"})))))


(deftest retrieving-status-code-ok

  (testing "True if status code is 2XX"
    (is (true? (status-code-ok {:status 200}))))

  (testing "False if status code is > 2XX"
    (is (false? (status-code-ok {:status 422}))))

  (testing "False if status code is nil"
    (is (false? (status-code-ok {:status nil})))))


(deftest creating-a-valid-json-response

  (testing "If it's a valid json response"
    (is (=
          {:status  200
           :headers {"Content-Type" "application/json"}
           :body "{\"message\":{\"foo\":\"bar\"}}"}
          (json-output 200 {:foo "bar"})))))

(deftest handling-restql-manager-urls

  (testing "Creating correct revision link"
    (is (=
          "/run-query/foo/bar/1"
          (make-revision-link "foo" "bar" "1"))))

  (testing "Creating correct revision list link"
    (is (=
          "/ns/foo/query/bar"
          (make-revision-list-link "foo" "bar"))))

  (testing "Creating correct saved query link"
    (is (=
          "/ns/foo/query/bar/revision/3"
          (make-query-link "foo" "bar" 3)))))


(deftest ignoring-errors-when-flagged

  (testing "Should return true for ignore"
    (is (true? (should-ignore-errors {:details {:metadata {:ignore-errors "ignore"}}}))))

  (testing "Should return false if no metadata provided"
    (is (false? (should-ignore-errors {:details {}})))))


(deftest returning-higher-value

  (testing "Should return highers"
    (is (=
          [503 200 404 422 200 408]
          [(higher-value 503 200)
           (higher-value 200 200)
           (higher-value 404 301)
           (higher-value 200 422)
           (higher-value nil 200)
           (higher-value 408 nil)]))))

(deftest calculating-response-status
  (testing "Should return higher status "
    (is (=
         404
         (calculate-response-status-code {:hero {:details {:status 200}}
                                          :villain {:details {:status 404}}}))))
  
  (testing "Should correct and return higher status"
    (is (=
         503
         (calculate-response-status-code {:villain {:details {:status 0}}
                                          :hero {:details {:status 200}}})))))

(deftest test-map-values
  (is (= {:foo 2 :bar 3}
         (map-values #(+ 1 %) {:foo 1 :bar 2}))))


(deftest formatting-restql-response

  (testing "Headers should not be in a response"
    (is (=
          {:metadata {:x 1}}
          (format-response-details {:headers {"foo" "bar"} :metadata {:x 1}}))))

  (testing "Format should remove headers"
    (is (=
          {:details {:metadata {:x 1}}}
          (format-response-item {:details {:headers {"foo" "bar"} :metadata {:x 1}}})))))


(deftest matching-contextual-items

  (testing "Should filter only params with key starting with 'x-'"
    (is (=
          [[:x-custom "123"]
           [:x-another "abc"]]
          (filter #(is-contextual? "x-" %) {:x-custom "123"
                                            :x-another "abc"
                                            :user-agent "foo"
                                            :movie "Star Wars"})))))