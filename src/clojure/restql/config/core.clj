(ns restql.config.core
  (:require [yaml.core :as yaml]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]]))

(defonce config-data (atom nil))

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

(defn init!
  "Read the configuration yaml file from resources
  and store the parsed value into config-data"
  ([filename-with-path]
   (let [file (or filename-with-path "restql.yml")]
     (when (nil? @config-data)
       (reset! config-data (get-config-from-file file))))))

(defn get-config
  "Gets part or all config data"
  ([] @config-data)
  ([configPath]
   (get-in @config-data configPath)))

(defn from-env-or-default
  ([name]
   (from-env-or-default name nil))
  ([name default]
   (cond (contains? env name) (read-string (env name))
         (not (nil? default)) default
         :else nil)))
