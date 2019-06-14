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
  
  (testing "Should not add header if there is an empty env or config variable"
    (with-redefs [config/config-data {:env     {:cors-allow-origin ""}
                                      :default {:cors-allow-origin       "*"
                                                :cors-allow-methods      "GET, POST, PUT, PATH, DELETE, OPTIONS"
                                                :cors-allow-headers      "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
                                                :cors-expose-headers     "Content-Length,Content-Range"}}]
      (is
       (= {"Access-Control-Allow-Methods"  "GET, POST, PUT, PATH, DELETE, OPTIONS"
           "Access-Control-Allow-Headers"  "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
           "Access-Control-Expose-Headers" "Content-Length,Content-Range"}
          (cors/fetch-cors-headers))))))
