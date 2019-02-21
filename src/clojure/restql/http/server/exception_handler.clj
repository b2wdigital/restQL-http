(ns restql.http.server.exception-handler
  (:require [slingshot.slingshot :refer [try+]]
            [clojure.tools.logging :as log]
            [restql.http.request.util :as request-util]
            [ring.util.response :refer [response]]))

(defn wrap-exception-handling
  "Exception handling middleware for concise exception responses"
  [handler]

  (fn
    ([request]
      (try+
        (handler request)

        (catch [:type :revision-not-an-integer] e
          {:status 400 :body {:message "revision value should be an integer"}})

        (catch [:type :pdg-query-validation-error] e
          {:status 422 :body {:message (-> e :data :details)}})

        (catch [:type :query-or-revision-not-found] e
          {:status 404 :body {:message "could not find a query with the given name and revision"}})

        (catch [:type :internal-server-error-pdg] e
          {:status 422 :body {:message "restQL could not finish the query"}})

        (catch Exception e
          (log/error "Internal server error" e)
          (.printStackTrace e)
          {:status 503 :body {:message "Unexpected behavior happened."}})))
    ([request respond raise]
      (try+
        (handler request respond raise)

        (catch [:type :revision-not-an-integer] e
          (respond {:status 400
                    :body (request-util/format-response-body {:message "revision value should be an integer"})}))

        (catch [:type :pdg-query-validation-error] e
          (respond {:status 422
                    :body (request-util/format-response-body {:message (-> e :data :details)})}))

        (catch [:type :query-or-revision-not-found] e
          (respond {:status 404
                    :body (request-util/format-response-body {:message "could not find a query with the given name and revision"})}))

        (catch [:type :internal-server-error-pdg] e
          (respond {:status 422
                    :body (request-util/format-response-body {:message "restQL could not finish the query"})}))

        (catch Exception e
          (log/error "Internal server error" e)
          (.printStackTrace e)
          (respond {:status 503
                    :body (request-util/format-response-body {:message "Unexpected behavior happened."})}))))))
