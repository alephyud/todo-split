(ns todo-split.models.todos
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def ::uuid (s/spec #(partial instance? UUID) :gen gen/uuid))
(s/def ::text string?)

(s/def ::task
  (s/keys :req [::uuid ::text]
          :opt [::subtasks]))

(s/def ::indent nat-int?)

(s/def ::flat-task
  (s/keys :req [::uuid ::text ::indent]))

(s/def ::todolist (s/coll-of ::task :gen-max 5))
(s/def ::subtasks ::todolist)

(defn flat-repr
  ([todolist] (flat-repr todolist 0))
  ([todolist base-indent]
   (reduce
    (fn [acc {:keys [::uuid ::text ::subtasks]}]
      (into (conj acc {::uuid uuid ::text text ::indent base-indent})
            (mapcat #(flat-repr [%] (inc base-indent)) subtasks)))
    [] todolist)))
