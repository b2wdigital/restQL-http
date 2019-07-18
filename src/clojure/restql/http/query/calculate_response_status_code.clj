(ns restql.http.query.calculate-response-status-code)

(defn- not-error-ignored? [resource-result]
  (-> resource-result
      :details
      :metadata
      :ignore-errors
      (= "ignore")
      (not)))

(defn- not-error-ignoreds [resource-results]
  (filter not-error-ignored? resource-results))

(defn- extract-status [details]
  (cond (sequential? details) (map extract-status details)
        :else (conj [] (:status details))))

(defn- only-resource-status [resource-result]
  (->> resource-result
       (map :details)
       (map extract-status)
       (flatten)))

(defn- fix-status [statuses]
  (replace {nil 500 0 503 204 200} statuses))

(defn- get-max [a b]
  (cond (nil? a) b
        (nil? b) a
        (> a b) a
        :else b))

(defn- higher-value [statuses]
  (reduce get-max 200 statuses))

(defn calculate-response-status-code
  "Calculates the response status code of a given result"
  [result]
  (->> result
       (vals)
       (not-error-ignoreds)
       (only-resource-status)
       (fix-status)
       (higher-value)))
