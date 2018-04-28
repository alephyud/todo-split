(ns todo-split.views.todos
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as cs]
            [goog.events :as events]
            [todo-split.models.todos :as todos]
            [todo-split.db :as db])
  (:import [goog.events KeyCodes]))

(defn todo-input [{:keys [text on-save on-stop]}]
  (let [!val (r/atom text)
        stop #(do (reset! !val text)
                  (when on-stop (on-stop)))
        save #(let [v (-> @!val str cs/trim)]
                (on-save v))
        key-handler #(condp = (.-which %)
                       KeyCodes.ENTER (do (save) (rf/dispatch [:move-cursor-down]))
                       KeyCodes.ESC (stop)
                       KeyCodes.UP (do (save) (rf/dispatch [:move-cursor-up]))
                       KeyCodes.DOWN (do (save) (rf/dispatch [:move-cursor-down]))
                       nil)]
    (r/create-class
     {:component-did-mount                                               
      #(let [input (r/dom-node %)]                                       
         (doto input                                                      
           (.focus)                                                       
           (.setSelectionRange 0 (.. input -value -length))))
      :reagent-render
      (fn [props]
        [:input.form-group
         (merge (dissoc props :on-save :on-stop :text)
                {:type        "text"
                 :value       @!val
                 :on-blur     save
                 :on-change   #(reset! !val (-> % .-target .-value))
                 :on-key-down key-handler})])})))

(declare todolist)

(defn todo-widget [index {:keys [::todos/uuid] :as todo-item}]
  (let [!editable? (r/atom (= uuid @(rf/subscribe [:new-todo-id])))]
    (fn [index {:keys [::todos/uuid ::todos/text ::todos/subtasks]}]
      (let [active? (= index @(rf/subscribe [:active-todo-index]))]
        [:li.list-group-item
         (if active?
           {:class "todo-editable"}
           {:on-click #(rf/dispatch [:move-cursor-to-index index])})
         (if active?
           [todo-input {:text text
                        :on-save #(when (not= (or text "") (or % ""))
                                    (rf/dispatch
                                     (if uuid
                                       [:change-text uuid %]
                                       [:add-todo {::todos/uuid (random-uuid)
                                                   ::todos/text %}])))
                        :on-stop #(do (reset! !editable? false)
                                      (rf/dispatch [:reset-new-todo-id]))}]
           (str text " "))
         (when (seq subtasks)
           [todolist (r/atom subtasks) (r/atom -1)])]))))

(defn todolist [!items !active-index]
  (let [num-items (count @!items)]
    [:ul.list-group
     (for [[index todo-item] (map-indexed vector @!items)]
       ^{:key index} [todo-widget index todo-item])
     (when (>= @!active-index num-items)
       ^{:key num-items} [todo-widget num-items nil])]))

(defn todos-page []
  [:div.container.app-container
   [todolist (rf/subscribe [:todos]) (rf/subscribe [:active-todo-index])]])
