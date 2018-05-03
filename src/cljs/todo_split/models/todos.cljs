(ns todo-split.models.todos
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as cs]
            [todo-split.models.todos.gen :as todos.gen]))

(s/def ::uuid (s/spec #(partial instance? UUID) :gen gen/uuid))
(s/def ::text (s/spec string? :gen todos.gen/task))
(s/def ::done? boolean?)

(s/def ::task
  (s/keys :req [::uuid ::text]
          :opt [::subtasks ::done?]))

(s/def ::indent nat-int?)

(s/def ::flat-task
  (s/keys :req [::uuid ::text ::indent]))

(s/def ::todolist (s/coll-of ::task :gen-max 10))
(s/def ::subtasks (s/coll-of ::task :gen-max 0))

(defn flat-repr
  ([todolist] (flat-repr todolist 0))
  ([todolist base-indent]
   (reduce
    (fn [acc {:keys [::uuid ::text ::subtasks]}]
      (into (conj acc {::uuid uuid ::text text ::indent base-indent})
            (mapcat #(flat-repr [%] (inc base-indent)) subtasks)))
    [] todolist)))

(defn get-by-path [todos path]
  (get-in todos (interpose ::subtasks path)))

(defn last-child-path [{:keys [::subtasks]} path]
  (if (empty? subtasks)
    path
    (last-child-path (peek subtasks) (conj path (dec (count subtasks))))))

(defn traverse-up [todos path]
  (cond
    (= path [0]) path
    (= (peek path) 0) (subvec path 0 (dec (count path)))
    :else (let [path-len (count path)
                dec-path (update path (dec path-len) dec)]
            (last-child-path (get-by-path todos dec-path) dec-path))))

(defn traverse-down [todos path create-new?]
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

(defn edit-todo-by-path
  "Replaces the text using the selected index path"
  [{todos :db :keys [new-uuids] :as cofx}
   [[index & rest-path] {:keys [text done? toggle-done] :as params}]]
  (if (empty? rest-path)
    (update (or todos []) index
            #(merge {::uuid (first new-uuids)} %
                    (when text {::text text})
                    (when (some? done?) {::done? done?})
                    (when toggle-done {::done? (not (::done? %))})))
    (assoc-in todos [index ::subtasks]
              (edit-todo-by-path (update cofx :db get-in [index ::subtasks])
                                 [rest-path params]))))

(defn cut-todos
  "Takes a todo list and a path of indices (which may end in a range of two
   indices or a single number). Cuts out the todos designated by the path and
   returns a vector of:
    - the todo list from which the selected todos are cut, and
    - the todos that were cut out."
  [todos [[index & rest-path]]]
  (if (empty? rest-path)
    (let [[start end] (if (seq? index) index [index index])]
      [(into (subvec todos 0 start) (subvec todos (inc end)))
       (subvec todos start (inc end))])
    (let [key-path [index ::subtasks]
          [remaining removed] (cut-todos (get-in todos key-path) [rest-path])]
      [(assoc-in todos key-path remaining) removed])))

(defn splittable? [{:keys [::subtasks ::text ::done?]}]
  (and (not (cs/blank? text)) (empty? subtasks) (not done?)))

(defn first-step-text [text]
  (str "First step to " (cs/lower-case (first text)) (subs text 1)))

(defn split-subtasks [{:keys [::text] :as todo} uuids]
  [{::uuid (first uuids)
    ::text (first-step-text text)}
   {::uuid (second uuids)
    ::text "..."}])

(defn split-hierarchical [todo uuids]
  (cond-> todo
    (splittable? todo)
    (assoc ::subtasks (split-subtasks todo uuids))))

(defn split-inline [todos index uuids]
  (let [todo (get todos index)]
    (if (splittable? todo)
      (-> (subvec todos 0 index)
          (into (split-subtasks todo uuids))
          (into (subvec todos (inc index))))
      todos)))

(defn split-todo [todos path uuids inline?]
  (if inline?
    (if (= 1 (count path))
      (split-inline todos (first path) uuids)
      (let [key-path (mapcat #(list % ::subtasks) (butlast path))]
        (update-in todos key-path split-inline (peek path) uuids)))
    (update-in todos (interpose ::subtasks path) split-hierarchical uuids)))
