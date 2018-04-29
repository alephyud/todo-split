(ns todo-split.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [pjstadig.humane-test-output]
            [reagent.core :as reagent :refer [atom]]
            [todo-split.models.todos :as todos]
            [todo-split.events :as events]))

(deftest wtf-test
  (is (= 1 1)))

(def sample-todos
  [{::todos/uuid (uuid "bb2a02a3-b678-4997-bc49-a5a12c4ac9dc"),
    ::todos/text "NLYJXQI9mZZVxBXLBIQ0Pzk6tEt"}
   {::todos/uuid (uuid "9caa67c5-f31c-4c01-96e4-6264199540b6"),
    ::todos/text "p1DdP3kC78kn8Oo4QT"}
   {::todos/uuid (uuid "1545da52-1e51-4e05-b1f9-c170dcdaf80b"),
    ::todos/text "Yb9Mhq875zE8ncctO90zleULoRWA"}
   {::todos/uuid (uuid "c677ef34-1920-4592-a34f-0914dc841f79"),
    ::todos/text "Y5J17wW3"}
   {::todos/uuid (uuid "fb8bedd9-7bdb-431f-a36c-9f995038c303"),
    ::todos/text "7Z0fs480Q1gsTqeG7K2Fsa",
    ::todos/subtasks
    [{::todos/uuid (uuid "6f316ccf-4b0f-476a-8ae0-f93eed4c8dd8"),
      ::todos/text "HvkofybYyFCB5P10",
      ::todos/subtasks
      [{::todos/uuid (uuid "3a8d7d7c-f2de-4f8c-9ea9-118f0e4157ed"),
        ::todos/text "GFghZsrZJ"}]}
     {::todos/uuid (uuid "e107f1d8-94db-4282-b9b9-1b33ffe5e439"),
      ::todos/text "3nD9JRqW70JD3",
      ::todos/subtasks []}
     {::todos/uuid (uuid "fff52503-ff66-4dc5-912a-42a5f6680d11"),
      ::todos/text "5P2rXwd8H"}
     {::todos/uuid (uuid "d06715f7-da73-44c1-84a7-e9dedcf5826e"),
      ::todos/text "tmADUe8WbFanapVOi9u2mpe5AJq7"}]}])

(deftest todos-traverse-up
  (is (= [0] (todos/traverse-up sample-todos [1])))
  (is (= [1] (todos/traverse-up sample-todos [2]))))

(deftest todos-traverse-down
  (testing "Traversing the tree downwards without adding new items"
    (is (= [1] (todos/traverse-down sample-todos [0] false)))
    (is (= [4] (todos/traverse-down sample-todos [3] false)))
    (is (= [4 0] (todos/traverse-down sample-todos [4] false)))
    (is (= [4 0 0] (todos/traverse-down sample-todos [4 0] false)))
    (is (= [4 1] (todos/traverse-down sample-todos [4 0 0] false)))
    (is (= [4 2] (todos/traverse-down sample-todos [4 1] false)))
    (is (= [4 3] (todos/traverse-down sample-todos [4 2] false)))
    (is (= [4 3] (todos/traverse-down sample-todos [4 3] false))))
  (testing "Traversing the tree downwards with adding new items"
    (is (= [0 0] (todos/traverse-down sample-todos [0] true)))
    (is (= [1] (todos/traverse-down sample-todos [0 0] true)))
    (is (= [3 0] (todos/traverse-down sample-todos [3] true)))
    (is (= [4 0] (todos/traverse-down sample-todos [4] true)))
    (is (= [4 0 0] (todos/traverse-down sample-todos [4 0] true)))
    (is (= [4 0 0 0] (todos/traverse-down sample-todos [4 0 0] true)))
    (is (= [4 1 0] (todos/traverse-down sample-todos [4 1] true)))
    (is (= [4 2 0] (todos/traverse-down sample-todos [4 2] true)))
    (is (= [4 3 0] (todos/traverse-down sample-todos [4 3] true)))))

(deftest adding-and-editing
  (testing "Editing existing items"
    (is (= "New text" (-> {:db sample-todos}
                          (events/edit-todo-by-path [[0] "New text"])
                          first ::todos/text))))
  (testing "Adding new items"
    (is (= "New text" (-> {:db sample-todos :new-uuid (uuid "1")}
                          (events/edit-todo-by-path [[0 0] "New text"])
                          first ::todos/subtasks first ::todos/text)))))
