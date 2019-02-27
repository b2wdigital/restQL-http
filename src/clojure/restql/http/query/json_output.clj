(ns restql.http.query.json-output
  (:require [cheshire.core :as json]
            [clojure.walk :refer [stringify-keys]]))

(defn- add-content-type [response]
  (->> (:headers response)
       (stringify-keys)
       (into {"Content-Type" "application/json"})))

(defn json-output
  "Creates a json output given it's status and body (message)"
  [response]

  {:status  (:status response)
   :headers (add-content-type response)
   :body    (json/generate-string (:body response))})
