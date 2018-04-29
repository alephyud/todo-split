(ns todo-split.models.todos.gen
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as cs]))

(s/def ::contact
  #{"Alex" "Bob" "Charlie" "Daphna" "Erica" "Farhad" "Gene" "Hillary"
    "Ian" "Jane" "support team" "IT" "the client" "Goldstein" "Morgan" "Google"
    "freelancer" "consultant" "E&Y" "PWC" "McKinsey team" "service center"})

(s/def ::product
  #{"presentation for next meeting" "the list of appointments"
    "list of invitees" "project budget" "project plan" "investment memorandum"
    "investor presentation" "business plan" "marketing materials" "NDA"
    "RfP" "the proposal" "market overview" "quarterly report" "speaker notes"
    "talking points"
    "kick-off presentation" "the white paper" "restaurant menu" "job description"
    "meeting agenda" "meeting minutes" "call agenda" "call minutes"})

(s/def ::appointment
  #{"meeting room" "conference room" "dial-in number" "plane tickets"})

(s/def ::contact-about-action
  #{"Remind" "Ask"})

(s/def ::simple-contact-action
  #{"Email" "Call" "Reply to" "Follow up with"})

(s/def ::contact-action
  (s/or :contact-about ::contact-about-action
        :simple-contact ::simple-contact-action))

(s/def ::product-action
  #{"Prepare" "Draft" "Send out" "Proofread" "Check" "Finalize"
    "Comment on" "Update"})

(s/def ::appointment-action
  #{"Book" "Check"})

(s/def ::task-seq
  (s/or :contact (s/cat :action ::simple-contact-action :recipient ::contact)
        :contact-about (s/cat :action ::contact-action :recipient ::contact
                              :about #{"re:" "about"}
                              :what ::product)
        :product (s/cat :action ::product-action :what ::product)
        :product-for-recipient (s/cat :action ::product-action
                                      :what #{"memo" "presentation"}
                                      :for #{"for"} :recipient ::contact)
        :appointment (s/cat :action ::appointment-action
                            :appointment ::appointment)))

(def task
  (gen/fmap #(cs/join " " %) (s/gen ::task-seq)))

