(ns todo-split.db
  (:require [clojure.spec.alpha :as s]
            [todo-split.models.todos :as todos]))

(def default-db
  {::page :home
   ::todos []
   ::new-todo-id nil})

(s/def ::page keyword?)
(s/def ::todos ::todos/todolist)
(s/def ::new-todo-id (s/nilable ::todos/uuid))

(s/def ::db
  (s/keys :req [::todos ::page]
          :opt [::new-todo-id]))
