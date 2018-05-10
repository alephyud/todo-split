(ns todo-split.db
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [todo-split.models.todos :as todos]))

(def default-db
  {::todos []
   ::initialized? false
   ::edit-mode? false
   ::active-todo-path [0]})

(s/def ::edit-mode? boolean?)
(s/def ::db-initialized? boolean?)
(s/def ::todos ::todos/todolist)
(s/def ::active-todo-path (s/coll-of nat-int? :min-count 1
                                     :gen #(gen/return [0])))

(defn valid-path? [todos [index & rest-path]]
  (and (<= index (count todos))
       (or (empty? rest-path)
           (valid-path? (get-in todos [index ::todos/subtasks 1]) rest-path))))

(s/def ::db
  (s/and (s/keys :req [::todos ::active-todo-path]
                 :opt [::edit-mode? ::initialized?])
         #(valid-path? (::todos %) (::active-todo-path %))))

;; Random task list generation.
;;
;; To prevent too long / too deeply nested sample lists, we limit ourselves
;; of lists of 5 to 7 items, each of which can with some probability
;; have up to 3 subtasks, with no nesting beyond that.

(s/def ::sample-todolist
  (s/coll-of ::todos/task :kind vector? :min-count 5 :max-count 7))

(defn generate-random-db []
  {::todos (mapv todos/set-uncompleted-expanded
                 (binding [s/*recursion-limit* 1]
                   (gen/generate (s/gen ::sample-todolist))))
   ::edit-mode? false
   ::initialized? true
   ::active-todo-path [0]})
