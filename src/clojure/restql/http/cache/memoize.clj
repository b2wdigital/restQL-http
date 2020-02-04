(ns restql.http.cache.memoize
  (:require [clojure.core.memoize :as memo]))

(defn ttl [ttl-threshold function]
  (memo/ttl function {} :ttl/threshold ttl-threshold))

(defn fifo [size function]
  (memo/fifo function {} :fifo/threshold size))