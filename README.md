# cloud-itonami-isco-8311

Open Occupation Blueprint for **ISCO-08 8311**: Locomotive Engine Drivers.

This repository designs a forkable OSS business for locomotive-crew
scheduling and maintenance coordination: a rail-crew scheduling and
logistics coordination robot manages service-record logging, crew-roster
scheduling and maintenance-order coordination under a governor-gated
actor, so a depot keeps its own operating records instead of renting a
closed crew-scheduling SaaS.

**This actor coordinates ADMINISTRATIVE/LOGISTICS SCHEDULING ONLY — it
never operates the locomotive.** Locomotive Engine Drivers directly
operate trains — a domain with real physical-safety stakes (collision,
derailment). No proposal this actor can produce may ever directly finalize
a locomotive-movement/throttle/brake-control decision, a track-clearance/
signal-override decision, or a decision to depart against a stop signal:
these are always a hard, permanent block, never overridable and never
merely an escalation.

**Maturity: `:implemented`.** `src/railcrew/` implements the
`RailCrewActor` as a `langgraph.graph/state-graph` (`railcrew.actor`)
wired to a `Rail Crew Advisor` (`railcrew.advisor`) and an independent
`RailCrewGovernor` (`railcrew.governor`), following the itonami actor
pattern (ADR-2607121000): `:intake -> :advise -> :govern -> :decide -+->
:commit (:ok?) +-> :request-approval (:escalate?, human-in-the-loop
interrupt) +-> :hold (:hard?)`. 22 tests / 48 assertions green
(`clojure -M:test`).

HARD invariants (always hold, never overridable): driver provenance, the
driver's route-license record must be independently verified/registered
before any action, no-actuation (`:effect` must be `:propose`), a closed
op-allowlist that never contains a locomotive-movement/throttle/
brake-control or signal-override/stop-signal-departure op, a dedicated
scope-exclusion rule that hard-blocks any proposal whose rationale text
directly describes finalizing such a decision (defense-in-depth against an
advisor smuggling a forbidden action into an allowed op — see
`test/railcrew/advisor_test.cljk` for the dedicated regression test proving
the mock-advisor's own default rationale text never self-trips this rule),
a registered assignment basis for any proposal citing one, and an attached
service log before any service record can be logged (logging without one
is a fabricated record, not routine logging).

Always-escalate (human sign-off regardless of confidence, mapping this
repo's Trust Controls in [`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (a surfaced mechanical-defect/signal-anomaly/
fatigue concern always requires human review) and maintenance orders whose
`:estimated-cost` exceeds the assignment's registered
`:max-maintenance-cost`.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical/administrative domain work**. Here a rail-crew
scheduling and logistics coordination robot performs service-record
logging, crew-roster proposals and maintenance-order coordination under
an actor that proposes actions and an independent **RailCrewGovernor**
that gates them. The governor never dispatches hardware itself and never
operates the locomotive; `:high`/`:safety-critical` actions (safety
concerns, over-ceiling maintenance orders) require human sign-off, and
any proposal to directly finalize a locomotive-movement/throttle/
brake-control or signal-override/stop-signal-departure decision is a
hard, permanent block regardless of confidence.

## Core Contract

```text
trip log + crew roster + maintenance backlog
        |
        v
Rail Crew Advisor -> RailCrewGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
suppress an operating record, finalize a locomotive-movement/throttle/
brake-control or signal-override/stop-signal-departure decision, or
disclose sensitive data without governor approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8311`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
