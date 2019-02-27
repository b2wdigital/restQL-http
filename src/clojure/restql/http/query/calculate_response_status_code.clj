(ns restql.http.query.calculate-response-status-code)

(defn- should-ignore-errors
  "Searches for an ignore-errors meta on a given item
   and returns it's value"
  [item]

  (-> item
      :details
      :metadata
      :ignore-errors
      (= "ignore")))

(defn- higher-value
  "Returns the max between a and b"
  [a b]

  (cond
    (nil? a) b
    (nil? b) a
    (> a b) a
    :else b))

(defn calculate-response-status-code
  "Calculates the response status code of a given result"
  [result]

  (->> result
       (vals)
       (flatten)
       (filter (complement should-ignore-errors))
       (map (comp :status :details))
       (replace {0 503 204 200})
       (reduce higher-value 200)))