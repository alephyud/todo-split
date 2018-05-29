(ns todo-split.routes.home
  (:require [todo-split.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defroutes test-routes
  (GET "/test" [] (layout/render "test.html")))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/help" [] (home-page)))

