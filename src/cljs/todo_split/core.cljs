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
            [todo-split.views.todos :as views.todos]
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
   [:button.navbar-toggler.hidden-sm-up
    {:type "button"
     :data-toggle "collapse"
     :data-target "#collapsing-navbar"}
    [:span.navbar-toggler-icon]]
   [:a.navbar-brand {:href "#/"} "todo-split"]
   [:div#collapsing-navbar.collapse.navbar-collapse
    [:ul.nav.navbar-nav.mr-auto
     [nav-link "Home" :home]
     [nav-link "Dev info" :info]
     [nav-link "About" :about]]]])

(defn about-page []
  [:div.container.app-container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])

(defn stub-page []
  [:div.container.app-container
   [:div.row>div.col-sm-12
    [:h2.alert.alert-info "Tip: try pressing CTRL+H to open re-frame tracing menu"]]
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(def pages
  {:home #'views.todos/todos-page
   :info #'stub-page
   :about #'about-page})

(defn page []
  [:div
   [navbar]
   [(#'pages (or (:handler @(rf/subscribe [:kee-frame/route])) :home))]])

;; -------------------------
;; Routes

(def routes ["" {"/" :home
                 "/info" :info
                 "/about" :about}])

#_(reg-controller :my-controller
                {:params (fn [{:keys [handler route-params]}]
                           (println handler route-params))
                 :start (fn [& args] (println args))})

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}))

(defn start-kf! []
  (kf/start! {:routes routes
              :app-db-spec ::db/db
              :initial-db db/default-db
              :debug? true
              :root-component [page]}))

(defn init! []
  (rf/clear-subscription-cache!)
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (rf/dispatch [:initialize-db])
  (start-kf!))

