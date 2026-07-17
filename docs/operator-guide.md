# Operator Guide

## First Deployment

1. Define the depot's crew-scheduling and maintenance-coordination scope.
2. Register every locomotive engine driver's route-license verification
   record before enabling any proposal for that driver.
3. Run synthetic operating cases (service-record logging, crew scheduling,
   safety-concern flags, maintenance-order coordination).
4. Enable human-reviewed sign-off for `:high`/`:safety-critical` actions —
   safety-concern flags and over-ceiling maintenance orders always require
   it, with no exceptions.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- consent and disclosure log
- safety-critical escalation path
- provenance for all operating records
- human review for high-risk cases (safety concerns, over-ceiling
  maintenance orders)
- audit export for all gated actions
- confirmation that the closed op-allowlist never contains a
  locomotive-movement/throttle/brake-control or signal-override/
  stop-signal-departure op, and that the scope-exclusion rule is exercised
  in pre-production tests

## Scope Boundary

This actor coordinates ADMINISTRATIVE/LOGISTICS SCHEDULING ONLY. It never
operates the locomotive. Any deployment that wires this actor's proposals
into a system capable of directly finalizing a locomotive-movement/
throttle/brake-control decision, a track-clearance/signal-override
decision, or a decision to depart against a stop signal is out of scope
and unsupported — the governor's hard, permanent block on those decisions
is a design invariant, not a configurable policy.

## Certification

Certified operators must prove that the governor gates every safety-critical
robot action, that safety-critical risks escalate to humans, and that no
configuration can route a locomotive-movement, throttle/brake-control,
signal-override or stop-signal-departure decision around the governor.
