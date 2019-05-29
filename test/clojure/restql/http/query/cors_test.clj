(ns restql.http.query.cors-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [restql.config.core :as config]            
            [restql.http.server.cors :as cors]))

(deftest environment-cors-test
  (testing "Should transform empty environment variable into empty strings"
    (with-redefs-fn {#'environ.core/env (fn [key] "")}
      #(let [get-from #'cors/get-from
             env      #'environ.core/env]
         (is
          (= ""
             (get-from env :cors-allow-origin))))))
  
  (testing "Should transform null environment variable into empty strings"
    (with-redefs-fn {#'environ.core/env (fn [key] nil)}
      #(let [get-from #'cors/get-from
             env      #'environ.core/env]
         (is
          (= nil
             (get-from env :cors-allow-origin)))))))

(deftest get-from-config-test
  (testing "Should get value from config"
    (reset! config/config-data {:cors {:allow-origin "xyz"}})
    
    (with-redefs-fn {#'environ.core/env (fn [key] nil)}
      #(let [get-from #'cors/get-from 
             config-file-cors-headers #'cors/config-file-cors-headers]
         (is
          (= "xyz"
             (get-from config-file-cors-headers :cors-allow-origin)))))

    (reset! config/config-data {}))
  
  (testing "Should set empty value to empty string"
    (reset! config/config-data {:cors {:allow-origin ""}})

    (with-redefs-fn {#'environ.core/env (fn [key] nil)}
      #(let [get-from #'cors/get-from
             config-file-cors-headers #'cors/config-file-cors-headers]
         (is
          (= ""
             (get-from config-file-cors-headers :cors-allow-origin)))))

    (reset! config/config-data {})))

(deftest assoc-header-if-not-empty-test
  (testing "Should return input map if env variable is empty"
    (with-redefs-fn {#'environ.core/env (fn [key] "")}
      #(let [assoc-header-if-not-empty #'cors/assoc-header-if-not-empty]
         (is
          (= {}
             (assoc-header-if-not-empty {} "Access-Control-Allow-Origin" :cors-allow-origin))))))
  
  (testing "Should return header map if env variable is present"
    (let [assoc-header-if-not-empty #'cors/assoc-header-if-not-empty]
      (is
       (= {"Access-Control-Allow-Origin" "abc"}
          (assoc-header-if-not-empty {} "Access-Control-Allow-Origin" :cors-allow-origin))))))

(deftest fetch-cors-headers-test
  (testing "Should get right headers"
    (is 
     (= {"Access-Control-Allow-Origin"  "abc"
         "Access-Control-Allow-Methods" "GET, POST, PUT, PATH, DELETE, OPTIONS"
         "Access-Control-Allow-Headers" "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
         "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
        (cors/fetch-cors-headers)))))