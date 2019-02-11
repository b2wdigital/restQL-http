(defproject restql-http "v2.8.8" :description "restQL HTTP"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://restql.b2w.io"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [b2wdigital/restql-core "2.8.7"]
                 [aleph "0.4.6"]
                 [compojure "1.6.1"]
                 [environ "1.0.0"]
                 [prismatic/schema "1.1.2"]
                 [expectations "2.0.9"]
                 [slingshot "0.12.2"]
                 [cheshire "5.8.1"]
                 [io.forward/yaml "1.0.9"]
                 [com.novemberain/monger "3.1.0"]
                 [prismatic/schema "1.1.7"]
                 [commons-validator "1.5.1"]
                 [org.clojure/core.async "0.3.465"]
                 [org.clojure/java.classpath "0.2.3"]
                 [org.clojure/tools.reader "1.0.5"]
                 [org.clojure/tools.reader "1.0.5"]
                 [org.mongodb/mongodb-driver "3.9.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [ch.qos.logback/logback-classic "1.2.3"]]
  :plugins [[lein-expectations "0.0.8"]
            [lein-autoexpect "1.4.0"]
            [lein-environ "1.0.0"]
            [lein-ring "0.12.1"]
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
