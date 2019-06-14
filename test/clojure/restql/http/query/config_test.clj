(ns restql.http.query.config-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [restql.config.core :as config]))

(deftest get-config-map-test
  (testing "Default case - should get all values"
    (is (contains? (config/get-config) :env))
    (is (contains? (config/get-config) :config-file))
    (is (contains? (config/get-config) :default)))

  (testing "Should accept empty string environment variable"
    (with-redefs-fn {#'config/get-env-map (fn [] {:cors-allow-origin ""})}
      #(with-redefs [config/config-data (#'config/build-config-map "restql.yml")]
         (is
          (= ""
             (config/get-config :cors-allow-origin identity))))))

  (testing "Should use default value if env variable is null"
    (with-redefs-fn {#'config/get-env-map (fn [] {:cors-allow-origin nil})}
      #(with-redefs [config/config-data (#'config/build-config-map "restql.yml")]
         (is
          (= "*"
             (config/get-config :cors-allow-origin identity))))))

  (testing "Should use default value if env variable is null"
    (with-redefs-fn {#'config/get-config-from-file (fn [filename] {:cors {:allow-origin ""}})}
      #(with-redefs [config/config-data (#'config/build-config-map "restql.yml")]
         (is
          (= ""
             (config/get-config :cors-allow-origin identity))))))

  (testing "Should use default value if env variable is null"
    (with-redefs-fn {#'config/get-config-from-file (fn [filename] {:cors {:allow-origin nil}})}
      #(with-redefs [config/config-data (#'config/build-config-map "restql.yml")]
         (is
          (= "*"
             (config/get-config :cors-allow-origin identity))))))
  
  (testing "Should follow priority ENV > Config File > Default"
    (with-redefs-fn {#'config/get-env-map (fn [] {:cors-allow-origin "https://example.com"})
                     #'config/get-config-from-file (fn [filename] {:cors {:allow-origin "https://another.example.com"}})}
      #(with-redefs [config/config-data (#'config/build-config-map "restql.yml")]
         (is
          (= "https://example.com"
             (config/get-config :cors-allow-origin identity)))))))