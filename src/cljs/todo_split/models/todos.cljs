(ns todo-split.models.todos
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as cs]
            [todo-split.models.todos.gen :as todos.gen]))

(s/def ::uuid (s/spec #(partial instance? UUID) :gen gen/uuid))
(s/def ::text (s/spec string? :gen todos.gen/task))
(s/def ::done? (s/spec boolean?))
(s/def ::collapsed? (s/spec boolean?))

(s/def ::task
  (s/keys :req [::uuid ::text]
          :opt [::subtasks ::done? ::collapsed?]))

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

(defn last-child-path [{:keys [::subtasks ::collapsed?]} path skip-collapsed?]
  (if (or (empty? subtasks) (and skip-collapsed? collapsed?))
    path
    (last-child-path (peek subtasks) (conj path (dec (count subtasks))))))

(defn traverse-up [todos path skip-collapsed?]
  (cond
    (= path [0]) path
    (= (peek path) 0) (subvec path 0 (dec (count path)))
    :else (let [path-len (count path)
                dec-path (update path (dec path-len) dec)]
            (last-child-path (get-by-path todos dec-path) dec-path
                             skip-collapsed?))))

(defn traverse-down [todos path create-new? skip-collapsed?]
  (let [new-item-adjustment (if create-new? 1 0)
        [subtask-counts inner-subtasks inner-collapsed?]
        (reduce (fn [[acc todos] index]
                  (let [{:keys [::subtasks ::collapsed?]} (get todos index)]
                    [(conj acc (+ (count todos) new-item-adjustment))
                     subtasks collapsed?]))
                [[] todos] path)]
    (if (or (and (pos? (count inner-subtasks))
                 (not (and inner-collapsed? skip-collapsed?)))
            (and create-new? (< (peek path) (dec (peek subtask-counts)))))
      (conj path 0)
      (loop [depth (dec (count path))
             new-path path]
        (cond
          (neg? depth) path
          (< (inc (get new-path depth))
             (get subtask-counts depth)) (update new-path depth inc)
          :else (recur (dec depth) (subvec new-path 0 depth)))))))

(defn insert-at [todos [index & rest-path] uuid]
  (if (seq rest-path)
    (update-in todos [index ::subtasks] insert-at rest-path uuid)
    (-> (subvec todos 0 index)
        (conj {::text "" ::uuid uuid})
        (into (subvec todos index)))))

(defn edit-todo-by-path
  "Replaces the text using the selected index path"
  [{todos :db :keys [new-uuids] :as cofx}
   [[index & rest-path] {:keys [text done? toggle-done collapsed?] :as params}]]
  (if (empty? rest-path)
    (update (or todos []) index
            #(merge {::uuid (first new-uuids)} %
                    (when text {::text text})
                    (when (some? done?) {::done? done?})
                    (when toggle-done {::done? (not (::done? %))})
                    (when (some? collapsed?) {::collapsed? collapsed?})))
    (assoc-in todos [index ::subtasks]
              (edit-todo-by-path (update cofx :db get-in [index ::subtasks])
                                 [rest-path params]))))

(defn cut-todos
  "Takes a todo list and a path of indices (which may end in a range of two
   indices or a single number). Cuts out the todos designated by the path and
   returns a vector of:
    - the todo list from which the selected todos are cut,
    - the todos that were cut out, and
    - whether the items that were cut were the last among their siblings."
  [todos [index & rest-path]]
  (if (empty? rest-path)
    (let [[start end] (if (seq? index) index [index index])]
      [(into (subvec todos 0 start) (subvec todos (inc end)))
       (subvec todos start (inc end))
       (>= end (dec (count todos)))])
    (let [key-path [index ::subtasks]
          [remaining removed last?] (cut-todos (get-in todos key-path)
                                               rest-path)]
      [(assoc-in todos key-path remaining) removed last?])))

(defn splittable? [{:keys [::subtasks ::text ::done?]}]
  (and (not (cs/blank? text)) (empty? subtasks) (not done?)))

(defn first-step-text [text]
  (str "First step to " (cs/lower-case (first text)) (subs text 1)))

(defn split-subtasks [{:keys [::text] :as todo} uuids]
  (when (splittable? todo)
    [{::uuid (first uuids)
      ::text (first-step-text text)}
     {::uuid (second uuids)
      ::text "..."}]))

(defn split-inline [todos index subtasks]
  (-> (subvec todos 0 index)
      (into subtasks)
      (into (subvec todos (inc index)))))

(defn split-todo
  "Split the todo in `todos` designated by the `path` in two tasks, if it
  is splittable (i. e. has text, doesn't have subtasks and is not done). If
  `inline?` is true, the splitting is done inline; otherwise, two subtasks are
  created.

  If the task is splittable, returns the resulting todos structure. Otherwise,
  returns nil."
  [todos path uuids inline?]
  (let [key-path (interpose ::subtasks path)
        todo (get-in todos key-path)]
    (when-let [subtasks (split-subtasks todo uuids)]
      (if inline?
        (if-let [key-path (butlast key-path)]
          (update-in todos key-path split-inline (peek path) subtasks)
          (split-inline todos (peek path) subtasks))
        (update-in todos key-path assoc ::subtasks subtasks)))))

(defn set-undone [todo]
  (assoc todo
         ::done? false
         ::subtasks (mapv set-undone (::subtasks todo))))

(defn done-status [{:keys [::subtasks ::done?] :as todo}]
  (if (empty? subtasks)
    [(if done? 1 0) 1]
    (reduce (partial map +) (map done-status subtasks))))
