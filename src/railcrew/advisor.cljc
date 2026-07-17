(ns railcrew.advisor
  "Rail Crew Advisor — the advisor named in this repository's README,
  proposing a rail-crew scheduling/logistics operation (log a service
  record, propose a crew/roster schedule, flag a safety concern,
  coordinate a maintenance order) from a trip log, crew roster and
  maintenance backlog. Swappable mock/llm; the advisor ONLY proposes
  — `railcrew.governor` checks driver/route-license verification, the
  closed op-allowlist and the locomotive-operation scope-exclusion
  independently, and always escalates safety-concern flags and
  over-threshold maintenance orders. This actor coordinates
  ADMINISTRATIVE/LOGISTICS SCHEDULING ONLY — it never proposes to
  operate the locomotive. Modeled on cloud-itonami-isco-3313's
  accountingsupport.advisor.

  A proposal: {:op :log-service-record|:schedule-crew-operation|:flag-safety-concern|:coordinate-maintenance-order
               :effect :propose :driver-id str :assignment-id str
               :service-log-attached? boolean :estimated-cost number
               :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake driver-id assignment-id
                              service-log-attached? estimated-cost]
                       :as request}]
  {:op op
   :effect :propose
   :driver-id driver-id
   :assignment-id assignment-id
   :service-log-attached? (boolean service-log-attached?)
   :estimated-cost estimated-cost
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for driver " (:driver-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a rail-crew scheduling and logistics advisor for locomotive
   engine drivers. Given a request, propose an :op (one of
   :log-service-record, :schedule-crew-operation, :flag-safety-concern
   or :coordinate-maintenance-order), the :driver-id, :assignment-id,
   an honest :confidence and a :stake. You coordinate administrative
   and logistics scheduling ONLY — never propose to operate the
   locomotive, and never propose to finalize a movement, throttle,
   brake, signal-override or stop-signal-departure decision; those are
   always out of scope for this actor and are hard-blocked regardless
   of confidence, by op name and by rationale text alike. Always flag
   safety concerns rather than resolve them yourself — the governor
   always escalates them to a human. Always defer to the governor's
   independent check for maintenance orders above the assignment's
   registered cost ceiling.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
