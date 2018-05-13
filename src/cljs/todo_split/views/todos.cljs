(ns todo-split.views.todos
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as cs]
            [goog.events :as events]
            [todo-split.models.todos :as todos]
            [todo-split.views.helpers :as vh]
            [todo-split.db :as db])
  (:import [goog.events KeyCodes]))

(defn non-edit-mode-key-handler [event]
  (when-not @(rf/subscribe [:edit-mode?])
    (let [!keep-default (atom false)]
      (if (or (.-ctrlKey event) (.-altKey event))
        (reset! !keep-default true)
        (if (.-shiftKey event)
          (condp = (.-which event)
            KeyCodes.ENTER (rf/dispatch [:insert-below])
            KeyCodes.S (rf/dispatch [:split-active-todo true])
            KeyCodes.O (rf/dispatch [:insert-above])
            (reset! !keep-default true))
          (condp = (.-which event)
            KeyCodes.G (rf/dispatch [:generate-random-db])
            KeyCodes.S (rf/dispatch [:split-active-todo false])
            KeyCodes.SPACE (rf/dispatch [:toggle-active-todo])
            KeyCodes.DELETE (rf/dispatch [:cut-active-todo])
            KeyCodes.X (rf/dispatch [:cut-active-todo])
            KeyCodes.D (rf/dispatch [:cut-active-todo])
            KeyCodes.U (rf/dispatch [:undo])
            KeyCodes.R (rf/dispatch [:redo])
            KeyCodes.ENTER (rf/dispatch [:edit-mode-on])
            KeyCodes.O (rf/dispatch [:insert-below])
            KeyCodes.K (rf/dispatch [:move-cursor-up])
            KeyCodes.UP (rf/dispatch [:move-cursor-up])
            KeyCodes.J (rf/dispatch [:move-cursor-down 0])
            KeyCodes.DOWN (rf/dispatch [:move-cursor-down 0])
            KeyCodes.LEFT (rf/dispatch [:collapse-or-go-to-parent])
            KeyCodes.RIGHT (rf/dispatch [:expand-or-go-to-child])
            KeyCodes.H (rf/dispatch [:show-help])
            (reset! !keep-default true))))
      (when-not @!keep-default
        (doto event (.preventDefault) (.stopPropagation))))))

(defn edit-mode-key-handler [{:keys [save stop]} event]
  (condp = (.-which event)
    KeyCodes.ENTER (do (save)
                       (rf/dispatch (if (.-shiftKey event)
                                      [:insert-below] [:move-cursor-down 1])))
    KeyCodes.ESC (do (save) (stop))
    KeyCodes.UP (do (save) (rf/dispatch [:move-cursor-up]))
    KeyCodes.DOWN (do (save) (rf/dispatch [:move-cursor-down 1]))
    nil))

(defn todo-input [{:keys [text on-save on-stop]}]
  (let [!val (r/atom text)
        !external-update? (r/atom false)
        stop #(do (reset! !val text) (when on-stop (on-stop)))
        save #(let [v (-> @!val str cs/trim)] (on-save v))
        key-handler (partial edit-mode-key-handler {:stop stop :save save})]
    (r/create-class
     {:display-name "todo-input"
      :component-did-mount
      #(let [input (r/dom-node %)]                                       
         (.focus input)
         (vh/select-all input))
      :component-will-receive-props
      (fn [this [_ {:keys [text]}]]
        (when-not (= text @!val)
          (reset! !val text)
          (reset! !external-update? true)))
      :component-did-update                                               
      #(let [input (r/dom-node %)]
         (when @!external-update?
           (vh/select-all input)
           (reset! !external-update? false)))
      :reagent-render
      (fn [props]
        [:input.form-control
         (merge (dissoc props :on-save :on-stop :text)
                {:type        "text"
                 :value       @!val
                 :on-blur     save
                 :on-change   #(reset! !val (-> % .-target .-value))
                 :on-key-down key-handler
                 :placeholder "What needs to be done?"})])})))

(defn to-rgb [{:keys [red green blue]}]
  (let [hex #(str (if (< % 16) "0")
                  (-> % js/Math.round (.toString 16)))]
    (str "#" (hex red) (hex green) (hex blue))))

(defn selected-shade [intensity]
  (to-rgb {:red   (- 255 (* intensity 30))
           :green (- 255 (* intensity 30))
           :blue  255}))

