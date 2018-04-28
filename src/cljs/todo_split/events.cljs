(ns todo-split.events
  (:require [todo-split.db :as db]
            [com.rpl.specter :as specter]
            [re-frame.core :as rf :refer [dispatch reg-sub]]
            [re-frame.std-interceptors :refer [path]]
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

(rf/reg-cofx
 :new-uuid
 (fn [coeffects _]
   (assoc coeffects :new-uuid (random-uuid))))

(reg-event-db
 :add-todo
 (fn [db [todo]]
   (-> db
       (update ::db/todos conj todo)
       (assoc ::db/new-todo-id (::todos/uuid todo)))))

(reg-event-db
 :change-text
 [(path [::db/todos])]
 (fn [todos [uuid text]]
   (specter/setval [(specter/filterer ::todos/uuid (specter/pred= uuid))
                    specter/FIRST ::todos/text] text todos)))

(reg-event-fx
 :edit-todo-by-position
 [(rf/inject-cofx :new-uuid) (path [::db/todos])]
 (fn [{todos :db new-uuid :new-uuid} [position text]]
   {:db (if (< position (count todos))
          (assoc-in todos [position ::todos/text] text)
          (assoc todos position {::todos/uuid new-uuid ::todos/text text}))}))

(reg-event-db
 :reset-new-todo-id
 (fn [db _]
   (dissoc db ::db/new-todo-id)))

(reg-event-db
 :move-cursor-to-index
 [(path ::db/active-todo-index)]
 (fn [_ [index]] index))

(reg-event-db
 :move-cursor-to-uuid
 (fn [{:keys [::db/todos] :as db} [target-uuid]]
   (let [result (ffirst (filter #(= target-uuid (::todos/uuid (second %)))
                                (map-indexed vector todos)))]
     (if (nil? result)
       (throw (str "To-do item with UUID " target-uuid " not found")))
     (assoc db ::db/active-todo-index result))))

(reg-event-db
 :move-cursor-down
 (fn [{:keys [::db/active-todo-index ::db/todos] :as db} _]
   (let [n (count todos)]
     (assoc db ::db/active-todo-index (min n (inc active-todo-index))))))

(reg-event-db
 :move-cursor-up
 (fn [{:keys [::db/active-todo-index ::db/todos] :as db} _]
   (assoc db ::db/active-todo-index (max 0 (dec active-todo-index)))))

;;;; Subscriptions

(reg-sub :page ::db/page)

(reg-sub :docs :docs)

(reg-sub :todos ::db/todos)

(reg-sub :todos-flat (comp todos/flat-repr ::db/todos))

(reg-sub :new-todo-id ::db/new-todo-id)

(reg-sub :active-todo-index ::db/active-todo-index)
