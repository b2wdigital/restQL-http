(ns restql.http.server.handler
  (:require
   [ring.middleware.params :as params]
   [compojure.core :as compojure :refer [GET POST OPTIONS]]
   [compojure.route :as route]
   [environ.core :refer [env]]
   [compojure.response :refer [Renderable]]
   [restql.http.query.handler :as query-handler]
   [restql.http.server.exception-handler :refer [wrap-exception-handling]]))

(extend-protocol Renderable
  clojure.lang.IDeref
  (render [d _] (manifold.deferred/->deferred d)))

(def default-value {:allow-adhoc-queries true})

(defn- get-default-value [env-var]
  (if (contains? env env-var) (get env env-var) (get default-value env-var)))

(defn- check-allow-adhoc []
  (if (true? (boolean (get-default-value :allow-adhoc-queries)))
    query-handler/adhoc
    {:status 405 :headers {"Content-Type" "application/json"} :body "{\"error\":\"FORBIDDEN_OPERATION\",\"message\":\"ad-hoc queries are turned off\"}"}))

(def adhoc-wrap (check-allow-adhoc))

(def handler
  (-> (compojure/routes
       (GET  "/health"                        [] "I'm healthy! :)")
       (GET  "/resource-status"               [] "Up and running! :)")
       (GET  "/run-query/:namespace/:id/:rev" [] query-handler/saved)
       (POST "/run-query"                     [] adhoc-wrap)
       (POST "/parse-query"                   [] query-handler/parse)
       (POST "/validate-query"                [] query-handler/validate)
       (OPTIONS "*"                           [] {:status 204})
       (route/not-found                       "There is nothing here. =/"))
      (wrap-exception-handling)
      (params/wrap-params)))