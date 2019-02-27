(ns restql.http.query.json-output
  (:require [cheshire.core :as json]
            [clojure.walk :refer [stringify-keys]]))

(defn- add-content-type [xpto]
  (->> (:headers xpto)
       (stringify-keys)
       (into {"Content-Type" "application/json"})))

(defn json-output
  "Creates a json output given it's status and body (message)"
  [xpto]

  {:status  (:status xpto)
   :headers (add-content-type xpto)
   :body    (json/generate-string (:body xpto))})