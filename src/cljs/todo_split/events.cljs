(ns todo-split.events
  (:require [todo-split.db :as db]
            [com.rpl.specter :as specter]
            [re-frame.core :as rf :refer [dispatch reg-sub]]
            [re-frame.std-interceptors :refer [path]]
            [kee-frame.core :as kf :refer [reg-event-db reg-event-fx]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [todo-split.models.todos :as todos]))

;;;; Dispatchers

;; General

(reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(reg-event-db
 :set-active-page
 (fn [db [_ page]]
   (assoc db ::db/page page)))

(reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

;; Todo-related

(rf/reg-cofx
 :new-uuid
 (fn [coeffects _]
   (assoc coeffects :new-uuid (random-uuid))))

(defn-traced edit-todo-by-path
  "Replaces the text using the selected index path"
  [{todos :db :keys [new-uuid] :as cofx} [[index & rest-path] text]]
  (if (empty? rest-path)
    (update (or todos []) index
            #(merge {::todos/uuid new-uuid} % {::todos/text text}))
    (assoc-in todos [index ::todos/subtasks]
              (edit-todo-by-path
               (update cofx :db get-in [index ::todos/subtasks])
               [rest-path text]))))

(defn-traced cut-todos
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
    (let [key-path [index ::todos/subtasks]
          [remaining removed] (cut-todos (get-in todos key-path) [rest-path])]
      [(assoc-in todos key-path remaining) removed])))

(reg-event-fx
 :edit-todo-by-path
 [(rf/inject-cofx :new-uuid) (path [::db/todos])]
 (fn-traced [cofx params]
   {:db (edit-todo-by-path cofx params)}))

(reg-event-db
 :cut-todos
 (fn [db params]
   (update db ::db/todos #(first (cut-todos % params)))))

(reg-event-fx
 :cut-active-todo
 (fn [{{:keys [::db/active-todo-path]} :db} _]
   {:dispatch [:cut-todos active-todo-path]}))

(reg-event-db
 :reset-new-todo-id
 (fn [db _]
   (dissoc db ::db/new-todo-id)))

(reg-event-db
 :move-cursor-to-path
 [(path ::db/active-todo-path)]
 (fn-traced [_ [index]] index))

(reg-event-db
 ;; TO BE REWRITTEN
 :move-cursor-to-uuid
 (fn-traced [{:keys [::db/todos] :as db} [target-uuid]]
   (let [result (ffirst (filter #(= target-uuid (::todos/uuid (second %)))
                                (map-indexed vector todos)))]
     (if (nil? result)
       (throw (str "To-do item with UUID " target-uuid " not found")))
     (assoc db ::db/active-todo-index result))))

(reg-event-db
 :move-cursor-up
 (fn-traced [{:keys [::db/active-todo-path ::db/todos] :as db} _]
   (assoc db ::db/active-todo-path (todos/traverse-up todos active-todo-path))))

(reg-event-db
 :move-cursor-down
 (fn-traced [{:keys [::db/active-todo-path ::db/todos] :as db} _]
   (let [n (count todos)]
     (assoc db ::db/active-todo-path
            (todos/traverse-down todos active-todo-path true)))))

;;;; Subscriptions

(reg-sub :page ::db/page)

(reg-sub :docs :docs)

(reg-sub :todos ::db/todos)

(reg-sub :todos-flat (comp todos/flat-repr ::db/todos))

(reg-sub :new-todo-id ::db/new-todo-id)

(reg-sub :active-todo-path ::db/active-todo-path)
