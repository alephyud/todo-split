(ns todo-split.core
  (:require [reagent.core :as r]
            [clojure.string :as cs]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [todo-split.ajax :refer [load-interceptors!]]
            [kee-frame.core :as kf :refer [reg-controller]]
            [todo-split.events]
            [todo-split.models.todos :as todos]
            [todo-split.db :as db])
  (:import goog.History))

(defn nav-link [title page]
  [:li.nav-item
   {:class (when (= page (some-> (rf/subscribe [:key-frame/route]) deref)) "active")}
   [:a.nav-link {:href (kf/path-for page)} title]])

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
  [:div.container
   [:div.row
    [:div.col-md-12
     [:img {:src (str js/context "/img/warning_clojure.png")}]]]])

(defn todo-input [{:keys [text on-save on-stop]}]
  (let [!val (r/atom text)
        stop #(do (reset! !val "")
                  (when on-stop (on-stop)))
        save #(let [v (-> @!val str cs/trim)]
                (on-save v)
                (stop))]
    (r/create-class
     {:component-did-mount                                               
      #(let [input (r/dom-node %)]                                       
         (doto input                                                      
           (.focus)                                                       
           (.setSelectionRange 0 (.. input -value -length))))
      :reagent-render
      (fn [props]
        [:input (merge (dissoc props :on-save :on-stop :text)
                       {:type        "text"
                        :value       @!val
                        :on-blur     save
                        :on-change   #(reset! !val (-> % .-target .-value))
                        :on-key-down #(case (.-which %)
                                        13 (save)
                                        27 (stop)
                                        nil)})])})))

(defn todo-widget [{:keys [::todos/uuid] :as todo-item}]
  (let [!editable? (r/atom (= @(rf/subscribe [:new-todo-id]) uuid))]
    (fn [{:keys [::todos/uuid ::todos/text]}]
      (if @!editable?
        [:div [todo-input {:text text
                           :on-save #(rf/dispatch [:change-text uuid %])
                           :on-stop #(do (reset! !editable? false)
                                         (rf/dispatch [:reset-new-todo-id]))}]]
        [:div {:on-click #(reset! !editable? true)} text]))))

(defn todos-page []
  (let [!todos (rf/subscribe [:todos])]
    [:div.container.app-container
     (if (seq @!todos)
       [:ul
        (for [{:keys [::todos/uuid] :as todo-item} @!todos]
          ^{:key uuid} [:li [todo-widget todo-item]])]
       [:div "You either have done everything you planned or have not yet "
        "entered what to do."])
     [:button.btn.btn-primary
      {:on-click #(rf/dispatch [:add-todo {::todos/uuid (random-uuid)
                                           ::todos/text "Todo"}])}
      "New"]]))

(defn stub-page []
  [:div.container
   [:div.row>div.col-sm-12
    [:h2.alert.alert-info "Tip: try pressing CTRL+H to open re-frame tracing menu"]]
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(def pages
  {:home #'todos-page
   :info #'stub-page
   :about #'about-page})

(defn page []
  [:div
   [navbar]
   [(pages (some-> (rf/subscribe [:key-frame/route]) deref) "WTF")]])

;; -------------------------
;; Routes
(comment
  (secretary/set-config! :prefix "#")

  (secretary/defroute "/" []
    (rf/dispatch [:set-active-page :home]))

  (secretary/defroute "/info" []
    (rf/dispatch [:set-active-page :info]))

  (secretary/defroute "/about" []
    (rf/dispatch [:set-active-page :about])))

(def routes ["" {"/" :home
                 "/info" :info
                 "/about" :about}])

(reg-controller :my-controller
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

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  #_(rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  #_(hook-browser-navigation!)
  #_(mount-components)
  (kf/start! {:routes routes
              :app-db-spec ::db/db
              :initial-db db/default-db
              :debug? true
              :root-component [page]}))
