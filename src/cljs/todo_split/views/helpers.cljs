(ns todo-split.views.helpers
 (:require [reagent.core :as r]))

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
