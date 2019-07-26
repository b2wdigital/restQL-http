(ns restql.http.query.calculate-response-status-code)

(defn- get-max [a b]
  (cond (nil? a) b
        (nil? b) a
        (> a b) a
        :else b))

(defn- higher-value [statuses]
  (reduce get-max 200 statuses))

(defn- fix-status [statuses]
  (replace {nil 500 0 503 204 200} statuses))

(defn- not-error-ignored? [details]
  (-> details
      :metadata
      :ignore-errors
      (= "ignore")
      (not)))

(defn calculate-response-status-code
  "Calculates the response status code of a given result"
  [result]
  (->>
   result
   (vals)
   (map :details)
   (flatten)
   (filter not-error-ignored?)
   (map :status)
   (fix-status)
   (higher-value)))