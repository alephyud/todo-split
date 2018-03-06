(ns user
  (:require 
            [mount.core :as mount]
            [todo-split.figwheel :refer [start-fw stop-fw cljs]]
            [todo-split.core :refer [start-app]]))

(defn start []
  (mount/start-without #'todo-split.core/repl-server))

(defn stop []
  (mount/stop-except #'todo-split.core/repl-server))

(defn restart []
  (stop)
  (start))


