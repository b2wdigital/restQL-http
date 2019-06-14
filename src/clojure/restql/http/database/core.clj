(ns restql.http.database.core
  (:require [restql.http.database.persistence :as db]
            [clojure.edn :as edn]
            [restql.core.validator.core :as validator]
            [slingshot.slingshot :refer [throw+]]))

;re-exporting find-query
(def find-query-by-id-and-revision db/find-query)
(def count-query-revisions db/count-query-revisions)
(def list-namespaces db/list-namespaces)
(def find-all-queries-by-namespace db/find-all-queries-by-namespace)

;re-exporting tenant methods
(def find-tenants db/find-tenants)
(def find-tenant-by-id db/find-tenant-by-id)
