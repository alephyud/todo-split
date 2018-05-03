(ns todo-split.views.todos
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as cs]
            [goog.events :as events]
            [todo-split.models.todos :as todos]
            [todo-split.db :as db])
  (:import [goog.events KeyCodes]))

(defn select-all [input-element]
  (.setSelectionRange input-element 0 (.. input-element -value -length)))

(defn component-with-handlers
  "A component that watches non-local events (on document), e. g. keyboard
  events. Handlers are placed when the component is mounted and removed when
  the component is unmounted."
  [handlers render-fn]
  (r/create-class
   {:component-did-mount
    #(doseq [[event handler] handlers]
       (.addEventListener js/document.body (name event) handler))
    :component-will-unmount
    #(doseq [[event handler] handlers]
       (.removeEventListener js/document.body (name event) handler))
    :reagent-render
    render-fn}))

(defn non-edit-mode-key-handler [event]
  (when-not @(rf/subscribe [:edit-mode?])
    (condp = (.-which event)
      KeyCodes.G (rf/dispatch [:generate-random-db])
      KeyCodes.S (rf/dispatch [:split-active-todo])
      KeyCodes.SPACE (rf/dispatch [:toggle-active-todo])
      KeyCodes.DELETE (rf/dispatch [:cut-active-todo])
      KeyCodes.X (rf/dispatch [:cut-active-todo])
      KeyCodes.U (rf/dispatch [:undo])
      KeyCodes.R (rf/dispatch [:redo])
      KeyCodes.ENTER (rf/dispatch (if (.-shiftKey event)
                                    [:insert-below] [:edit-mode-on]))
      KeyCodes.K (rf/dispatch [:move-cursor-up])
      KeyCodes.UP (rf/dispatch [:move-cursor-up])
      KeyCodes.J (rf/dispatch [:move-cursor-down])
      KeyCodes.DOWN (rf/dispatch [:move-cursor-down])
      nil)))

(defn edit-mode-key-handler [{:keys [save stop]} event]
  (if (and (.-ctrlKey event) (.-shiftKey event))
    (condp = (.-which event)
      KeyCodes.BACKSPACE (do (rf/dispatch [:cut-active-todo])
                             (.preventDefault event))
      nil)
    (condp = (.-which event)
      KeyCodes.ENTER (do (save)
                         (rf/dispatch (if (.-shiftKey event)
                                        [:insert-below] [:move-cursor-down])))
      KeyCodes.ESC (stop)
      KeyCodes.UP (do (save) (rf/dispatch [:move-cursor-up]))
      KeyCodes.DOWN (do (save) (rf/dispatch [:move-cursor-down]))
      nil)))

(defn todo-input [{:keys [text on-save on-stop]}]
  (let [!val (r/atom text)
        !external-update? (r/atom false)
        stop #(do (reset! !val text) (when on-stop (on-stop)))
        save #(let [v (-> @!val str cs/trim)] (on-save v))
        key-handler (partial edit-mode-key-handler {:stop stop :save save})]
    (r/create-class
     {:component-did-mount                                               
      #(let [input (r/dom-node %)]                                       
         (.focus input)
         (select-all input))
      :component-will-receive-props
      (fn [this [_ {:keys [text]}]]
        (when-not (= text @!val)
          (reset! !val text)
          (reset! !external-update? true)))
      :component-did-update                                               
      #(let [input (r/dom-node %)]
         (when @!external-update?
           (select-all input)
           (reset! !external-update? false)))
      :reagent-render
      (fn [props]
        [:input.form-control
         (merge (dissoc props :on-save :on-stop :text)
                {:type        "text"
                 :value       @!val
                 :on-blur     save
                 :on-change   #(reset! !val (-> % .-target .-value))
                 :on-key-down key-handler})])})))

(defn to-rgb [{:keys [red green blue]}]
  (let [hex #(str (if (< % 16) "0")
                  (-> % js/Math.round (.toString 16)))]
    (str "#" (hex red) (hex green) (hex blue))))

(defn selected-shade [intensity]
  (to-rgb {:red   (- 255 (* intensity 30))
           :green (- 255 (* intensity 30))
           :blue  255}))

(declare todolist)

(defn progress-icon [percentage])

(defn todo-widget
  [path {:keys [::todos/uuid ::todos/text ::todos/subtasks ::todos/done?]
         :as todo-item}]
  (let [active-path @(rf/subscribe [:active-todo-path])
        active? (= path active-path)
        editable? (and active? @(rf/subscribe [:edit-mode?]))
        depth (count (take-while identity (map = path active-path)))]
    [:div.todo-wrapper
     {:style {:background-color (when (pos? depth) (selected-shade depth))}}
     [:div.todo-list-item
      (if editable?
        {:class "todo-editable"}
        {:class (->> [(when active? "todo-active")
                      (when done? "todo-done")]
                     (keep identity) (cs/join " "))
         :on-click #(do (rf/dispatch [:move-cursor-to-path path])
                        (rf/dispatch [:edit-mode-on]))})
      [:i {:class (if done? "fas fa-check text-success" "far fa-square")
           :on-click (fn [e]
                       (rf/dispatch
                        [:edit-todo-by-path path {:done? (not done?)}])
                       (.stopPropagation e))}]
      (if editable?
        [todo-input {:text text
                     :on-save #(when (not= (or text "") (or % ""))
                                 (rf/dispatch
                                  [:edit-todo-by-path path {:text %}]))
                     :on-stop #(rf/dispatch [:edit-mode-off])}]
        [:div.todo-text (str text " ")])]
     (when (or subtasks (= active-path (conj path 0)))
       [:div {:style {:margin-left 20}}
        (todolist path subtasks)])]))

(defn todolist [path items]
  (let [active-path @(rf/subscribe [:active-todo-path])
        num-items (count items)]
    [:div
     (for [[index todo-item] (map-indexed vector items)]
       ^{:key index} [todo-widget (conj path index) todo-item])
     (when (= active-path (conj path num-items))
       [todo-widget (conj path num-items) nil])]))

(defn todos-page []
  (component-with-handlers
   {:keydown non-edit-mode-key-handler}
   (fn []
     [:div.container.app-container
      #_[:div.mb-1
       [:button.btn.btn-default
        {:on-click #(rf/dispatch [:generate-random-db])}
        "Generate random list of tasks"]]
      [todolist [] @(rf/subscribe [:todos])]])))
