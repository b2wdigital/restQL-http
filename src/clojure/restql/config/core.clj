(ns restql.config.core
  (:require [clojure.java.io :as io]
            [yaml.core :as yaml]
            [clojure.tools.logging :as log])
)

(defonce config-data (atom nil))

(defn- get-config-from-file
  [filename]
  (try
    (->
      filename
      (io/resource)
      (slurp)
      (yaml/parse-string :keywords true)
    )
    (catch Exception e
      (do
        (log/error "Error getting config file:" (str "resources/" filename) "error:" (.getMessage e))
        {}
      )
    )
  )
)

(defn init!
  "Read the configuration yaml file from resources
  and store the parsed value into config-data"
  ([]
    (init! "restql.yml"))
  ([filename]
    (
      when (nil? @config-data)
      (reset! config-data (get-config-from-file filename))
    )
  )
)

(defn get-config
  "Gets part or all config data"
  ([] @config-data)
  ([configPath]
    (get-in @config-data configPath)
  )
)
