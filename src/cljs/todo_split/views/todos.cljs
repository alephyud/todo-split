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

(defn todo-widget [path {:keys [::todos/uuid] :as todo-item}]
  (let [!editable? (r/atom (= uuid @(rf/subscribe [:new-todo-id])))]
    (fn todo-widget-inner
      [path {:keys [::todos/uuid ::todos/text ::todos/subtasks] :as todo-item}]
      (let [active-path @(rf/subscribe [:active-todo-path])
            active? (= path active-path)]
        [:div
         [:div.list-group-item
          (merge
           {:style {:margin-left (* (count path) 20)
                    :background-color (when (nil? todo-item) "#fdd")}}
           (if active?
             {:class "todo-editable"}
             {:on-click #(rf/dispatch [:move-cursor-to-path path])}))
          (if active?
            [todo-input {:text text
                         :on-save #(do
                                     (println "on-save invoked")
                                     (when (not= (or text "") (or % ""))
                                       (rf/dispatch [:edit-todo-by-path path %])))
                         :on-stop #(do (reset! !editable? false)
                                       (rf/dispatch [:reset-new-todo-id]))}]
            (str text " "))]
         (when (or subtasks (= active-path (conj path 0)))
           (todolist path subtasks))]))))

(defn todolist [path items]
  (let [active-path @(rf/subscribe [:active-todo-path])
        num-items (count items)]
    [:div.list-group
     (for [[index todo-item] (map-indexed vector items)]
       ^{:key index} [todo-widget (conj path index) todo-item])
     (when (= active-path (conj path num-items))
       [todo-widget (conj path num-items) nil])]))

(defn todos-page []
  [:div.container.app-container
   [todolist [] @(rf/subscribe [:todos])]])
