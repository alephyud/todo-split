(ns todo-split.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [todo-split.layout :refer [error-page]]
            [todo-split.routes.home :refer [home-routes test-routes]]
            [todo-split.routes.services :refer [service-routes]]
            [todo-split.routes.oauth :refer [oauth-routes]]
            [compojure.route :as route]
            [todo-split.env :refer [defaults]]
            [mount.core :as mount]
            [todo-split.middleware :as middleware]))

(mount/defstate init-app
  :start ((or (:init defaults) identity))
  :stop  ((or (:stop defaults) identity)))

(mount/defstate app
  :start
  (middleware/wrap-base
   (routes
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    #'test-routes
    #'oauth-routes
    #'service-routes
    (route/not-found
     (:body
      (error-page {:status 404
                   :title "page not found"}))))))
