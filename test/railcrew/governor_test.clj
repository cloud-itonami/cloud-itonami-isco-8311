(ns railcrew.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [railcrew.store :as store]
            [railcrew.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-driver! st {:driver-id "driver-1" :name "Kobo Rail"
                                :route-license-verified? true})
    (store/register-assignment! st {:assignment-id "A-1" :driver-id "driver-1"
                                    :name "chuo-line-shift-1"
                                    :max-maintenance-cost 5000})
    st))

(defn- log-op [attached?]
  {:op :log-service-record :effect :propose :assignment-id "A-1"
   :service-log-attached? attached? :confidence 0.9 :stake :low})

(def ^:private req {:driver-id "driver-1"})

(deftest ok-service-record-with-log-attached
  (let [st (fresh-store)
        v (governor/check req {} (log-op true) st)]
    (is (:ok? v))))

(deftest hard-on-missing-service-log
  (testing "サービスログ未添付の記録提案は捏造記録であって通常のロギング業務ではない"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op false) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :missing-service-log (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-driver
  (let [st (fresh-store)
        v (governor/check {:driver-id "nobody"} {} (log-op true) st)]
    (is (:hard? v))
    (is (some #(= :no-driver (:rule %)) (:violations v)))))

(deftest hard-on-unverified-license
  (testing "driver/route-license 記録が独立検証・登録される前は一切のアクションを許可しない"
    (let [st (fresh-store)]
      (store/register-driver! st {:driver-id "driver-2" :name "Unverified"
                                  :route-license-verified? false})
      (store/register-assignment! st {:assignment-id "A-2" :driver-id "driver-2"
                                      :name "unverified-shift"
                                      :max-maintenance-cost 5000})
      (let [v (governor/check {:driver-id "driver-2"} {}
                                (assoc (log-op true) :assignment-id "A-2") st)]
        (is (:hard? v))
        (is (some #(= :license-not-verified (:rule %)) (:violations v)))))))

(deftest hard-on-unknown-assignment
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :assignment-id "A-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-assignment (:rule %)) (:violations v)))))

(deftest hard-on-foreign-assignment
  (let [st (fresh-store)]
    (store/register-driver! st {:driver-id "driver-2" :name "Other"
                                :route-license-verified? true})
    (let [v (governor/check {:driver-id "driver-2"} {} (log-op true) st)]
      (is (:hard? v))
      (is (some #(= :assignment-wrong-driver (:rule %)) (:violations v))))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-not-in-allowlist
  (testing "closed op-allowlist は機関車運行/信号越権の直接確定 op を一切含まない"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op true) :op :operate-locomotive) st)]
      (is (:hard? v))
      (is (some #(= :op-not-allowed (:rule %)) (:violations v))))))

(deftest hard-permanent-block-on-locomotive-movement-scope-exclusion
  (testing "機関車運行/スロットル・ブレーキ制御の直接確定は常に永久ハードブロック（confidence に関わらず override 不可）"
    (let [st (fresh-store)
          v (governor/check req {}
                              (assoc (log-op true)
                                     :confidence 0.99
                                     :rationale "initiate the locomotive movement now")
                              st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

(deftest hard-permanent-block-on-stop-signal-departure-scope-exclusion
  (testing "停止信号に反した発車の直接確定は常に永久ハードブロック"
    (let [st (fresh-store)
          v (governor/check req {}
                              (assoc (log-op true)
                                     :confidence 0.99
                                     :rationale "depart against the stop signal to save time")
                              st)]
      (is (:hard? v))
      (is (some #(= :scope-exclusion (:rule %)) (:violations v))))))

(deftest hard-permanent-block-on-signal-override-scope-exclusion-cannot-be-escalated
  (testing "scope-exclusion は confidence が高くても escalate ではなく常に hard block"
    (let [st (fresh-store)
          v (governor/check req {}
                              (assoc (log-op true)
                                     :confidence 1.0
                                     :rationale "override the stop signal to proceed")
                              st)]
      (is (:hard? v))
      (is (not (:escalate? v))))))

(deftest always-escalates-flag-safety-concern-even-at-high-confidence
  (testing "安全上の懸念のフラグは常に人間の承認を要する"
    (let [st (fresh-store)
          v (governor/check req {} {:op :flag-safety-concern :effect :propose
                                    :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-maintenance-order-above-cost-ceiling
  (testing "登録済み上限を超える保守発注は常に人間の承認を要する"
    (let [st (fresh-store)
          v (governor/check req {} {:op :coordinate-maintenance-order :effect :propose
                                    :assignment-id "A-1" :estimated-cost 50000
                                    :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest ok-maintenance-order-within-cost-ceiling
  (let [st (fresh-store)
        v (governor/check req {} {:op :coordinate-maintenance-order :effect :propose
                                  :assignment-id "A-1" :estimated-cost 2000
                                  :confidence 0.9 :stake :low} st)]
    (is (:ok? v))))

(deftest ok-maintenance-order-at-exact-cost-ceiling-boundary
  (testing "cost ceiling は inclusive"
    (let [st (fresh-store)
          v (governor/check req {} {:op :coordinate-maintenance-order :effect :propose
                                    :assignment-id "A-1" :estimated-cost 5000
                                    :confidence 0.9 :stake :low} st)]
      (is (:ok? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
