(ns todo-split.views.helpers
  (:require [reagent.core :as r]
            [clojure.string :as cs]))

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

(defn scroll-into-view-if-needed! [elem]
  ;; https://gist.github.com/hsablonniere/2581101
  (let [parent js/document.body.parentElement ; (.-parentNode elem)
        parent-style (js/window.getComputedStyle parent nil)
        parent-border-top (-> parent-style
                              (.getPropertyValue "border-top-width")
                              js/parseInt)
        parent-border-left (-> parent-style
                               (.getPropertyValue "border-left-width")
                               js/parseInt)
        over-top? (< (- (.-offsetTop elem) (.-offsetTop parent))
                     (.-scrollTop parent))
        over-bottom? (> (- (+ (.-offsetTop elem) (.-clientHeight elem))
                            (.-offsetTop parent) parent-border-top)
                         (+ (.-scrollTop parent) (.-clientHeight parent)))
        over-left? (< (- (.-offsetLeft elem) (.-offsetLeft parent))
                      (.-scrollLeft parent))
        over-right? (> (- (+ (.-offsetLeft elem) (.-clientWidth elem))
                           (.-offsetLeft parent) parent-border-left)
                        (+ (.-scrollLeft parent) (.-clientWidth parent)))
        align-with-top? (and over-top? (not over-bottom?))]
    #_(println "Check: " (.-offsetTop elem) (.-clientHeight elem)
             (.-offsetTop parent) parent-border-top
             (.-scrollTop parent) (.-clientHeight parent))
    (when (or over-top? over-bottom? over-left? over-right?)
      (.scrollIntoView elem align-with-top?))))

(defn completion-chart [percentage]
  (let [radius 0.9
        coords-for #(let [w (* 2 % Math/PI)]
                      [(* radius (Math/cos w)) (* radius (Math/sin w))])
        large-arc? (if (> percentage 0.5) 1 0)
        path-data (-> (into ["M"] (coords-for 0))
                      (into ["A" radius radius 0 large-arc? 1])
                      (into (coords-for percentage))
                      (into ["L" 0 0]))]
    [:svg {:view-box "-1 -1 2 2"
           :style {:transform "rotate(-90deg)"}
           :width "1rem"}
     [:path {:d (cs/join " " path-data) :fill "green"}]
     [:circle {:cx 0 :cy 0 :r 0.9 :stroke "#555" :fill "none"
               :stroke-width 0.15}]]))

(defn waiting-screen [message]
  [:div.container.waiting-screen.app-container
   [:div.waiting-message
    [:i.fas.fa-spinner.fa-pulse.fa-3x]
    [:div message]]])
