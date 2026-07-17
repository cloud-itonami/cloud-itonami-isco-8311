(ns railcrew.advisor-test
  "Dedicated regression test for a known self-tripping bug pattern: a
  governor's scope-exclusion term list can accidentally match inside
  an advisor's own default rationale text (e.g. a bare noun like
  \"movement\" or \"signal\" appearing in an auto-generated rationale
  string), causing false self-blocking of routine, in-scope
  proposals. `railcrew.governor`'s scope-exclusion-terms are
  deliberately phrased as full finalization/execution ACTIONS
  (\"initiate the locomotive movement\", \"override the stop
  signal\"), never bare nouns (\"throttle\", \"signal\"),
  specifically to avoid this. This test asserts every op the
  mock-advisor can produce for a well-formed request never trips the
  governor's hard scope-exclusion rule."
  (:require [clojure.test :refer [deftest is testing]]
            [railcrew.store :as store]
            [railcrew.advisor :as advisor]
            [railcrew.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-driver! st {:driver-id "driver-1" :name "Kobo Rail"
                                :route-license-verified? true})
    (store/register-assignment! st {:assignment-id "A-1" :driver-id "driver-1"
                                    :name "chuo-line-shift-1"
                                    :max-maintenance-cost 5000})
    st))

(def ^:private requests
  [{:driver-id "driver-1" :op :log-service-record :assignment-id "A-1"
    :service-log-attached? true :stake :low}
   {:driver-id "driver-1" :op :schedule-crew-operation :assignment-id "A-1"
    :stake :low}
   {:driver-id "driver-1" :op :flag-safety-concern :stake :low}
   {:driver-id "driver-1" :op :coordinate-maintenance-order :assignment-id "A-1"
    :estimated-cost 1000 :stake :low}])

(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (testing "mock-advisor の既定 rationale テキストは governor の scope-exclusion を一度も誤爆させない"
    (doseq [request requests]
      (let [st (fresh-store)
            adv (advisor/mock-advisor)
            proposal (advisor/-advise adv st request)
            verdict (governor/check request {} proposal st)]
        (is (not (some #(= :scope-exclusion (:rule %)) (:violations verdict)))
            (str "op " (:op request) " unexpectedly tripped scope-exclusion with rationale: "
                 (:rationale proposal)))))))
