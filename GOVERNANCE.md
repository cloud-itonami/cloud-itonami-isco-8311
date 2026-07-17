# Governance

`cloud-itonami-isco-8311` is an OSS open-occupation blueprint. Governance
covers both code and the operator model.

## Maintainers

Maintainers may merge changes that preserve these invariants:

- the Advisor cannot directly dispatch robot actions, operate the
  locomotive, or disclose records.
- RailCrewGovernor remains independent of the advisor.
- hard policy violations cannot be overridden by human approval —
  including, permanently, any proposal that directly finalizes a
  locomotive-movement/throttle/brake-control decision, a track-clearance/
  signal-override decision, or a decision to depart against a stop signal.
- every commit, hold and approval path is auditable.
- real driver/operator data stays outside Git.

## Decision Records

Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance

Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit, support and
data-flow review.

Certified operators can lose certification for:

- bypassing policy checks
- mishandling driver/operator data
- misrepresenting certification status
- failing to respond to security incidents
- hiding material changes to customer-facing operation
- attempting to route a locomotive-movement, throttle/brake-control,
  signal-override or stop-signal-departure decision around the governor
