(ns railcrew.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [railcrew.actor :as actor]
            [railcrew.advisor :as advisor]
            [railcrew.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-driver! st {:driver-id "driver-1" :name "Kobo Rail"
                                :route-license-verified? true})
    (store/register-assignment! st {:assignment-id "A-1" :driver-id "driver-1"
                                    :name "chuo-line-shift-1"
                                    :max-maintenance-cost 5000})
    st))

(deftest commits-a-service-record-with-log-attached
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:driver-id "driver-1" :op :log-service-record :stake :low
                 :assignment-id "A-1" :service-log-attached? true}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "driver-1"))))))

(deftest holds-a-service-record-without-log-attached
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:driver-id "driver-1" :op :log-service-record :stake :low
                 :assignment-id "A-1" :service-log-attached? false}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "driver-1")))))

(deftest holds-an-attempt-to-directly-finalize-locomotive-movement
  (testing "機関車運行を直接確定しようとする提案は、advisor がどう主張しようと commit に到達せず常に hold される"
    (let [st (fresh-store)
          request {:driver-id "driver-1" :op :log-service-record :stake :low
                   :assignment-id "A-1" :service-log-attached? true}
          ;; a broken/malicious advisor that smuggles a forbidden
          ;; finalization phrase into the rationale of an otherwise
          ;; allowed op, to prove the governor's scope-exclusion rule
          ;; — not just the advisor's own restraint — is what blocks
          ;; it.
          smuggling-advisor
          (reify advisor/Advisor
            (-advise [_ _store req]
              {:op (:op req) :effect :propose
               :assignment-id (:assignment-id req)
               :service-log-attached? true :confidence 0.99 :stake :low
               :rationale "initiate the locomotive movement to clear the yard"}))
          graph (actor/build-graph {:store st :advisor smuggling-advisor})
          result (actor/run-request! graph request {} "thread-3")]
      (is (= :hold (:disposition (:state result))))
      (is (empty? (store/records-of st "driver-1"))))))

(deftest interrupts-then-approves-flag-safety-concern-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:driver-id "driver-1" :op :flag-safety-concern :stake :low}
        interrupted (actor/run-request! graph request {} "thread-4")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "driver-1")))
    (let [resumed (actor/approve! graph "thread-4")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "driver-1")))))))

(deftest interrupts-then-approves-over-ceiling-maintenance-order-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:driver-id "driver-1" :op :coordinate-maintenance-order :stake :low
                 :assignment-id "A-1" :estimated-cost 50000}
        interrupted (actor/run-request! graph request {} "thread-5")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "driver-1")))
    (let [resumed (actor/approve! graph "thread-5")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "driver-1")))))))
