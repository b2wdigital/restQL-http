(ns restql.http.server.handler
  (:require
   [ring.middleware.params :as params]
   [compojure.core :as compojure :refer [GET ANY POST OPTIONS]]
   [compojure.route :as route]
   [environ.core :refer [env]]
   [restql.http.server.cors :as cors]
   [compojure.response :refer [Renderable]]
   [restql.http.query.handler :as query-handler]
   [restql.http.server.exception-handler :refer [wrap-exception-handling]]))

(extend-protocol Renderable
  clojure.lang.IDeref
  (render [d _] (manifold.deferred/->deferred d)))

(def default-value {:allow-adhoc-queries true})

(defn- get-default-value [env-var]
  (if (contains? env env-var) (read-string (env env-var)) (get default-value env-var)))

(defn- get-adhoc-behaviour [req]
  (if (true? (boolean (get-default-value :allow-adhoc-queries)))
    query-handler/adhoc
    {:status 405 :headers {"Content-Type" "application/json"} :body "{\"error\":\"FORBIDDEN_OPERATION\",\"message\":\"ad-hoc queries are turned off\"}"}))

(defn options [req]
  {:status 204 :headers (cors/fetch-cors-headers)})

(def handler
  (-> (compojure/routes
       (GET     "/health"                        [] "I'm healthy! :)")
       (GET     "/resource-status"               [] "Up and running! :)")
       (ANY     "/run-query/:namespace/:id/:rev" [] query-handler/saved)
       (ANY     "/run-query"                     [] get-adhoc-behaviour)
       (POST    "/parse-query"                   [] query-handler/parse)
       (POST    "/validate-query"                [] query-handler/validate)
       (OPTIONS "*"                              [] options)
       (route/not-found                          "There is nothing here. =/"))
      (wrap-exception-handling)
      (params/wrap-params)))