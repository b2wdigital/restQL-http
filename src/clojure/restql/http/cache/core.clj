(ns restql.http.cache.core
  (:require [restql.config.core :as config]
            [clojure.core.memoize :as memo]))

(defmulti cached
  "Verifies if a given function is cached, executing and saving on the cache
   if not cached or returning the cached value"
  (fn [type & _] type))

(defmethod cached :ttl
  ([_:ttl function] (cached :ttl (config/get-config :cache-ttl) function))
  ([_:ttl ttl function] (memo/ttl function {} :ttl/threshold ttl)))

(defmethod cached :fifo
  ([_:fifo function] (cached :fifo (config/get-config :cache-count) function))
  ([_:fifo cached_count function] (memo/fifo function {} :fifo/threshold cached_count)))