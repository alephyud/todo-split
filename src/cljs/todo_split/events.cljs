(ns todo-split.events
  (:require [todo-split.db :as db]
            [com.rpl.specter :as specter]
            [re-frame.core :as rf :refer [dispatch reg-sub]]
            [kee-frame.core :as kf :refer [reg-event-db reg-event-fx]]
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

(reg-event-db
 :add-todo
 (fn [db [_ todo]]
   (-> db
       (update ::db/todos conj todo)
       (assoc ::db/new-todo-id (::todos/uuid todo)))))

(reg-event-db
 :change-text
 (fn [{:keys [::db/todos]} [_ uuid text]]
   {::db/todos
    (specter/setval [(specter/filterer ::todos/uuid (specter/pred= uuid))
                     specter/FIRST ::todos/text] text todos)}))

(reg-event-db
 :reset-new-todo-id
 (fn [db _]
   (dissoc db ::db/new-todo-id)))

;;;; Subscriptions

(reg-sub :page ::db/page)

(reg-sub :docs :docs)

(reg-sub :todos ::db/todos)

(reg-sub :new-todo-id ::db/new-todo-id)
