(ns todo-split.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [todo-split.core-test]))

(doo-tests 'todo-split.core-test)

