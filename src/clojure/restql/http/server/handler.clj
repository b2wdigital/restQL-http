(ns restql.http.server.handler
  (:require
   [ring.middleware.params :as params]
   [compojure.core :as compojure :refer [GET POST OPTIONS]]
   [compojure.route :as route]
   [restql.config.core :as config]
   [restql.http.server.cors :as cors]
   [compojure.response :refer [Renderable]]
   [restql.http.query.handler :as query-handler]
   [restql.http.server.exception-handler :refer [wrap-exception-handling]]))

(extend-protocol Renderable
  clojure.lang.IDeref
  (render [d _] (manifold.deferred/->deferred d)))

(defn- get-adhoc-behaviour [req]
  (if (true? (boolean (config/get-config :allow-adhoc-queries)))
    query-handler/adhoc
    {:status 405 :headers {"Content-Type" "application/json"} :body "{\"error\":\"FORBIDDEN_OPERATION\",\"message\":\"ad-hoc queries are turned off\"}"}))

(defn options [req]
  {:status 204 :headers (cors/fetch-cors-headers)})

(def handler
  (-> (compojure/routes
       (GET  "/health"                        [] "I'm healthy! :)")
       (GET  "/resource-status"               [] "Up and running! :)")
       (GET  "/run-query/:namespace/:id/:rev" [] query-handler/saved)
       (POST "/run-query"                     [] get-adhoc-behaviour)
       (POST "/parse-query"                   [] query-handler/parse)
       (POST "/validate-query"                [] query-handler/validate)
       (OPTIONS "*"                           [] options)
       (route/not-found                       "There is nothing here. =/"))
      (wrap-exception-handling)
      (params/wrap-params)))