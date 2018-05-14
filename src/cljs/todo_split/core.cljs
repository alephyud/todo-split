(ns todo-split.core
  (:require [reagent.core :as r]
            [clojure.string :as cs]
            [clojure.test.check.generators]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [todo-split.ajax :refer [load-interceptors!]]
            [kee-frame.core :as kf :refer [reg-controller]]
            [todo-split.events]
            [todo-split.views.todos]
            [todo-split.views.help]
            [todo-split.db :as db])
  (:import goog.History
           [goog.events KeyCodes]))

(defn nav-link [title page]
  [:li.nav-item
   {:class (when (= page (:handler @(rf/subscribe [:kee-frame/route])))
             "active")}
   [:a.nav-link {:href (kf/path-for [page])} title]])

(defn navbar []
  [:nav.navbar.navbar-dark.bg-primary.navbar-expand-md
   {:role "navigation"}
   [:div.container
    [:button.navbar-toggler.hidden-sm-up
     {:type "button"
      :data-toggle "collapse"
      :data-target "#collapsing-navbar"}
     [:span.navbar-toggler-icon]]
    [:a.navbar-brand {:href "#/"} "todo-split"]
    [:div#collapsing-navbar.collapse.navbar-collapse
     [:ul.nav.navbar-nav.mr-auto
      [nav-link "Home" :home]
      [nav-link "Help" :help]]]]])

(def pages
  {:home #'todo-split.views.todos/todos-page
   :help #'todo-split.views.help/help-page})

(defn page []
  [:div.app-page 
   [navbar]
   [:main
    [(#'pages (or (:handler @(rf/subscribe [:kee-frame/route])) :home))]]])

;; -------------------------
;; Routes

(def routes ["" {"/" :home
                 "/help" :help}])

;; -------------------------
;; Initialize app

(defn start-kf! []
  (kf/start! {:routes routes
              :app-db-spec ::db/db
              :initial-db db/default-db
              :debug? true
              :root-component [page]}))

(defn init! []
  (rf/clear-subscription-cache!)
  (load-interceptors!)
  (rf/dispatch [:initialize-db])
  (start-kf!))

