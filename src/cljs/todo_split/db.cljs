(ns todo-split.db
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [todo-split.models.todos :as todos]))

(def default-db
  {::page :home
   ::todos []
   ::edit-mode? false
   ::active-todo-path [0]
   ::new-todo-id nil})

(s/def ::page keyword?)
(s/def ::edit-mode? boolean?)
(s/def ::todos ::todos/todolist)
(s/def ::new-todo-id (s/nilable ::todos/uuid))
(s/def ::active-todo-path (s/coll-of nat-int? :gen #(gen/return [0])))

(defn valid-path? [todos [index & rest-path]]
  (and (<= index (count todos))
       (or (empty? rest-path)
           (valid-path? (get-in todos [index ::todos/subtasks]) rest-path))))

(s/def ::db
  (s/and (s/keys :req [::todos ::page ::active-todo-path ::edit-mode?]
                 :opt [::new-todo-id])
         #(valid-path? (::todos %) (::active-todo-path %))))

(defn generate-random-db []
  (gen/generate (s/gen ::db)))
