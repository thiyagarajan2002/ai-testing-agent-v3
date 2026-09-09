# AI Testing Agent — v3.28.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, bounded retries, safe locator healing, artifacts, reporting, suites, history/analytics, security redaction, preflight validation, data-driven execution and structured AI test intelligence.

## v3.28.0 — Phase 7 Self-Healing & Adaptive Execution

Phase 7 adds bounded adaptive retry behavior and conservative UI locator healing on top of deterministic failure intelligence.

- **Adaptive retry policy:** deterministic exponential backoff with a hard delay cap.
- **Retry safety:** only `RETRY` and `RETRY_WITH_BACKOFF` recommendations are eligible for automatic retry; assertion/authentication/validation failures remain non-retryable.
- **Retry budget:** callers must provide a finite retry count; no infinite retry loop is introduced.
- **Locator healing:** failed locator actions may try a single safe alternative derived from the original selector.
- **Confidence scoring:** healing candidates carry a confidence score and rationale.
- **Uniqueness gate:** a candidate is accepted only when Playwright finds exactly one visible element.
- **Healing audit trail:** successful healing records the original locator, healed locator, confidence and reason in the execution step details.
- **Security boundary:** the engine does not send DOM data or credentials to an AI provider and does not blindly mutate test plans.
- **Regression coverage:** retry budget/backoff and conservative locator-candidate tests are included.

The Phase 7 healing implementation is intentionally conservative. It currently derives alternatives for stable `id`, `data-testid` and `name` selector relationships. Broad XPath/text guessing is deliberately excluded.

## v3.27.0 — Phase 6 Failure Intelligence & Smart Retry Guidance

Phase 6 adds deterministic failure classification for assertion, timeout, authentication, network, locator, validation, server and unknown failures, with bounded retry/healing recommendations.

## v3.26.0 — Phase 5 AI Testing Intelligence

Phase 5 adds requirement summaries, multiple positive/negative/boundary/authentication/validation/resilience scenarios, priorities, executable plans, requirement coverage, missing-test detection, duplicate groups and provider abstraction.

## v3.25.0 — Phase 4 UI Testing Enhancement

Phase 4 provides Playwright lifecycle handling, DOMContentLoaded navigation, locator actions, event-oriented waits, assertions, screenshots and failure metadata.

## v3.24.0 — Phase 3 API Testing Enhancement

API execution supports GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS, request substitution, forms, authentication, JSONPath/XMLPath assertions, response extraction and retries.

## v3.23.0 — Phase 2 Reporting Standardization

Every execution report contains exactly `report.html`, `report.csv` and `report.pdf`, with integrity validation.

## v3.22.0 — Phase 1 Stability & Security

Configuration bounds, standardized errors, durable secure logging, UTF-8 redaction and corrected dataset validation.

## Core commands

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file> --parallelism 3"
mvn exec:java -Dexec.args="validate plan <file>"
mvn exec:java -Dexec.args="interactive"
```

## Documentation

- `PROJECT_DETAILS.md` — implementation and method-level behavior.
- `docs/architecture/` — architecture and package boundaries.
- `docs/configuration/` — configuration guidance.
- `docs/testing/` — testing standards.
- `docs/releases/` — release history.
- `examples/README.md` — examples and evidence guide.

## Security

Do not store production credentials in plans or datasets. Persisted diagnostics use the existing redaction pipeline. AI prompts use placeholders instead of invented secrets. Retry and healing behavior is bounded, deterministic and cannot grant an AI provider unrestricted execution control.

## Version history

- **v3.28.0** — Phase 7 self-healing and adaptive execution
- **v3.27.0** — Phase 6 failure intelligence and smart retry/healing guidance
- **v3.26.0** — Phase 5 AI testing intelligence
- **v3.25.0** — Phase 4 UI testing enhancement
- **v3.24.0** — Phase 3 API testing enhancement
- **v3.23.0** — Phase 2 reporting standardization and integrity validation
- **v3.22.0** — Phase 1 stability, configuration bounds, standardized errors and secure logging
- v3.21.0 — API/UI examples, screenshots, execution logs and unified reports
- v3.20.0 — repository organization and engineering standards

## Repository

https://github.com/thiyagarajan2002/ai-testing-agent-v3
