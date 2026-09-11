# Contributing

`cloud-itonami-isco-8311` accepts contributions to the OSS actor, policy
tests, documentation, examples and open occupation blueprint.

## Development

```bash
clojure -M:dev:test
clojure -M:lint
```

Keep changes small and include tests for policy, audit, store or disclosure
behavior.

## Rules

- Do not commit real driver, crew or operator data, credentials or
  operating documents.
- Keep production writes and disclosures behind RailCrewGovernor.
- Never add an op to the closed op-allowlist that could directly finalize a
  locomotive-movement/throttle/brake-control decision, a track-clearance/
  signal-override decision, or a decision to depart against a stop signal.
  This actor coordinates ADMINISTRATIVE/LOGISTICS SCHEDULING ONLY.
- Treat this occupation's workflows as high-risk: add tests for permission,
  purpose, safety and audit logging, including scope-exclusion regression
  tests (see `test/railcrew/advisor_test.cljk`).
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
