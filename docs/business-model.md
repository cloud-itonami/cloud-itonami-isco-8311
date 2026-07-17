# Business Model: Locomotive Crew Scheduling & Maintenance Coordination Service

## Classification

- Repository: `cloud-itonami-isco-8311`
- ISCO-08: `8311`
- Occupation: Locomotive Engine Drivers
- Social impact: rail-workforce-support, transit-safety-compliance, logistics-efficiency

## Customer

- rail operators
- freight and passenger transit authorities
- independent locomotive-crew depots

## Offer

- service-record logging (trip/mileage/incident-report data)
- driver-roster and shift scheduling
- safety-concern surfacing (mechanical defect, signal anomaly, fatigue)
- locomotive maintenance-order coordination

## Revenue

- monthly retainer per depot
- per-crew scheduling fee

## Trust Controls

- this actor coordinates ADMINISTRATIVE/LOGISTICS SCHEDULING ONLY — it never
  operates the locomotive
- no proposal may ever directly finalize a locomotive-movement/throttle/
  brake-control decision, a track-clearance/signal-override decision, or a
  decision to depart against a stop signal — always a hard, permanent block,
  never overridable and never merely an escalation
- a driver's route-license record must be independently verified and
  registered before any action is proposed for that driver
- safety concerns always escalate to human review; the advisor never
  resolves them itself
- maintenance orders above a depot's registered cost ceiling always require
  human sign-off
- operating and scheduling records are auditable, not editable
