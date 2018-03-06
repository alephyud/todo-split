(ns todo-split.app
  (:require [todo-split.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
