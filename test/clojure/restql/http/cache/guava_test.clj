(ns restql.http.cache.guava-test
  (:require [clojure.test :refer :all]
            [slingshot.test]
            [slingshot.slingshot :refer [throw+]]
            [restql.http.cache.guava :as cache]))

(deftest wrap-function-that-will-load-values-to-cache
  (deftest with-no-previous-value
    (testing "It should return nothing if function throw an exception"
      (let [wrapped-fn (cache/wrap-load-fn (fn [_] (throw+ {:type :some-exception})))]
        (is (thrown+-with-msg? map? #"\{:type :some-exception\}" (wrapped-fn ["some-key"])))))

    (testing "It should return nothing if function return nothing"
      (let [wrapped-fn (cache/wrap-load-fn (fn [_] (identity nil)))]
        (is (= nil
               (wrapped-fn ["some-key"])))))

    (testing "It should return nothing if function return an empty value"
      (let [wrapped-fn (cache/wrap-load-fn (fn [_] (identity {})))]
        (is (= nil
               (wrapped-fn ["some-key"])))))

    (testing "It should return the value if function succeeds"
      (let [wrapped-fn (cache/wrap-load-fn #(str "hello-" %))]
        (is (= "hello-some-key"
               (wrapped-fn ["some-key"]))))))

  (deftest with-previous-value
    (testing "It should return previous value if function throw an exception"
      (let [wrapped-fn (cache/wrap-load-fn #(throw (Exception.)))]
        (is (= "hello"
               (wrapped-fn ["some-key"] :previous-value "hello")))))

    (testing "It should return previous value if function return nothing"
      (let [wrapped-fn (cache/wrap-load-fn #(identity nil))]
        (is (= "hello"
               (wrapped-fn ["some-key"] :previous-value "hello")))))

    (testing "It should return previous value if function return an empty value"
      (let [wrapped-fn (cache/wrap-load-fn #(identity {}))]
        (is (= "hello"
               (wrapped-fn ["some-key"] :previous-value "hello")))))

    (testing "It should return the value if function succeeds"
      (let [wrapped-fn (cache/wrap-load-fn #(str "hello-" %))]
        (is (= "hello-some-key"
               (wrapped-fn ["some-key"] :previous-value "hello")))))))