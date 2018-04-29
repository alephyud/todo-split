(ns todo-split.models.todos
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [todo-split.models.todos.gen :as todos.gen]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]))

(s/def ::uuid (s/spec #(partial instance? UUID) :gen gen/uuid))
(s/def ::text (s/spec string? :gen (fn [] todos.gen/task)))

(s/def ::task
  (s/keys :req [::uuid ::text]
          :opt [::subtasks]))

(s/def ::indent nat-int?)

(s/def ::flat-task
  (s/keys :req [::uuid ::text ::indent]))

(s/def ::todolist (s/coll-of ::task :gen-max 10))
(s/def ::subtasks (s/coll-of ::task :gen-max 0))

(defn-traced flat-repr
  ([todolist] (flat-repr todolist 0))
  ([todolist base-indent]
   (reduce
    (fn [acc {:keys [::uuid ::text ::subtasks]}]
      (into (conj acc {::uuid uuid ::text text ::indent base-indent})
            (mapcat #(flat-repr [%] (inc base-indent)) subtasks)))
    [] todolist)))

(defn-traced get-by-path [todos path]
  (get-in todos (interpose ::subtasks path)))

(defn-traced last-child-path [{:keys [::subtasks]} path]
  (if (empty? subtasks)
    path
    (last-child-path (peek subtasks) (conj path (dec (count subtasks))))))

(defn-traced traverse-up [todos path]
  (cond
    (= path [0]) path
    (= (peek path) 0) (subvec path 0 (dec (count path)))
    :else (let [path-len (count path)
                dec-path (update path (dec path-len) dec)]
            (last-child-path (get-by-path todos dec-path) dec-path))))

(defn-traced traverse-down [todos path create-new?]
  (let [new-item-adjustment (if create-new? 1 0)
        [subtask-counts inner-subtasks]
        (reduce (fn [[acc todos] index]
                  [(conj acc (+ (count todos) new-item-adjustment))
                   (get-in todos [index ::subtasks])])
                [[] todos] path)]
    (if (or (pos? (count inner-subtasks))
            (and create-new? (< (peek path) (dec (peek subtask-counts)))))
      (conj path 0)
      (loop [depth (dec (count path))
             new-path path]
        (cond
          (neg? depth) path
          (< (inc (get new-path depth))
             (get subtask-counts depth)) (update new-path depth inc)
          :else (recur (dec depth) (subvec new-path 0 depth)))))))
