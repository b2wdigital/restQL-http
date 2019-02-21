(ns restql.http.server.handler
  (:require
   [ring.middleware.params :as params]
   [compojure.core :as compojure :refer [GET POST OPTIONS]]
   [compojure.route :as route]
   [compojure.response :refer [Renderable]]
   [restql.http.query.handler :as query-handler]
   [restql.http.server.exception-handler :refer [wrap-exception-handling]]))

(extend-protocol Renderable
  clojure.lang.IDeref
  (render [d _] (manifold.deferred/->deferred d)))

(def handler
  (-> (compojure/routes
       (GET  "/health"                        [] "I', healthy! :)")
       (GET  "/resource-status"               [] "Up and running! :)")
       (GET  "/run-query/:namespace/:id/:rev" [] query-handler/saved)
       (POST "/run-query"                     [] query-handler/adhoc)
       (POST "/parse-query"                   [] query-handler/parse)
       (POST "/validate-query"                [] query-handler/validate)
       (OPTIONS "*"                           [] {:status 204})
       (route/not-found                       "There is nothing here. =/"))
      (wrap-exception-handling)
      (params/wrap-params)))