(ns restql.http.cache.memoize
  (:require [environ.core :refer [env]]
            [clojure.core.memoize :as memo]))

(def DEFAULT_TTL (if (contains? env :cache-ttl) (read-string (env :cache-ttl)) 60000))

(def DEFAULT_CACHED_COUNT (if (contains? env :cache-count) (read-string (env :cache-count)) 2000))

(defmulti cached
          "Verifies if a given function is cached, executing and saving on the cache
           if not cached or returning the cached value"
          (fn [type & _] type))

(defmethod cached :ttl
  ([_:ttl function] (cached :ttl DEFAULT_TTL function))
  ([_:ttl ttl function] (memo/ttl function {} :ttl/threshold ttl)))

(defmethod cached :fifo
  ([_:fifo function] (cached :fifo DEFAULT_CACHED_COUNT function))
  ([_:fifo cached_count function] (memo/fifo function {} :fifo/threshold cached_count)))