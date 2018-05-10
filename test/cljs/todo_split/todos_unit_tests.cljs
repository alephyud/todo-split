(ns todo-split.todos-unit-tests
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [pjstadig.humane-test-output]
            [clojure.spec.alpha :as s]
            [todo-split.models.todos :as todos]
            [todo-split.db :as db]))

(def sample-todos
  [{::todos/uuid (uuid "bb2a02a3-b678-4997-bc49-a5a12c4ac9dc"),
    ::todos/text "Feed the cat"}
   {::todos/uuid (uuid "9caa67c5-f31c-4c01-96e4-6264199540b6"),
    ::todos/text "p1DdP3kC78kn8Oo4QT"
    ::todos/done? true}
   {::todos/uuid (uuid "1545da52-1e51-4e05-b1f9-c170dcdaf80b"),
    ::todos/text "Yb9Mhq875zE8ncctO90zleULoRWA"}
   {::todos/uuid (uuid "c677ef34-1920-4592-a34f-0914dc841f79"),
    ::todos/text "Y5J17wW3"}
   {::todos/uuid (uuid "fb8bedd9-7bdb-431f-a36c-9f995038c303"),
    ::todos/text "7Z0fs480Q1gsTqeG7K2Fsa",
    ::todos/subtasks
    [{::todos/uuid (uuid "6f316ccf-4b0f-476a-8ae0-f93eed4c8dd8"),
      ::todos/text "HvkofybYyFCB5P10",
      ::todos/collapsed? true
      ::todos/subtasks
      [{::todos/uuid (uuid "3a8d7d7c-f2de-4f8c-9ea9-118f0e4157ed"),
        ::todos/text "GFghZsrZJ"}]}
     {::todos/uuid (uuid "e107f1d8-94db-4282-b9b9-1b33ffe5e439"),
      ::todos/text "3nD9JRqW70JD3",
      ::todos/subtasks []}
     {::todos/uuid (uuid "fff52503-ff66-4dc5-912a-42a5f6680d11"),
      ::todos/text "make the slides"}
     {::todos/uuid (uuid "d06715f7-da73-44c1-84a7-e9dedcf5826e"),
      ::todos/text "tmADUe8WbFanapVOi9u2mpe5AJq7"}]}])

(deftest todos-basic
  (is (s/valid? ::db/db {::db/todos sample-todos ::db/active-todo-path [0]}))
  (is (s/valid? ::db/db {::db/todos sample-todos ::db/active-todo-path [5]}))
  (is (s/valid? ::db/db {::db/todos sample-todos ::db/active-todo-path [4 3]})))

(deftest todos-traverse-up
  (are [after before] (= after (todos/traverse-up
                                sample-todos before true))
    [0] [0]
    [0] [1]
    [3] [4]
    [4] [4 0]
    [4 0] [4 1]
    [4 1] [4 2]
    [4 2] [4 3])
  (is (= [4 0 0] (todos/traverse-up sample-todos [4 1] false))))

(deftest todos-traverse-down
  (testing "Traversing the tree downwards without adding new items"
    (are [before after] (= after (todos/traverse-down
                                  sample-todos before 0 true))
      [0] [1]
      [3] [4]
      [4] [4 0]
      [4 0] [4 1]
      [4 0 0] [4 1]
      [4 1] [4 2]
      [4 2] [4 3]
      [4 3] [4 3]))
  (is (= [4 0 0] (todos/traverse-down sample-todos [4 0] false false)))
  (is (= [1] (todos/traverse-down sample-todos [0] 1 false)))
  (is (= [5] (todos/traverse-down sample-todos [4 3] 1 false)))
  (testing "Traversing the tree downwards with adding new items"
    (are [before after] (= after (todos/traverse-down
                                  sample-todos before 5 true))
      [0] [1]
      [0 0] [1]
      [3] [4]
      [4] [4 0]
      [4 0] [4 1]
      [4 0 0] [4 0 1]
      [4 1] [4 2]
      [4 2] [4 3]
      [4 3] [4 4])))

