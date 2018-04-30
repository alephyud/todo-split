(ns todo-split.models.todos.gen
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as cs]))

(s/def ::contact
  #{"Alex" "Bob" "Chelsea" "Daphna" "Erica" "Farhad" "Gene" "Harriet"
    "Ian" "Jane" "support team" "IT" "the client" "Goldstein" "Morgan" "Google"
    "Buzzfeed" "White&Case" "HR" "BoD" "CEO" "CFO" "COO" "IRS"
    "lawyers" "consultant" "E&Y" "PWC" "McKinsey team" "service center"
    "QA team" "designer" "PR team" "devs" "product manager"})

(s/def ::simple-product
  #{"list of invitees" "project budget" "project plan" "investment memorandum"
    "investor presentation" "business plan" "marketing materials" "NDA"
    "RfP" "the proposal" "market overview" "speaker notes"
    "talking points" "tech specs" "ToU" "privacy policy" "executive summary"
    "product description" "mission statement" "introductory video"
    "kick-off presentation" "the white paper" "restaurant menu" "job description"
    "meeting agenda" "meeting minutes" "call agenda" "call minutes"
    "financial model" "patent application" "compliance policy"
    "compliance statement" "press release" "pitchbook" "PR kit" "post-mortem"
    "IFRS accounts" "costs breakdown" "revenue charts" "electricity bills"
    "product portfolio" "reserves report" "development schedule" "tax returns"
    "presentation outline" "launch announcement"})

(s/def ::period
  #{"hourly" "daily" "weekly" "biweekly" "monthly" "quarterly" "half-year"
    "yearly" "annual"})

(s/def ::periodic-product-name
  #{"budget" "plan" "schedule" "report" "financials" "update"
    "sales update" "development update" "management accounts"})

(defn join-str-gen [seq-spec]
  (fn [] (gen/fmap (partial cs/join " ") (s/gen seq-spec))))

(s/def ::periodic-product
  (s/spec string? :gen (join-str-gen (s/cat :period ::period
                                            :product ::periodic-product-name))))

(s/def ::product
  (s/or :simple ::simple-product
        :periodic ::periodic-product))

(s/def ::two-products
  (s/spec string? :gen (join-str-gen
                        (s/and (s/cat :product-1 ::product
                                      :and #{"and"}
                                      :product-2 ::product)
                               (s/coll-of any? :distinct true)))))

(s/def ::products
  (s/or :one ::product :two ::two-products))

(s/def ::appointment-resource
  #{"meeting room" "conference room" "dial-in number" "plane tickets"})

(s/def ::contact-about-action
  #{"Remind" "Ask" "Talk to" "Reply to"})

(s/def ::simple-contact-action
  #{"Email" "Call" "Follow up with"})

(s/def ::contact-action
  (s/or :contact-about ::contact-about-action
        :simple-contact ::simple-contact-action))

(s/def ::product-action
  #{"Design" "Prepare" "Draft" "Send out" "Proofread" "Check" "Finalize"
    "Comment on" "Update" "Upload" "Download" "Rehearse" "Present"})

(s/def ::appointment-action
  #{"Book" "Check"})

(s/def ::task-seq
  (s/or :contact (s/cat :action ::simple-contact-action :recipient ::contact)
        :contact-about (s/cat :action ::contact-about-action
                              :recipient ::contact
                              :about #{"re:" "about"}
                              :what ::products)
        :discuss (s/cat :action #{"Discuss"} :what ::products
                        :with #{"with"} :whom ::contact)
        :product (s/cat :action ::product-action :what ::product)
        :product-for-recipient (s/cat :action ::product-action
                                      :what #{"memo" "presentation" "report"}
                                      :for #{"for"} :recipient ::contact)
        :appointment (s/cat :action ::appointment-action
                            :what ::appointment-resource)))

(def task (join-str-gen ::task-seq))
