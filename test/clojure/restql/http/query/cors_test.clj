(ns restql.http.query.cors-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [restql.config.core :as config]            
            [restql.http.server.cors :as cors]))

(deftest environment-cors-test
  (testing "Should transform empty environment variable into empty strings"
    (with-redefs-fn {#'environ.core/env (fn [key] "")}
      #(let [get-from-env #'cors/get-from-env]
         (is
          (= ""
             (get-from-env :cors-allow-origin))))))
  
  (testing "Should transform null environment variable into empty strings"
    (with-redefs-fn {#'environ.core/env (fn [key] nil)}
      #(let [get-from-env #'cors/get-from-env]
         (is
          (= ""
             (get-from-env :cors-allow-origin)))))))

(deftest get-from-config-test
  (testing "Should follow CORS headers priority ENV > Config File > Default"
    (reset! config/config-data {:cors {:allow-origin "xyz"}})
    
    (with-redefs-fn {#'environ.core/env (fn [key] nil)}
      #(let [get-from-config #'cors/get-from-config]
         (is
          (= "xyz"
             (get-from-config :cors-allow-origin)))))

    (reset! config/config-data {})))

(deftest assoc-existing-header-test
  (testing "Should return input map if env variable is empty"
    (with-redefs-fn {#'cors/get-from-env (fn [key] "")}
      #(let [assoc-existing-header #'cors/assoc-existing-header]
         (is
          (= {}
             (assoc-existing-header {} "Access-Control-Allow-Origin" :cors-allow-origin))))))
  
  (testing "Should return header map if env variable is present"
    (let [assoc-existing-header #'cors/assoc-existing-header]
      (is
       (= {"Access-Control-Allow-Origin" "abc"}
          (assoc-existing-header {} "Access-Control-Allow-Origin" :cors-allow-origin))))))

(deftest fetch-cors-headers-test
  (testing "Should get right headers"
    (is 
     (= {"Access-Control-Allow-Origin"  "abc"
         "Access-Control-Allow-Methods" "GET, POST, PUT, PATH, DELETE, OPTIONS"
         "Access-Control-Allow-Headers" "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
         "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
        (cors/fetch-cors-headers)))))