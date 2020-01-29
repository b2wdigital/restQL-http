(ns restql.http.core
  (:require [restql.http.server.core :as server]
            [restql.http.plugin.core :as plugin]
            [restql.http.database.persistence :as db]
            [restql.config.core :as config]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:gen-class))

(defn- from-env-or-default
  ([name]
   (from-env-or-default name nil))
  ([name default]
   (cond (contains? env name) (read-string (env name))
         (not (nil? default)) default
         :else nil)))

(defn -main
  "Runs the restQL-server"
  [& args]

  (log/info "Starting the amazing restQL Server!")

  (config/init! (:restql-config-file env))
  (db/connect!  (env :mongo-url))
  (plugin/load!)

  (server/start! {:port                    (from-env-or-default :port 9000)
                  :executor-utilization    (from-env-or-default :executor-utilization 0.5)
                  :executor-max-threads    (from-env-or-default :executor-max-threads 512)
                  :executor-control-period (from-env-or-default :executor-control-period 1000)}))
