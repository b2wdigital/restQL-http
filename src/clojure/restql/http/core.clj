(ns restql.http.core
  (:require [restql.http.server.core :as server]
            [restql.http.plugin.core :as plugin]
            [restql.http.database.persistence :as db]
            [restql.config.core :as config]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]
            [restql.config.core :as config])
  (:gen-class))

(defn -main
  "Runs the restQL-server"
  [& args]

  (log/info "Starting the amazing restQL Server!")

  (config/init! (:restql-config-file env))
  (db/connect!  (env :mongo-url))
  (plugin/load!)

  (server/start! {:port                    (config/from-env-or-default :port 9000)
                  :executor-utilization    (config/from-env-or-default :executor-utilization 0.5)
                  :executor-max-threads    (config/from-env-or-default :executor-max-threads 512)
                  :executor-control-period (config/from-env-or-default :executor-control-period 1000)}))
