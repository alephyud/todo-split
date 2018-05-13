(ns todo-split.integration-tests
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [clojure.spec.alpha :as s]
            [pjstadig.humane-test-output]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [day8.re-frame.test :as rf-test]
            [todo-split.db :as db]
            [todo-split.models.todos :as todos]
            [akiroz.re-frame.storage :refer [<-store]]
            [todo-split.events :as events]))

(defn clear-todos! []
  (rf/dispatch [:initialize-db])
  (while (seq @(rf/subscribe [:todos]))
    (rf/dispatch [:cut-todos [0]])))

(deftest persistence-test
  (rf-test/run-test-sync
   (clear-todos!)
   (is (s/valid? ::db/db @re-frame.db/app-db))
   (is (empty? @(rf/subscribe [:todos])) "Clean initial list")
   (rf/dispatch [:edit-todo-by-path [0] {:text "Stop procrastinating"}])
   (rf/dispatch [:edit-todo-by-path [1] {:text "Highlighted task"}])
   (rf/dispatch [:move-cursor-down])
   (is (s/valid? ::db/db @re-frame.db/app-db))
   ;; Now, reset DB directly and re-initialize it (emulating a page reload)
   (swap! re-frame.db/app-db assoc ::db/todos [])
   (rf/dispatch [:initialize-db])
   ;; The state must be preserved
   (is (= 2 (count @(rf/subscribe [:todos]))))
   (is (= [1] @(rf/subscribe [:active-todo-path])))))

(deftest undo-test
  (testing "Undo should not interfere with persistence"
    (rf-test/run-test-sync
     (clear-todos!)
     (is (empty? @(rf/subscribe [:todos])) "Clean initial list")
     (rf/dispatch [:edit-todo-by-path [0]
                   {:text "Stop procrastinating"}])
     (rf/dispatch [:edit-todo-by-path [1]
                   {:text "Stop procrastinating now"}])
     (rf/dispatch [:edit-todo-by-path [2]
                   {:text "Stop procrastinating, finally"}])
     (is (= 3 (count @(rf/subscribe [:todos]))) "Before deleting")
     (rf/dispatch [:cut-todos [1]])
     (is (s/valid? ::db/db @re-frame.db/app-db))
     (rf/dispatch [:undo])
     (is (s/valid? ::db/db @re-frame.db/app-db))
     (is (= 3 (count @(rf/subscribe [:todos]))) "After undo")
     (rf/dispatch [:edit-todo-by-path [0] {:text "New text"}])
     (is (s/valid? ::db/db @re-frame.db/app-db))
     (is (= 3 (count @(rf/subscribe [:todos]))) "After undo and changes"))))

(deftest toggle-done-test
  (testing ":toggle-active-todo event should work normally"
    (rf-test/run-test-sync
     (clear-todos!)
     (rf/dispatch [:edit-todo-by-path [0] {:text "Make sure this test works"}])
     (rf/dispatch [:toggle-active-todo])
     (is (s/valid? ::db/db @re-frame.db/app-db))
     (is (= 1 (first (todos/done-status (first @(rf/subscribe [:todos]))))))
     (rf/dispatch [:toggle-active-todo])
     (is (s/valid? ::db/db @re-frame.db/app-db))
     (is (= 0 (first (todos/done-status (first @(rf/subscribe [:todos]))))))
     (rf/dispatch [:toggle-active-todo])
     (is (s/valid? ::db/db @re-frame.db/app-db))
     (is (= 1 (first (todos/done-status (first @(rf/subscribe [:todos])))))))))
