(ns restql.http.cache.core
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+]]
            [environ.core :refer [env]]
            [clojure.core.memoize :as memo]))

(def DEFAULT_TTL (if (contains? env :cache-ttl) (read-string (env :cache-ttl)) 60000))

(defn cached
  "Verifies if a given function is cached, executing and saving on the cache
   if not cached or returning the cached value"
  ([function]

   (cached function DEFAULT_TTL))

  ([function ttl]
   (memo/ttl function {} :ttl/threshold ttl)))

(defn clear-cache
  "Clears the cache"
  [cached-fn]

  (memo/memo-clear! cached-fn))