(deftest todos-adding-and-editing
  (testing "Editing existing items"
    (is (= "New text" (-> {:db sample-todos}
                          (todos/edit-todo-by-path [[0] {:text "New text"}])
                          first ::todos/text))))
  (testing "Marking items as done or not done"
    (is (= true (-> {:db sample-todos}
                    (todos/edit-todo-by-path [[0] {:done? true}])
                    first ::todos/done?)))
    (is (= false (-> {:db sample-todos}
                     (todos/edit-todo-by-path [[1] {:done? false}])
                     second ::todos/done?))))
  (testing "Adding new sub-items"
    (is (= "New text" (-> {:db sample-todos :new-uuids [(uuid "1")]}
                          (todos/edit-todo-by-path [[0 0] {:text "New text"}])
                          first ::todos/subtasks first ::todos/text))))
  (testing "Inserting new items"
    (let [n (-> sample-todos count)
          result (todos/insert-at sample-todos [3] (uuid "42"))]
      (is (= (inc n) (count result)))
      (is (= (uuid "42") (::todos/uuid (get result 3)))))
    (let [n (-> sample-todos (get 4) ::todos/subtasks count)
          result (todos/insert-at sample-todos [4 2] (uuid "42"))
          result-branch (-> result (get 4) ::todos/subtasks)]
      (is (= (inc n) (count result-branch)))
      (is (= (uuid "42") (::todos/uuid (get result-branch 2)))))))

(deftest todos-cutting
  (is (= (subvec sample-todos 1)
         (first (todos/cut-todos sample-todos [0]))))
  (is (= (into (subvec sample-todos 0 1) (subvec sample-todos 2))
         (first (todos/cut-todos sample-todos [1]))))
  (is (= (let [key-path [4 ::todos/subtasks]
               subtasks (get-in sample-todos key-path)]
           (assoc-in sample-todos key-path
                     (into (subvec subtasks 0 1) (subvec subtasks 2))))
         (first (todos/cut-todos sample-todos [4 1]))))
  (is (= true (last (todos/cut-todos sample-todos [4 3])))))

(deftest todos-splitting
  (let [uuids [(uuid "1") (uuid "2")]]
    (testing "Tree splitting"
      (let [result (todos/split-todo sample-todos [0] uuids false)
            first-result (-> result first ::todos/subtasks first)]
        (is (= 2 (count (::todos/subtasks (first result)))))
        (is (= "First step to feed the cat" (::todos/text first-result)))
        (is (= (uuid "1") (::todos/uuid first-result)))))
    (testing "Inline splitting"
      (let [n (count sample-todos)
            result (todos/split-todo sample-todos [0] uuids true)]
        (is (= (inc n) (count result)))
        (is (= "First step to feed the cat" (-> result first ::todos/text))))
      (let [n (-> sample-todos (get 4) ::todos/subtasks count)
            result (todos/split-todo sample-todos [4 2] uuids true)
            result-branch (-> result (get 4) ::todos/subtasks)
            first-result (get result-branch 2)]
        (is (= (inc n) (count result-branch)))
        (is (= "First step to make the slides" (::todos/text first-result)))
        (is (= (uuid "1") (::todos/uuid first-result)))))))

(deftest regression-cases
  (testing "Completion status is preserved when the list is edited"
    (let [tasks [{::todos/text "Compound task"
                  ::todos/uuid (uuid "0")
                  ::todos/done? false
                  ::todos/subtasks
                  [{::todos/text "Done task"
                    ::todos/uuid (uuid "1")
                    ::todos/done? true}
                   {::todos/text "Task being edited"
                    ::todos/uuid (uuid "2")
                    ::todos/done? false}]}]
          edit-result
          (todos/edit-todo-by-path {:db tasks} [[0 1] {:text "New text"}])
          insert-result
          (todos/insert-at tasks [0 1] (uuid "42"))]
      (is (::todos/done? (-> edit-result first ::todos/subtasks first)))
      (is (= "New text" (-> edit-result first ::todos/subtasks
                            second ::todos/text)))
      (is (::todos/done? (-> insert-result first ::todos/subtasks first))))))
