(ns restql.http.server.core
  (:require [aleph.http :as http]
            [aleph.flow :as flow]
            [clojure.tools.logging :as log]
            [restql.http.server.handler :refer [handler]]
            [restql.hooks.core :as hooks])
  (:import [java.util EnumSet]
           [io.aleph.dirigiste Stats$Metric]))

(defn- stats-callback-fn [stats]
  (hooks/execute-hook :stats-server-executor (assoc {} :stats stats)))

(defn- start-server [port
                     executor-utilization
                     executor-max-threads
                     executor-control-period]
  (-> (hooks/execute-hook-pipeline :handler handler)
      (http/start-server {:port port
                          :executor (flow/utilization-executor executor-utilization
                                                               executor-max-threads
                                                               {:metrics (EnumSet/of Stats$Metric/UTILIZATION)
                                                                :control-period executor-control-period
                                                                :initial-thread-count (/ executor-max-threads 2)
                                                                :stats-callback stats-callback-fn})})))

(defonce server
  (atom nil))

(defn start!
  "Starts the restQL http server"
  [{:keys [port executor-utilization executor-max-threads executor-control-period]}]
  (let [cfg {:port (or port 9000)
             :executor-utilization (or executor-utilization 0.9)
             :executor-max-threads (or executor-max-threads 512)
             :executor-control-period (or executor-control-period 1000)}]
    (log/info "Server starting with" cfg)
    (reset! server (start-server (:port cfg)
                                 (:executor-utilization cfg)
                                 (:executor-max-threads cfg)
                                 (:executor-control-period cfg)))
    (log/info "Server started!")))

(defn stop!
  "Stops the server"
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))
