(ns restql.http.core
  (:require [restql.http.server.core :as server]
            [restql.http.plugin.core :as plugin]
            [clojure.tools.logging :as log]
            [restql.http.database.persistence :as db]
            [restql.hooks.core :as hooks]
            [restql.config.core :as config]
            [environ.core :refer [env]])
  (:gen-class))

(defn get-port
  "Gets a port to run the server api, or the default api port"
  [default]

  (if (contains? env :port) (read-string (env :port))  default))

(defn get-executor-utilization
  "Gets sizes the thread pool according to target utilization, within `[0,1]`"
  [default]
  (if (contains? env :executor-utilization) (read-string (env :executor-utilization)) default))

(defn get-executor-max-threads
  "Gets sizes the thread pool according to target utilization, within `[0,1]`"
  [default]
  (if (contains? env :executor-max-threads) (read-string (env :executor-max-threads)) default))

(defn get-executor-control-period
  "Gets executor control period in milliseconds"
  [default]
  (if (contains? env :executor-control-period) (read-string (env :executor-control-period)) default))

(defn start-api?
  "Verifies if it should start the api"
  []

  (if (contains? env :run-api)
    (= "true" (:run-api env))
    true))

(defn display-loaded-plugins!
  "Prints the plugin information to the logger as info"
  []

  (doseq [p (plugin/get-loaded-plugins)]
    (log/info "Loaded: "(:name p))))

(defn connect-to-mongo []
  (if (:mongo-url env)
    (do
      (log/info "Connecting to MongoDB:" (:mongo-url env))
      (db/connect! (:mongo-url env))
    )
  )
)

(defn -main
  "Runs the restQL-server"
  [& args]

  (let [port (get-port 9000)
        executor-utilization (get-executor-utilization 0.9)
        executor-max-threads (get-executor-max-threads 512)
        executor-control-period (get-executor-control-period 1000)]
    (log/info "Starting the amazing restQL Server!")

    (config/init!)
    (connect-to-mongo)

    (log/info "Loading plugins")
    (plugin/load-plugins!)
    (display-loaded-plugins!)
    (plugin/register-plugins!)
    (when (start-api?)
      (server/start! {:port port
                      :executor-utilization executor-utilization
                      :executor-max-threads executor-max-threads
                      :executor-control-period executor-control-period}))))
