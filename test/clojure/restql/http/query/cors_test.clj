(ns restql.http.query.cors-test
  (:require [clojure.test :refer :all]
            [environ.core :refer [env]]
            [restql.config.core :as config]            
            [restql.http.server.cors :as cors]))

(deftest fetch-cors-headers-test
  (testing "Should get default headers if there are no env or config variables"
    (is
     (= {"Access-Control-Allow-Origin"   "*"
         "Access-Control-Allow-Methods"  "GET, POST, PUT, PATH, DELETE, OPTIONS"
         "Access-Control-Allow-Headers"  "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
         "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
        (cors/fetch-cors-headers))))

  (testing "Should remove header if env variable is empty"
    (with-redefs-fn {#'environ.core/env (fn [key]
                                          (if (= key :cors-allow-origin) ""))}
      #(is
        (= {"Access-Control-Allow-Methods"  "GET, POST, PUT, PATH, DELETE, OPTIONS"
            "Access-Control-Allow-Headers"  "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
            "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
           (cors/fetch-cors-headers)))))

  (testing "Should use default header if env variable is null"
    (with-redefs-fn {#'environ.core/env (fn [key]
                                          (if (= key :cors-allow-origin) nil))}
      #(is
        (= {"Access-Control-Allow-Origin"   "*"
            "Access-Control-Allow-Methods"  "GET, POST, PUT, PATH, DELETE, OPTIONS"
            "Access-Control-Allow-Headers"  "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
            "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
           (cors/fetch-cors-headers)))))

  (testing "Should remove header if config variable is empty"
    (reset! config/config-data {:cors {:allow-methods ""}})

    (is
     (= {"Access-Control-Allow-Origin"  "*"
         "Access-Control-Allow-Headers" "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
         "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
        (cors/fetch-cors-headers))))

  (reset! config/config-data {})

  (testing "Should use default header if config variable is null"
    (reset! config/config-data {:cors {:allow-methods nil}})

    (is
     (= {"Access-Control-Allow-Origin"  "*"
         "Access-Control-Allow-Methods"  "GET, POST, PUT, PATH, DELETE, OPTIONS"
         "Access-Control-Allow-Headers" "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
         "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
        (cors/fetch-cors-headers))))

  (reset! config/config-data {})

  (testing "Should follow CORS headers priority ENV > Config File > Default"
    (reset! config/config-data {:cors {:allow-origin "http://www.another.example.com"
                                       :allow-methods "GET,POST"}})
    
    (with-redefs-fn {#'environ.core/env (fn [key]
                                          (if (= key :cors-allow-origin) "http://www.example.com"))}
      #(is
        (= {"Access-Control-Allow-Origin"  "http://www.example.com"
            "Access-Control-Allow-Methods" "GET,POST"
            "Access-Control-Allow-Headers" "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
            "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
           (cors/fetch-cors-headers))))

      (reset! config/config-data {})))