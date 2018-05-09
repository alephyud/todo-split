(ns todo-split.integration-tests
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
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
   (is (empty? @(rf/subscribe [:todos])) "Clean initial list")
   (rf/dispatch [:edit-todo-by-path [0] {:text "Stop procrastinating"}])
   (rf/dispatch [:edit-todo-by-path [1] {:text "Highlighted task"}])
   (rf/dispatch [:move-cursor-down])
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
     (rf/dispatch [:undo])
     (is (= 3 (count @(rf/subscribe [:todos]))) "After undo")
     (rf/dispatch [:edit-todo-by-path [0] {:text "New text"}])
     (is (= 3 (count @(rf/subscribe [:todos]))) "After undo and changes"))))

(def saved-todos
 "[[\"^ \",\"~:todo-split.models.todos/uuid\",\"~ua4dc1445-eb45-4157-bc1a-a31e9c6ab023\",\"~:todo-split.models.todos/text\",\"Send pension fund telephone number to Olga\",\"~:todo-split.models.todos/subtasks\",[[\"^ \",\"^0\",\"~u98807abf-c51a-4f52-bd5e-79fb822f7071\",\"^1\",\"Find the pension fund number\",\"~:todo-split.models.todos/done?\",true],[\"^ \",\"^0\",\"~u85596121-a05e-4ee6-82fb-cb2f17b4062c\",\"^1\",\"Message Olga\",\"^3\",true,\"~:todo-split.models.todos/collapsed?\",false]],\"^3\",true,\"^4\",true],[\"^ \",\"^0\",\"~uc3282cec-e9fa-47a0-97c5-58cb0f841584\",\"^1\",\"Respond to Zozulya\",\"^3\",true],[\"^ \",\"^0\",\"~uf6730823-b8cb-4aeb-a2cc-966e5af5fb31\",\"^1\",\"Follow up with Leila re: Skoltech work\",\"^2\",[[\"^ \",\"^0\",\"~ub89dd545-9f99-4c20-892f-8177b9a82a32\",\"^1\",\"Analyze the output file\",\"^3\",true,\"^4\",true],[\"^ \",\"^0\",\"~u2b7fecce-3769-4e5f-9779-8043b5ae3b2a\",\"^1\",\"Cross-check yields vs. Vyacheslav's inputs\",\"^2\",[[\"^ \",\"^0\",\"~u2396ede7-aa7d-4035-b651-b249ffdb2a1d\",\"^1\",\"Find the field number in the correspondence with Leila (7к4)\",\"^3\",true],[\"^ \",\"^0\",\"~ud3dbc977-ae83-4b2c-8736-2a84423843a7\",\"^1\",\"Open Vyacheslav's inputs for 2017\",\"^3\",true],[\"^ \",\"^0\",\"~ucbca5a0d-831f-4786-97bb-f37189cc58fd\",\"^1\",\"Mismatched crops - check!\",\"^3\",true,\"^4\",true]],\"^3\",true]],\"^3\",true,\"^4\",true],[\"^ \",\"^0\",\"~uf6d124f0-83e7-4278-8d50-0b03cd1ee863\",\"^1\",\"BUG: Done status is reset when the todo list is edited\",\"^3\",true],[\"^ \",\"^0\",\"~ud1c53799-1ae8-4d53-aee0-21fb144a4559\",\"^1\",\"BUG: Default scrolling behaviour for space, arrow keys etc. must be disabled\",\"^3\",true],[\"^ \",\"^0\",\"~u52ee7657-a943-46f6-a321-489f2ec4f9ef\",\"^1\",\"Scroll must follow selected item\",\"^3\",true],[\"^ \",\"^0\",\"~ufe5f3b95-dc95-4d8f-b87c-82382727eff5\",\"^1\",\"Reply to Oseledets and Skoltech team\",\"^2\",[[\"^ \",\"^0\",\"~u024facb8-c0c5-4434-ade3-2da7524bc5a1\",\"^1\",\"Leila's study leave\",\"^3\",true],[\"^ \",\"^0\",\"~uce774252-4f61-48b4-9dcb-a173e25f6307\",\"^1\",\"Satellite data access\",\"^3\",true]],\"^3\",false,\"^4\",true],[\"^ \",\"^0\",\"~u9c465afe-d6ce-4d8d-b1ae-6c90591b3e68\",\"^1\",\"Completion status for tasks with subtasks should be based on the subtasks' status\",\"^2\",[[\"^ \",\"^0\",\"~u0d788ac7-0e32-4262-a049-2271de9fae1e\",\"^1\",\"Find how to display a pie chart instead of an icon\",\"^3\",true],[\"^ \",\"^0\",\"~uff922389-411a-48df-8f40-078273fb4b7d\",\"^1\",\"Write a \\\"done percentage\\\" function\",\"^3\",true],[\"^ \",\"^0\",\"~u97da6b1a-d885-4f0f-a7c3-888d4bb116f2\",\"^1\",\"Code the 'pie' icon as needed\",\"^3\",true]],\"^4\",true],[\"^ \",\"^0\",\"~u43f07e78-1cfb-4225-9a29-f5082e7ffcf4\",\"^1\",\"Collapsing / expanding tasks\",\"^2\",[[\"^ \",\"^0\",\"~uc6065b5d-91f8-442c-8c22-44e47fd3afc9\",\"^1\",\"Expanded / collapsed status in the database\",\"^3\",true],[\"^ \",\"^0\",\"~ued8683da-98e3-41a4-98ca-c2e09367f2dd\",\"^1\",\"Rendering\",\"^3\",true],[\"^ \",\"^0\",\"~u4db86333-6705-4e9c-8c4e-3a416e6a16f7\",\"^1\",\"Events\",\"^3\",true],[\"^ \",\"^0\",\"~udca6cb5f-62a6-455f-830f-a215b10e27a4\",\"^1\",\"Key bindings\",\"^3\",true]],\"^4\",true],[\"^ \",\"^0\",\"~u04076522-16d6-4541-b4db-af0f05d18a08\",\"^1\",\"BUG: undo fucks up state in a subtle way\",\"^2\",[[\"^ \",\"^0\",\"~ua2c57113-2e0d-4274-8279-6ad77f095c2c\",\"^1\",\"Make integration tests\",\"^3\",true],[\"^ \",\"^0\",\"~uc03f7d41-6591-4c28-a91f-b206375341b5\",\"^1\",\"Fix the undo\"]]],[\"^ \",\"^0\",\"~u11a458cd-6bc2-4c83-948a-8abe25aa6e31\",\"^1\",\"Attach creation / done time to todo items\"],[\"^ \",\"^0\",\"~u29405fa4-4719-44ad-ad53-bcbe8219f8fd\",\"^1\",\"Moving done tasks to top / hiding them\"],[\"^ \",\"^0\",\"~ude8e0fed-2ccb-40e8-8645-052825f31235\",\"^1\",\"Process the VTB's request\",\"^2\",[[\"^ \",\"^0\",\"~uabb9df22-47dd-403b-8cb8-96d4c9e121b2\",\"^1\",\"Send the management presentation to the VTB team\",\"^3\",false],[\"^ \",\"^0\",\"~u3c2eb948-dcf3-4ca0-8bff-e6f1d89704d5\",\"^1\",\"Check if counteragent information is in the VDR already\"],[\"^ \",\"^0\",\"~u521d5e5a-e146-46fb-a056-dfa14fcafb3c\",\"^1\",\"Put the rest of the questions in Excel\"],[\"^ \",\"^0\",\"~uf3ff5866-576d-4d4d-911e-e1c5c76bca49\",\"^1\",\"Questions 6 and 7 need to be discussed with Alexey\"],[\"^ \",\"^0\",\"~u1ad418e1-d7ac-4dba-b755-9958b932ff9c\",\"^1\",\"Send the questions to Marina\"]]],[\"^ \",\"^0\",\"~ue9e24499-d0dd-454b-97e3-215a719c344c\",\"^1\",\"Follow up with Igor Kuznetsov re: financing\"],[\"^ \",\"^0\",\"~uc10fe3e8-cea8-43dd-8ec4-a0b5bc255b9e\",\"^1\",\"Follow up with Alexey Kuznetsov re: Sber\"]]")
