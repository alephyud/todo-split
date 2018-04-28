(ns todo-split.db
  (:require [clojure.spec.alpha :as s]
            [todo-split.models.todos :as todos]))

(def default-db
  {::page :home
   ::todos []
   ::active-todo-index 0
   ::new-todo-id nil})

(s/def ::page keyword?)
(s/def ::todos ::todos/todolist)
(s/def ::new-todo-id (s/nilable ::todos/uuid))
(s/def ::active-todo-index nat-int?)

(s/def ::db
  (s/and (s/keys :req [::todos ::page ::active-todo-index]
                 :opt [::new-todo-id])
         #(<= (::active-todo-index %) (count (::todos %)))))
