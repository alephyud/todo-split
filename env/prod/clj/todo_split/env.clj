(ns todo-split.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[todo-split started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[todo-split has shut down successfully]=-"))
   :middleware identity})
