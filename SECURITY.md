# Security Policy

This project handles locomotive engine drivers operating workflows. Treat
vulnerabilities as potentially high impact even when the demo data is
synthetic — this domain has real physical-safety stakes (collision,
derailment).

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real driver or operator data exposure
- authorization bypass
- RailCrewGovernor bypass
- any path that could let a proposal reach a locomotive-movement/throttle/
  brake-control decision, a track-clearance/signal-override decision, or a
  decision to depart against a stop signal
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch

## Reporting

Use GitHub private vulnerability reporting when available for the
repository. If that is unavailable, contact the repository maintainers
through the cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on driver data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real driver/operator data outside this repository.
- Run policy tests before deployment, including scope-exclusion regression
  tests.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
- Never wire this actor's proposals into a system that could directly
  execute a locomotive-movement, throttle/brake-control, signal-override or
  stop-signal-departure decision — this actor coordinates ADMINISTRATIVE/
  LOGISTICS SCHEDULING ONLY.
