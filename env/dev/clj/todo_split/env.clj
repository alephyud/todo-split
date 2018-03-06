(ns todo-split.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [todo-split.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[todo-split started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[todo-split has shut down successfully]=-"))
   :middleware wrap-dev})