(declare todolist)

(defn subtasks-hidden [total-subtasks completed-subtasks]
  (str total-subtasks
       " item" (when (> total-subtasks 1) "s") " hidden"
       (when (pos? completed-subtasks)
         (str " (" (if (= total-subtasks completed-subtasks)
                     "all" completed-subtasks)
              " done)"))))

(defn todo-widget
  [path todo-item]
  (r/create-class
   {:display-name "todo-list-item-widget"
    :component-will-update
    (fn [this [_ path]]
      (when (= path @(rf/subscribe [:active-todo-path]))
        (vh/scroll-into-view-if-needed! (r/dom-node this))))
    :reagent-render
    (fn [path {:keys [::todos/text ::todos/subtasks ::todos/created-at
                      ::todos/collapsed?] :as todo-item}]
      (let [active-path @(rf/subscribe [:active-todo-path])
            active? (= path active-path)
            editable? (and active? @(rf/subscribe [:edit-mode?]))
            [done-subtasks total-subtasks done-at] (todos/done-status todo-item)
            done? (= done-subtasks total-subtasks)
            depth (count (take-while identity (map = path active-path)))
            collapsed? (and (seq subtasks) collapsed?)]
        [:div.todo-wrapper
         {:style {:background-color (when (pos? depth) (selected-shade depth))}}
         [:div.todo-list-item
          ;; Class and handlers of the task's widget.
          (if editable?
            {:class "todo-editable"}
            {:class (->> [(when active? "todo-active")
                          (when done? "todo-done")]
                         (keep identity) (cs/join " "))
             :on-click #(do (rf/dispatch [:move-cursor-to-path path])
                            (rf/dispatch [:edit-mode-on]))})
          ;; Task's status icon and its click handlers.
          (if (seq subtasks)
            [:span.icon
             {:on-click (fn [e]
                          (rf/dispatch
                           [:edit-todo-by-path path
                            {:collapsed? (not collapsed?)}])
                          (.stopPropagation e))}
             (vh/completion-chart (/ done-subtasks total-subtasks))]
            [:i.icon
             {:class (if done? "fas fa-check text-success" "far fa-square")
              :on-click (fn [e]
                          (rf/dispatch
                           [:edit-todo-by-path path {:done? (not done?)}])
                          (.stopPropagation e))}])
          ;; Task's text.
          (if editable?
            ^{:key path}
            [todo-input {:text text
                         :on-save #(when (not= (or text "") (or % ""))
                                     (rf/dispatch
                                      [:edit-todo-by-path path {:text %}]))
                         :on-stop #(rf/dispatch [:edit-mode-off])}]
            [:div.todo-text (str text " ")])
          (when (or done? collapsed?))
          [:div.small.text-muted {:style {:margin-right 20}}
           (when done?
             [:div (str done-at)])
           (when collapsed?
             [:div (subtasks-hidden total-subtasks done-subtasks)])]]
         (when (and (seq subtasks) (not collapsed?))
           [:div {:style {:margin-left 20}}
            [todolist path subtasks]])]))}))

(defn todolist [path items]
  (let [active-path @(rf/subscribe [:active-todo-path])
        num-items (count items)]
    [:div
     (for [[index todo-item] (map-indexed vector items)]
       ^{:key index} [todo-widget (conj path index) todo-item])
     (when (= active-path (conj path num-items))
       [todo-widget (conj path num-items) nil])]))

(defn initial-advice []
  [:div.mt-4
   [:div "This is a simple to-do list app."]
   [:ul
    [:li "Enter what you want to do."]
    [:li "For help on controls and keyboard shortcuts, "
     "click " [:strong "Help"] " in the menu above."]
    [:li "Not sure what you need to do? Click "
     [:a.btn.btn-sm.btn-info
      {:on-click #(rf/dispatch [:generate-random-db])}
      "here"] " to have a list of tasks generated "
     "for you."]]])

(defn todos-page []
  (if-not @(rf/subscribe [:db-initialized?])
    (vh/waiting-screen "Initializing list")
    (vh/component-with-handlers
     {:keydown non-edit-mode-key-handler}
     (fn []
       (let [todos @(rf/subscribe [:todos])]
         [:div.container.app-container
          [todolist [] todos]
          (when (empty? todos)
            [initial-advice])])))))
