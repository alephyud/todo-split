(ns todo-split.views.help
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as cs]
            [todo-split.views.helpers :as vh])
  (:import [goog.events KeyCodes]))

(defn key-handler [event]
  (condp = (.-which event)
    KeyCodes.ESC #(rf/dispatch [:go-to-home-page])
    nil))

(defn help-page []
  (vh/component-with-handlers
   {:keydown key-handler}
   (fn []
     [:div.container.app-container
      [:table.table
       [:thead>tr
        [:th "Key"]
        [:th "Action"]]
       [:tbody
        [:tr
         [:td [:kbd "Enter"]]
         [:td "Edit the current task"]]
        [:tr
         [:td [:kbd "Shift"] " + " [:kbd "Enter"]]
         [:td "Add and edit a new task after the current one"]]
        [:tr
         [:td [:kbd "Space"]]
         [:td "Mark the current task done (or undone)"]]
        [:tr
         [:td [:kbd "S"]]
         [:td "Split the current task into subtasks"]]
        [:tr
         [:td [:kbd "Shift"] " + " [:kbd "S"]]
         [:td "Split the current task inline"]]
        [:tr
         [:td [:kbd "Del"] " or " [:kbd "X"]]
         [:td "Delete the current task"]]
        [:tr
         [:td [:kbd "Up"] " or " [:kbd "K"]]
         [:td "Move cursor up"]]
        [:tr
         [:td [:kbd "Down"] " or " [:kbd "J"]]
         [:td "Move cursor down"]]
        [:tr
         [:td [:kbd "U"]]
         [:td "Undo last action"]]
        [:tr
         [:td [:kbd "R"]]
         [:td "Redo last action"]]
        [:tr
         [:td [:kbd "G"]]
         [:td "Generate a random list of tasks"]]]]])))
