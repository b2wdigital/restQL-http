(ns restql.http.server.cors
  (:require [restql.config.core :as config]))

(defn- assoc-header-if-not-empty [map header-name value]
  (if (empty? value)
    map
    (assoc map header-name value)))

(defn fetch-cors-headers []
  (-> {}
      (assoc-header-if-not-empty "Access-Control-Allow-Origin"   (config/get-config :cors-allow-origin identity))
      (assoc-header-if-not-empty "Access-Control-Allow-Methods"  (config/get-config :cors-allow-methods identity))
      (assoc-header-if-not-empty "Access-Control-Allow-Headers"  (config/get-config :cors-allow-headers identity))
      (assoc-header-if-not-empty "Access-Control-Expose-Headers" (config/get-config :cors-expose-headers identity))))
