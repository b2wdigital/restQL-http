(ns restql.config.core
  (:require [clojure.java.io :as io]
            [yaml.core :as yaml]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]))

(def default-values {:executor-utilization    0.5
                     :executor-max-threads    512
                     :executor-control-period 1000
                     :max-query-overhead-ms   50
                     :port                    9000
                     :cache-ttl               60000
                     :cache-count             2000
                     :mappings-ttl            60000
                     :query-global-timeout    30000
                     :allow-adhoc-queries     true
                     :tenant                  "DEFAULT"
                     :cors-allow-origin       "*"
                     :cors-allow-methods      "GET, POST, PUT, PATH, DELETE, OPTIONS"
                     :cors-allow-headers      "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
                     :cors-expose-headers     "Content-Length,Content-Range"})

(defn- from-env-or-defaut
  ([name]
   (from-env-or-defaut name nil))
  ([name default]
   (if (contains? env name)
     (read-string (env name))
     default)))

(defn- get-config-from-file
  [filename-with-path]
  (try
    (log/info "Getting configuration from" filename-with-path)
    (-> filename-with-path
        (slurp)
        (yaml/parse-string :keywords true))
    (catch Exception e
      (do (log/error "Error getting configuration from file:" filename-with-path "error:" (.getMessage e))
          {}))))

(defn get-env-map []
  (identity env))

(defn- build-config-map [filename]
  (-> {}
      (assoc :env (get-env-map))
      (assoc :config-file (get-config-from-file filename))
      (assoc :default default-values)))

(defn- env-to-config-translate [config-path]
  (cond
    (= config-path :cors-allow-origin)   [:cors :allow-origin]
    (= config-path :cors-allow-methods)  [:cors :allow-methods]
    (= config-path :cors-allow-headers)  [:cors :allow-headers]
    (= config-path :cors-expose-headers) [:cors :expose-headers]
    (keyword? config-path)               (conj [] config-path)
    :else                                config-path))

(defonce config-data
  (build-config-map (from-env-or-defaut :restql-config-file "restql.yml")))

(defn- search-in-config [key function]
  (->
    (get-in config-data [:env key])
    (as-> value (if (some? value) (function value) (get-in config-data (concat [:config-file] (env-to-config-translate key)))))
    (as-> value (if (some? value) value (get-in config-data [:default key])))))

(defn get-config
  "Gets part or all config data"
  ([] config-data)
  ([config-path]
   (get-config config-path read-string))
   ([config-path function]
    (search-in-config config-path function)))
