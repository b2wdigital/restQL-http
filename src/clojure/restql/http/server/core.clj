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
  (http/start-server handler
                     {:port port
                      :executor (flow/utilization-executor executor-utilization
                                                           executor-max-threads
                                                           {:metrics (EnumSet/of Stats$Metric/UTILIZATION)
                                                            :control-period executor-control-period
                                                            :initial-thread-count (/ executor-max-threads 2)
                                                            :stats-callback stats-callback-fn})}))

(defonce server
  (atom nil))

(defn start!
  "Starts the restQL http server"
  [{:keys [port
           executor-utilization
           executor-max-threads
           executor-control-period]
    :or {port 9000
         executor-utilization 0.9
         executor-max-threads 512
         executor-control-period 1000}}]
  (log/info "Server starting with" {:port port
                                     :executor-utilization executor-utilization
                                     :executor-max-threads executor-max-threads
                                     :executor-control-period executor-control-period})
  (reset! server (start-server port
                               executor-utilization
                               executor-max-threads
                               executor-control-period))
  (log/info "Server started!"))

(defn stop!
  "Stops the server"
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))
