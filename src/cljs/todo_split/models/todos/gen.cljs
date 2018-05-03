(ns todo-split.models.todos.gen
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as cs]))

(defn recursive-join [s]
  (if (seq? s)
    (cs/join " " (map recursive-join s))
    s))

(defn join-str-gen [seq-spec]
  (fn [] (gen/fmap recursive-join (s/gen seq-spec))))

(defn one-or-two [base-spec]
  (s/spec string? :gen
          #(gen/fmap (partial cs/join " and ")
                     (s/gen (s/coll-of base-spec :distinct true
                                       :min-count 1 :max-count 2)))))

(s/def ::contact
  #{"Alex" "Bob" "Chelsea" "Daphna" "Erica" "Farhad" "Gene" "Hans"
    "Ian" "Jane" "support team" "IT" "the client" "Reuters" "Citi" "Google"
    "Buzzfeed" "White&Case" "HR" "BoD" "CEO" "CFO" "COO" "IRS" "Amazon"
    "lawyers" "consultant" "E&Y" "PWC" "McKinsey team" "service center"
    "QA team" "designer" "PR team" "devs" "product manager" "manufacturer"
    "distributor" "frontend team" "help desk"})

(s/def ::simple-product
  #{"list of invitees" "project budget" "project plan" "investment memorandum"
    "investor presentation" "business plan" "marketing materials" "NDA"
    "RfP" "the proposal" "market overview" "speaker notes" "teaser"
    "talking points" "tech specs" "ToU" "privacy policy" "executive summary"
    "product description" "mission statement" "introductory video"
    "kick-off presentation" "the white paper" "job description"
    "meeting agenda" "meeting minutes" "call agenda" "call minutes"
    "financial model" "patent application" "compliance policy" "court filing"
    "compliance statement" "press release" "pitchbook" "PR kit" "post-mortem"
    "IFRS accounts" "costs breakdown" "revenue charts" "electricity bills"
    "product portfolio" "reserves report" "development schedule" "tax returns"
    "presentation outline" "launch announcement" "logo" "corporate slogan"
    "purchase agreement" "long-term agreement" "service agreement" "mandate"
    "stock quotes" "incentive program" "termination notice" "media kit"
    "layoff plan" "Chapter 7 filing" "Chapter 11 filing" "expense report"
    "value proposition" "go-to-market strategy" "patient records"
    "sales pipeline" "feasibility study" "key action points" "FEED"
    "risk assessment" "risks report" "onboarding policy"})

(s/def ::period
  #{"hourly" "daily" "weekly" "biweekly" "monthly" "quarterly" "half-year"
    "yearly" "annual"})

(s/def ::periodic-product-name
  #{"budget" "plan" "schedule" "report" "financials" "update" "drilling update"
    "sales update" "development update" "management accounts"})

(s/def ::periodic-product
  (s/spec string? :gen (join-str-gen (s/cat :period ::period
                                            :product ::periodic-product-name))))

(s/def ::product
  (s/or :simple ::simple-product
        :periodic ::periodic-product))

(s/def ::location
  #{"Atlanta" "Bahrain" "Chennai" "Dubai" "Exeter" "Fargo" "Geneva"
    "Haifa" "Ibiza" "Jakarta" "Kinshasa" "Lagos" "Macau" "Norilsk" "Oslo"
    "Perth" "Qingdao" "Rio" "Seoul" "Tokyo" "Utrecht" "Vancouver" "Xinjiang"
    "Yerevan" "Zaragoza"})

(s/def ::products (one-or-two ::product))

(s/def ::locations (one-or-two ::location))

(s/def ::contacts (one-or-two ::contact))

(s/def ::location-resource
  (s/spec string? :gen (join-str-gen (s/cat :resource #{"tickets to"
                                                        "meeting room in"
                                                        "meetings in"}
                                            :location ::locations))))

(s/def ::appointment-resource
  (s/or :static-resource #{"conference room" "dial-in number" "call details"}
        :location-resource ::location-resource))

(s/def ::contact-about-action
  #{"Remind" "Ask" "Talk to" "Reply to"})

(s/def ::simple-contact-action
  #{"Email" "Call" "Follow up with"})

(s/def ::contact-action
  (s/or :contact-about ::contact-about-action
        :simple-contact ::simple-contact-action))

(s/def ::product-action
  #{"Design" "Prepare" "Draft" "Send out" "Proofread" "Check" "Finalize"
    "Double-check" "Comment on" "Update" "Upload" "Download" "Rehearse"
    "Present"})

(s/def ::appointment-action
  #{"Book" "Follow up re:"})

(s/def ::task-seq
  (s/or :contact (s/cat :action ::simple-contact-action :recipient ::contacts)
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
