(defproject restql-http "v2.8.20" :description "restQL HTTP"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://restql.b2w.io"}
  :dependencies [[b2wdigital/restql-core "2.8.19"]
                 [org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/java.classpath "0.3.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [aleph "0.4.6"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.8.1"]
                 [com.novemberain/monger "3.5.0"]
                 [compojure "1.6.1"]
                 [environ "1.1.0"]
                 [io.forward/yaml "1.0.9"]
                 [org.flatland/ordered "1.5.7"] ;https://github.com/owainlewis/yaml/issues/28
                 [org.mongodb/mongodb-driver "3.10.1"] ;monger uses mongodb-driver-3.9.1
                 [org.slf4j/slf4j-api "1.7.26"]
                 [slingshot "0.12.2"]]
  :plugins [[lein-autoexpect "1.9.0"]
            [lein-environ "1.1.0"]
            [lein-expectations "0.0.8"]
            [lein-ring "0.12.5"]
            [single-file-lein-zip "0.1.0"]]
  :main ^:skip-aot restql.http.core
  :target-path "target/%s"
  :source-paths ["src/clojure"]
  :test-paths  ["test/clojure"]
  :resource-paths ["plugins/first.jar" "src/resources"]
  :zip ["Dockerfile" {:file-name "restql-http-standalone.jar" :file-folder "target/uberjar"}]
  :uberjar-name "restql-http-standalone.jar"
  :profiles {:uberjar {:aot :all
                       :env {:port      "9000"
                             :cache-ttl "30000"}}
             :dev     {:env     {:port      "9000"
                                 :cache-ttl "30000"
                                 :mappings-cache-ttl "60000"
                                 :cards     "http://api.magicthegathering.io/v1/cards"
                                 :card      "http://api.magicthegathering.io/v1/cards/:id"
                                 :planets   "https://swapi.co/api/planets/:id"}
                       :plugins [[lein-cloverage "1.0.9"]]}})
