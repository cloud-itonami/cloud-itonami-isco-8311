(ns railcrew.store
  "SSoT for the ISCO-08 8311 locomotive engine drivers actor (itonami
  actor pattern, ADR-2607121000 / CLAUDE.md Actors section; README's
  'Robotics premise' — a rail-crew scheduling and logistics
  coordination robot performs service-record logging, crew-roster
  proposals and maintenance-order coordination under this
  advisor/governor pair, which never dispatches hardware itself,
  never directly finalizes a locomotive-movement/throttle/
  brake-control decision or a signal-override/stop-signal-departure
  decision, and never coordinates a maintenance order above the
  assignment's registered cost ceiling without governor escalation).
  This actor coordinates ADMINISTRATIVE/LOGISTICS SCHEDULING ONLY — it
  never operates the locomotive. Modeled on cloud-itonami-isco-3313's
  accountingsupport.store.

  Domain:

    driver     — a registered locomotive engine driver {:driver-id
                 :name :route-license-verified? boolean}. The
                 driver/route-license record must be independently
                 verified/registered before any action is proposed
                 for that driver.
    assignment — a registered crew/route assignment {:assignment-id
                 :driver-id :name :max-maintenance-cost number}.
                 `:max-maintenance-cost` is the registered ceiling a
                 `:coordinate-maintenance-order` proposal's
                 `:estimated-cost` above which always escalates to
                 human sign-off (never a hard block — an over-ceiling
                 maintenance order is a legitimate business action
                 that just requires sign-off, unlike a locomotive
                 movement/signal-override decision, which is always a
                 hard, permanent block regardless of cost or
                 confidence).
    record     — a committed operating record (a logged service
                 record, crew schedule, safety flag or maintenance
                 order) — written ONLY via commit-record!.
    ledger     — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (driver [s driver-id])
  (assignment [s assignment-id])
  (records-of [s driver-id])
  (ledger [s])
  (register-driver! [s driver])
  (register-assignment! [s a])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (driver [_ driver-id] (get-in @a [:drivers driver-id]))
  (assignment [_ assignment-id] (get-in @a [:assignments assignment-id]))
  (records-of [_ driver-id] (filter #(= driver-id (:driver-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-driver! [s d]
    (swap! a assoc-in [:drivers (:driver-id d)] d) s)
  (register-assignment! [s asg]
    (swap! a assoc-in [:assignments (:assignment-id asg)] asg) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:drivers {} :assignments {} :records [] :ledger []}
                                   seed)))))
