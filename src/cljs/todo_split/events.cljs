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

(rf/reg-cofx
 :random-db
 (fn [coeffects _]
   (assoc coeffects :random-db (db/generate-random-db))))

(reg-event-fx
 :generate-random-db
 [(rf/inject-cofx :random-db)]
 (fn [{:keys [random-db]} _] {:db random-db}))

;; Todo-related

(rf/reg-cofx
 :new-uuids
 (fn [coeffects _]
   (assoc coeffects :new-uuids (repeatedly 5 random-uuid))))

(reg-event-fx
 :edit-todo-by-path
 [(rf/inject-cofx :new-uuids) (path [::db/todos])]
 (fn-traced [cofx params]
   {:db (todos/edit-todo-by-path cofx params)}))

(reg-event-db
 :toggle-active-todo
 (fn [{:keys [::db/active-todo-path ::db/todos] :as db} [_]]
   (assoc db ::db/todos
          (todos/edit-todo-by-path {:db todos}
                                   [active-todo-path {:toggle-done true}]))))

(reg-event-db
 :cut-todos
 (fn [db params]
   (update db ::db/todos #(first (todos/cut-todos % params)))))

(reg-event-fx
 :cut-active-todo
 (fn [{{:keys [::db/active-todo-path]} :db} _]
   {:dispatch [:cut-todos active-todo-path]})) 

(reg-event-fx
 :split-todo
 [(rf/inject-cofx :new-uuids) (path [::db/todos])]
 (fn [{:keys [new-uuids] todos :db} [path]]
   {:db (todos/split-todo todos path new-uuids false)}))

(reg-event-fx
 :split-active-todo
 (fn [{{:keys [::db/active-todo-path]} :db} _]
   {:dispatch [:split-todo active-todo-path]})) 

(reg-event-db
 :reset-new-todo-id
 (fn [db _]
   (dissoc db ::db/new-todo-id)))

(reg-event-db
 :move-cursor-to-path
 [(path ::db/active-todo-path)]
 (fn-traced [_ [index]] index))

(reg-event-db
 :move-cursor-up
 (fn-traced [{:keys [::db/active-todo-path ::db/todos] :as db} _]
   (assoc db ::db/active-todo-path (todos/traverse-up todos active-todo-path))))

(reg-event-db
 :move-cursor-down
 (fn-traced [{:keys [::db/active-todo-path ::db/todos] :as db} _]
   (let [n (count todos)]
     (assoc db ::db/active-todo-path
            (todos/traverse-down todos active-todo-path false)))))

(reg-event-db
 :edit-mode-on
 [(path ::db/edit-mode?)]
 (fn [_ _] true))

(reg-event-db
 :edit-mode-off
 [(path ::db/edit-mode?)]
 (fn [_ _] false))

;;;; Subscriptions

(reg-sub :page ::db/page)

(reg-sub :docs :docs)

(reg-sub :todos ::db/todos)

(reg-sub :edit-mode? ::db/edit-mode?)

(reg-sub :todos-flat (comp todos/flat-repr ::db/todos))

(reg-sub :new-todo-id ::db/new-todo-id)

(reg-sub :active-todo-path ::db/active-todo-path)
