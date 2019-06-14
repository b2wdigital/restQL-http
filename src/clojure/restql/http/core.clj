(ns restql.http.core
  (:require [restql.http.server.core :as server]
            [restql.http.plugin.core :as plugin]
            [restql.http.database.persistence :as db]
            [restql.config.core :as config]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn -main
  "Runs the restQL-server"
  [& args]

  (log/info "Starting the amazing restQL Server!")

  (db/connect!  (config/get-config :mongo-url))
  (plugin/load!)

  (server/start! {:port                    (config/get-config :port)
                  :executor-utilization    (config/get-config :executor-utilization)
                  :executor-max-threads    (config/get-config :executor-max-threads)
                  :executor-control-period (config/get-config :executor-control-period)}))
