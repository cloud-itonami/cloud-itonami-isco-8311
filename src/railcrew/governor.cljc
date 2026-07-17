(ns railcrew.governor
  "RailCrewGovernor — the independent safety/traceability layer named
  in this repository's README/business-model.md, gating every
  rail-crew scheduling/logistics operation an advisor may propose.
  The governor never dispatches hardware itself, never directly
  finalizes a locomotive-movement/throttle/brake-control decision or
  a signal-override/stop-signal-departure decision (always a hard,
  permanent block — never overridable, never merely an escalation),
  and never coordinates a maintenance order above the assignment's
  registered cost ceiling without human sign-off. This actor
  coordinates ADMINISTRATIVE/LOGISTICS SCHEDULING ONLY — it never
  operates the locomotive. Modeled on cloud-itonami-isco-3313's
  accountingsupport.governor.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. driver provenance      — the locomotive engine driver must be
                                registered.
    2. route-license verified — the driver's route-license record
                                must be independently
                                verified/registered
                                (`:route-license-verified?` true)
                                before ANY action is proposed for that
                                driver.
    3. no-actuation           — proposal :effect must be :propose
                                (the governor never dispatches
                                hardware and never operates the
                                locomotive; it only gates what the
                                advisor may propose).
    4. closed op-allowlist    — proposal :op must be one of
                                `:log-service-record`,
                                `:schedule-crew-operation`,
                                `:flag-safety-concern` or
                                `:coordinate-maintenance-order`. The
                                allowlist NEVER includes any op that
                                directly finalizes a
                                locomotive-movement/throttle/
                                brake-control decision or a
                                signal-override/stop-signal-departure
                                decision — any such op is rejected
                                here as :op-not-allowed.
    5. scope-exclusion         — defense-in-depth against a proposal
                                (under an otherwise-allowed op) whose
                                free-text rationale/detail directly
                                describes finalizing a locomotive
                                movement, throttle/brake control,
                                signal override or stop-signal
                                departure. ALWAYS a hard, permanent
                                block, regardless of op, confidence or
                                anything else — this check exists
                                because an LLM advisor could otherwise
                                smuggle a forbidden action description
                                into an allowed op's rationale. The
                                term list is phrased as full
                                finalization/execution ACTIONS
                                ('initiate the locomotive movement',
                                'override the stop signal'), never
                                bare nouns ('throttle', 'signal'), so
                                it never accidentally matches inside
                                an advisor's own default rationale
                                text — see
                                `railcrew.advisor-test` /
                                `default-mock-advisor-proposals-never-self-trip-scope-exclusion`
                                for the dedicated regression test.
    6. assignment basis        — a proposal citing an :assignment-id
                                (`:log-service-record`,
                                `:schedule-crew-operation`,
                                `:coordinate-maintenance-order`) must
                                cite a REGISTERED assignment belonging
                                to this driver.
    7. service-log basis       — a `:log-service-record` proposal
                                must have `:service-log-attached?`
                                true before any service record can be
                                logged (logging a trip/mileage/
                                incident record without an attached
                                service log is a fabricated record,
                                not routine logging).

  ESCALATION invariants (:escalate? true, ALWAYS human sign-off —
  these are :high/:safety-critical regardless of confidence):
    8. :op :flag-safety-concern (a surfaced mechanical-defect/
                                signal-anomaly/fatigue concern always
                                requires human review — the advisor
                                never resolves it itself).
    9. maintenance orders above the assignment's registered
                                `:max-maintenance-cost` (no maintenance
                                procurement above the registered
                                ceiling without human sign-off).
   10. low confidence (< `confidence-floor`)."
  (:require [railcrew.store :as store]
            [clojure.string :as str]))

(def confidence-floor 0.6)

(def ^:private allowed-ops
  #{:log-service-record :schedule-crew-operation
    :flag-safety-concern :coordinate-maintenance-order})

(def ^:private ops-needing-assignment
  #{:log-service-record :schedule-crew-operation :coordinate-maintenance-order})

(def ^:private always-escalate-ops #{:flag-safety-concern})

;; Defense-in-depth: phrased as full finalization/execution ACTIONS,
;; never bare nouns, so they never accidentally match inside an
;; advisor's own default rationale text (e.g. "proposed
;; :log-service-record for driver driver-1" shares no substring with
;; any phrase below). See `railcrew.governor`'s docstring point 5 and
;; the dedicated self-trip regression test in `railcrew.advisor-test`.
(def ^:private scope-exclusion-terms
  ["initiate the locomotive movement"
   "finalize the locomotive movement"
   "engage the throttle to move the train"
   "release the brake to depart"
   "finalize the brake application"
   "override the stop signal"
   "depart against the stop signal"
   "authorize departure against a stop signal"
   "clear the track without signal authority"
   "override the signal to proceed"])

(defn- scope-excluded-text? [s]
  (let [low (str/lower-case (or s ""))]
    (boolean (some #(str/includes? low %) scope-exclusion-terms))))

(defn- scope-excluded? [proposal]
  (or (scope-excluded-text? (:rationale proposal))
      (scope-excluded-text? (:detail proposal))))

(defn- hard-violations [{:keys [request proposal]} driver-record a]
  (let [{:keys [op assignment-id service-log-attached?]} proposal
        needs-assignment? (contains? ops-needing-assignment op)
        log? (= :log-service-record op)]
    (cond-> []
      (nil? driver-record)
      (conj {:rule :no-driver :detail "未登録 driver"})

      (and driver-record (not (:route-license-verified? driver-record)))
      (conj {:rule :license-not-verified
             :detail "driver/route-license 記録が独立検証・登録される前は一切のアクションを許可しない"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は機関車運行を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :op-not-allowed
             :detail "closed op-allowlist にない operation は永久に禁止（機関車運行/スロットル・ブレーキ制御/信号越権の直接確定を含む）"})

      (scope-excluded? proposal)
      (conj {:rule :scope-exclusion
             :detail "機関車の運行・スロットル・ブレーキ制御の直接確定、信号越権、停止信号無視の発車確定は常に永久ハードブロック"})

      (and needs-assignment? (nil? assignment-id))
      (conj {:rule :missing-assignment :detail "assignment-id が未指定"})

      (and needs-assignment? assignment-id (nil? a))
      (conj {:rule :unknown-assignment :detail "未登録 assignment への提案は不可"})

      (and needs-assignment? a (not= (:driver-id a) (:driver-id request)))
      (conj {:rule :assignment-wrong-driver :detail "assignment が別 driver のもの"})

      (and log? (not service-log-attached?))
      (conj {:rule :missing-service-log
             :detail "サービスログ未添付の記録提案は捏造記録であって通常のロギング業務ではない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `railcrew.store/Store`. Pure — never mutates
  the store, never dispatches hardware, never operates the
  locomotive."
  [request context proposal store]
  (let [driver-record (store/driver store (:driver-id request))
        a (some->> (:assignment-id proposal) (store/assignment store))
        hard (hard-violations {:request request :proposal proposal}
                              driver-record a)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))
        over-cost? (and (= :coordinate-maintenance-order (:op proposal))
                        a
                        (number? (:estimated-cost proposal))
                        (> (:estimated-cost proposal) (:max-maintenance-cost a)))]
    {:ok? (and (not hard?) (not low?) (not always-risky?) (not over-cost?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky? over-cost?))}))
