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

(reg-event-fx
 :edit-todo-by-path
 [(rf/inject-cofx :new-uuid) (path [::db/todos])]
 (fn-traced edit-todo-by-path [{todos :db new-uuid :new-uuid} [path text]]
   {:db (update-in todos (interpose ::todos/subtasks path)
                   #(merge {::todos/uuid new-uuid} % {::todos/text text}))}))

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
 (fn [{:keys [::db/active-todo-path ::db/todos] :as db} _]
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
