(ns restql.http.database.persistence
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer [$inc $push $slice $group $set]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+]]))

(defonce conn-data (atom nil))

(defn connect!
  "Connects to the MongoDB"
  [uri]
  (if (nil? uri)
    (log/info "Skipping connection with MongoDB: db url is not set")
    (when (nil? @conn-data)
      (do (log/info "Connecting to MongoDB:" uri)
          (reset! conn-data (mg/connect-via-uri uri))))))

(defn disconnect!
  "Disconnects from the MongoDB"
  []

  (when-not (nil? @conn-data)
    (mg/disconnect (:conn @conn-data))
    (reset! conn-data nil)))

(defn check-conn!
  "Checks the MongoDB connection"
  []

  (when (nil? (:db @conn-data))
    (throw+ {:type :db-not-connected})))

(defmacro defquery [qname args & body]
  (let [[fargs _ [dbarg]] (->> args (partition-by #{:with}))]
    `(defn ~qname ~(into [] fargs)
       (let [~dbarg (:db @conn-data)]
       (check-conn!)
       ~@body))))

(defquery find-tenant-by-id [id :with db]
          (mc/find-one-as-map db "tenant" {:_id id}))

(defquery find-tenants [{} :with db]
          (->> (mc/find-maps db "tenant" {}) (map :_id)))


(defquery list-namespaces [{} :with db]
  (let [namespaces (mc/aggregate db "query" [{$group {:_id "$namespace"}}])]
    namespaces))

(defquery save-resource [tenant resource-name resource-url :with db]
  (let [mapping-str (str "mappings." resource-name)
        mapping-key (keyword mapping-str)]
    (mc/find-and-modify db "tenant"
                {:_id tenant}
                {$set {mapping-key resource-url}}
                {:upsert false :return-new true})))

(defquery find-query [query-ns id revision :with db]
  (let [res (mc/find-one-as-map db "query" {:name id :namespace query-ns}
                                     {:_id 0 :size 0 :revisions {$slice [(dec revision) 1]}})
        text (-> res :revisions first)]
    text))

(defquery count-query-revisions [query-ns id :with db]
  (let [res (mc/find-one-as-map db "query" {:name id :namespace query-ns} {:name 0 :revisions 0})]
    (if (nil? res)
      0
      (:size res))))

(defquery find-all-queries-by-namespace [query-ns :with db]
  (let [res (mc/find-maps db "query" {:namespace query-ns})]
    (map
      (fn [q]
        {:id (:name q)
         :size (:size q)} )
      res)))