(ns todo-split.models.todos
  (:require [clojure.spec.alpha :as s]))

(s/def ::uuid string?)
(s/def ::text string?)

(s/def ::task
  (s/keys :req [::uuid ::text]
          :opt [::subtasks]))

(s/def ::todolist (s/* ::task))

(s/valid? ::task {:uuid "gfy" :text "Clean teeth"})

(s/valid? ::text "clean teeth")
