(ns restql.http.cache.guava
  (:require [clojure.tools.logging :as log]
            [restql.config.core :as config])
  (:import (java.util.concurrent TimeUnit Executors)
           (com.google.common.cache CacheBuilder CacheLoader LoadingCache)
           (com.google.common.util.concurrent MoreExecutors ListeningExecutorService)))

(defonce ^ListeningExecutorService
         executor-service
         (-> (config/from-env-or-default :cache-thread-pool-size 1)
             (Executors/newFixedThreadPool)
             (MoreExecutors/listeningDecorator)))

(defn wrap-load-fn [f]
  (fn [keys & {:keys [previous-value]}]
    (try
      (let [new-value (apply f keys)]
        (if (and (not (nil? new-value)) (not (empty? new-value)))
          new-value
          previous-value))
      (catch Exception e
        (log/warn e "Cache loader function failed")
        (if-not (nil? previous-value)
          previous-value
          (throw e))))))

(defn- ^CacheLoader cache-loader [f]
  (let [loader (wrap-load-fn f)]
    (proxy [CacheLoader] []
      (load [keys]
        (loader keys))
      (reload [keys previous-value]
        (.submit executor-service (proxy [Callable] []
                                    (call []
                                      (loader keys :previous-value previous-value))))))))

(defn- make-cache [f options]
  (let [builder (CacheBuilder/newBuilder)]
    (when-let [ttl (:ttl/threshold options)]
      (.refreshAfterWrite builder ttl TimeUnit/MILLISECONDS))
    (when-let [size (:fifo/threshold options)]
      (.maximumSize builder size))
    (.build builder (cache-loader f))))

(defn- get
  ([^LoadingCache c keys]
   (.get c keys)))

(defn decorate-with-cache [f options]
  (let [cache (make-cache f options)]
    (fn [& args]
      (get cache args))))