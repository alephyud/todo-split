(ns todo-split.events
  (:require [todo-split.db :as db]
            [clojure.string :as cs]
            [com.rpl.specter :as specter]
            [re-frame.core :as rf :refer [dispatch reg-sub]]
            [re-frame.std-interceptors :refer [path]]
            [kee-frame.core :as kf :refer [reg-event-db reg-event-fx]]
            [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
            [day8.re-frame.undo :as undo :refer [undoable]]
            [akiroz.re-frame.storage :as storage]
            [todo-split.models.todos :as todos]))

;;;; Dispatchers

;; General

(def local-storage-key
  (if js/window.todosplitTest
    :todosplit-test-0.1.0
    :todosplit-0.1.0))

(def persistence-on? true)

(def dummy-interceptor
  "Dummy interceptor for debugging purposes"
  (rf/->interceptor :id :dummy :before identity :after identity))

(defn save-db! [db]
  (when persistence-on?
    (storage/->store local-storage-key
                     (select-keys db [::db/todos ::db/active-todo-path]))))

(defn load-db [db]
  (cond-> db
    persistence-on? (merge (storage/<-store local-storage-key))))

(storage/register-store local-storage-key)
(def persist-db
  (rf/->interceptor
   :id local-storage-key
   :before (fn [context]
             ;; Retrieve state from local storage
             (update-in context [:coeffects :db] load-db))
   :after (fn [context]
            ;; Save state to local storage
            (save-db! (get-in context [:effects :db]))
            context)))

(defn compose-interceptors [interceptors]
  (rf/->interceptor
   :id (keyword (cs/join "+" (map :id interceptors)))
   :before (apply comp (keep :before interceptors))
   :after (apply comp (reverse (keep :after interceptors)))))

(rf/reg-cofx
 :new-uuids
 (fn [coeffects _]
   (assoc coeffects :new-uuids (repeatedly 5 random-uuid))))

(rf/reg-cofx
 :current-time
 (fn [coeffects _]
   (assoc coeffects :now (js/Date.))))

(def full-context
  (compose-interceptors [(undoable)
                         persist-db
                         (rf/inject-cofx :new-uuids)
                         (rf/inject-cofx :current-time)]))

(reg-event-fx
 :initialize-db
 [persist-db (rf/inject-cofx :current-time)]
 (fn [{:keys [db now]} _]
   {:db (merge
         db/default-db
         (cond-> db
           (nil? (::db/active-todo-path db)) (dissoc ::db/active-todo-path)
           ;; Safe upgrade from the version without timestamps
           (seq? (::db/todos db)) (update ::db/todos todos/attach-timestamps now)
           (nil? (::db/todos db)) (dissoc ::db/todos)
           (empty? (::db/todos db)) (assoc ::db/edit-mode? true))
         {::db/initialized? true})}))

(reg-event-db
 :set-active-page
 (fn [db [_ page]]
   (assoc db ::db/page page)))

(rf/reg-cofx
 :random-db
 (fn [coeffects _]
   (assoc coeffects :random-db (db/generate-random-db))))

(reg-event-fx
 :generate-random-db
 [(undoable) persist-db (rf/inject-cofx :random-db)]
 (fn [{:keys [random-db]} _] {:db random-db}))

(reg-event-fx
 :show-help
 (fn [_ _]
   {:navigate-to [:help]}))

(reg-event-fx
 :hide-help
 (fn [_ _]
   {:navigate-to [:home]}))

;; Todo-related

(reg-event-fx
 :edit-todo-by-path
 [full-context (path [::db/todos])]
 (fn-traced [cofx params]
   {:db (todos/edit-todo-by-path cofx params)}))

(reg-event-fx
 :toggle-active-todo
 ;; If the active task is leaf (no sub-tasks), toggles the "done" status.
 ;; Otherwise, expands or collapses it.
 [(undoable) persist-db (rf/inject-cofx :current-time)]
 (fn [{{:keys [::db/active-todo-path ::db/todos] :as db} :db now :now} [_]]
   {:pre [(inst? now)]}
   {:db (assoc db ::db/todos
               (todos/edit-todo-by-path
                {:db todos :now now}
                [active-todo-path {:toggle-status true}]))}))

(reg-event-db
 :expand-or-go-to-child
 [persist-db]
 (fn [{:keys [::db/active-todo-path ::db/todos] :as db} [_]]
   (let [active-todo (todos/get-by-path todos active-todo-path)]
     (cond
       ;; An item that has no subtasks cannot be expanded or collapsed
       ;; (though it may make sense to switch to editing mode)
       (empty? (::todos/subtasks active-todo)) db
       ;; If the item is collapsed, we expand it
       (::todos/collapsed? active-todo)
       (assoc db ::db/todos
              (todos/edit-todo-by-path
               {:db todos} [active-todo-path {:collapsed? false}]))
       ;; If it's already expanded, we go to its first child
       :else (update db ::db/active-todo-path conj 0)))))

(reg-event-db
 :collapse-or-go-to-parent
 [persist-db]
 (fn [{:keys [::db/active-todo-path ::db/todos] :as db} [_]]
   (let [active-todo (todos/get-by-path todos active-todo-path)]
     (cond
       ;; If the item has subtasks and is expanded, we collapse it
       (and (seq (::todos/subtasks active-todo))
            (not (::todos/collapsed? active-todo)))
       (assoc db ::db/todos
              (todos/edit-todo-by-path
               {:db todos} [active-todo-path {:collapsed? true}]))
       ;; If the item doesn't have subtask or is already collapsed,
       ;; we go to its parent (unless it's a top level item)
       (= 1 (count active-todo-path)) db
       :else (update db ::db/active-todo-path
                     subvec 0 (dec (count active-todo-path)))))))

(reg-event-db
 :cut-todos
 [persist-db (undoable)]
 (fn [{:keys [::db/todos] :as db} [path]]
   (let [[remaining removed last?] (todos/cut-todos todos path)
         ;; If the last item in a list was removed, move the cursor upwards
         new-path (if (and last? (not= [0] path))
                    (let [n (dec (count path))
                          path-last (get path n)]
                      (if (zero? path-last)
                        (subvec path 0 n)
                        (update path n dec)))
                    path)]
     (assoc db ::db/todos remaining ::db/active-todo-path new-path))))

(reg-event-fx
 :cut-active-todo
 (fn [{{:keys [::db/active-todo-path]} :db} _]
   {:dispatch [:cut-todos active-todo-path]})) 

(reg-event-fx
 :split-todo
 [full-context]
 (fn [{:keys [new-uuids now] {:keys [::db/todos] :as db} :db} [path inline?]]
   (when-let [new-todos (todos/split-todo todos path new-uuids now inline?)]
     {:db (merge db {::db/todos new-todos
                     ::db/active-todo-path (cond-> path (not inline?) (conj 0))
                     ::db/edit-mode? true})})))

(reg-event-fx
 :split-active-todo
 (fn-traced [{{:keys [::db/active-todo-path]} :db} [inline?]]
   {:dispatch [:split-todo active-todo-path inline?]})) 

(reg-event-db
 :move-cursor-to-path
 [persist-db (path ::db/active-todo-path)]
 (fn-traced [_ [index]] index))

(reg-event-db
 :move-cursor-up
 [persist-db]
 (fn-traced [{:keys [::db/active-todo-path ::db/todos] :as db} _]
   (assoc db ::db/active-todo-path
          (todos/traverse-up todos active-todo-path true))))

(reg-event-db
 :move-cursor-down
 [persist-db]
 (fn [{:keys [::db/active-todo-path ::db/todos] :as db} [append-depth]]
   (let [n (count todos)]
     (assoc db ::db/active-todo-path
            (todos/traverse-down todos active-todo-path append-depth true)))))

(reg-event-fx
 :insert-above
 [full-context]
 (fn-traced
   [{:keys [new-uuids now]
     {:keys [::db/todos ::db/active-todo-path] :as db} :db} _]
   (let [new-todos (todos/insert-at todos active-todo-path
                                    (first new-uuids) now)]
     {:db (merge db {::db/todos new-todos
                     ::db/edit-mode? true})})))

(reg-event-fx
 :insert-below
 [full-context]
 (fn-traced
   [{:keys [new-uuids now]
     {:keys [::db/todos ::db/active-todo-path] :as db} :db} _]
  (let [new-path (update active-todo-path (dec (count active-todo-path)) inc)
        new-todos (todos/insert-at todos new-path (first new-uuids) now)]
    {:db (merge db {::db/todos new-todos
                    ::db/active-todo-path new-path
                    ::db/edit-mode? true})})))

(reg-event-db
 :edit-mode-on
 [(path ::db/edit-mode?)]
 (fn [_ _] true))

(reg-event-db
 :edit-mode-off
 [(path ::db/edit-mode?)]
 (fn [_ _] false))

;;;; Subscriptions

(reg-sub :todos ::db/todos)

(reg-sub :db-initialized? ::db/initialized?)

(reg-sub :edit-mode? ::db/edit-mode?)

(reg-sub :todos-flat (comp todos/flat-repr ::db/todos))

(reg-sub :active-todo-path ::db/active-todo-path)

;;;; Undos and redos

(undo/undo-config!
 {:harvest-fn (fn [!db] (select-keys @!db [::db/todos ::db/active-todo-path]))
  :reinstate-fn (fn [!db value]
                  (save-db! value)
                  (swap! !db merge value))})
