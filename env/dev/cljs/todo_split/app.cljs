(ns ^:figwheel-no-load todo-split.app
  (:require [todo-split.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
