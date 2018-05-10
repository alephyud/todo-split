(ns todo-split.views.help
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as cs]
            [todo-split.views.helpers :as vh])
  (:import [goog.events KeyCodes]))

(defn key-handler [event]
  (condp = (.-which event)
    KeyCodes.ESC (rf/dispatch [:hide-help])
    nil))

(defn help-page []
  (vh/component-with-handlers
   {:keydown key-handler}
   (fn []
     [:div.container.app-container
      [:h2 "Key bindings"]
      [:p "You should use this app from a desktop / laptop to access all"
          "of its functions. Mobile controls are not yet implemented."]
      [:table.table
       [:tbody
        [:tr
         [:td [:kbd "Enter"]]
         [:td "Edit the selected task"]]
        [:tr
         [:td [:kbd "Shift"] " + " [:kbd "Enter"] " or " [:kbd "O"]]
         [:td "Add and edit a new task after the selected one"]]
        [:tr
         [:td [:kbd "Shift"] " + " [:kbd "O"]]
         [:td "Add and edit a new task before the selected one"]]
        [:tr
         [:td [:kbd "Space"]]
         [:td "Mark the selected task done (or undone)"]]
        [:tr
         [:td [:kbd "S"]]
         [:td "Split the selected task into subtasks"]]
        [:tr
         [:td [:kbd "Shift"] " + " [:kbd "S"]]
         [:td "Split the selected task inline"]]
        [:tr
         [:td [:kbd "Del"], ", " [:kbd "D"] " or " [:kbd "X"]]
         [:td "Delete the selected task"]]
        [:tr
         [:td [:kbd "Up"] " or " [:kbd "K"]]
         [:td "Move cursor up"]]
        [:tr
         [:td [:kbd "Down"] " or " [:kbd "J"]]
         [:td "Move cursor down"]]
        [:tr
         [:td [:kbd "Right"]]
         [:td "Expand item"]]
        [:tr
         [:td [:kbd "Left"]]
         [:td "Collapse item"]]
        [:tr
         [:td [:kbd "U"]]
         [:td "Undo last action"]]
        [:tr
         [:td [:kbd "R"]]
         [:td "Redo last action"]]
        [:tr
         [:td [:kbd "G"]]
         [:td "Generate a random list of tasks"]]
        [:tr
         [:td [:kbd "H"]]
         [:td "Display this help message"]]]]])))
