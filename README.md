# AI Testing Agent — v3.27.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, retries, artifacts, reporting, suites, history/analytics, security redaction, preflight validation, data-driven execution, structured AI test intelligence and deterministic failure intelligence.

## v3.27.0 — Phase 6 Failure Intelligence & Smart Retry Guidance

Phase 6 adds a safe failure-diagnosis layer that turns failed execution results into deterministic remediation guidance.

- **Failure classification:** assertion, timeout, authentication, network, locator, validation, server and unknown categories.
- **Retry guidance:** `RETRY`, `RETRY_WITH_BACKOFF`, `DO_NOT_RETRY` and `HEAL_LOCATOR`.
- **Deterministic:** classification does not require Ollama, so CI behavior remains predictable.
- **Safe retry model:** recommendations are bounded guidance, not an automatic infinite retry loop.
- **Locator healing boundary:** locator failures are identified as healing candidates, but the framework does not blindly mutate selectors.
- **AgentRunner integration:** `analyzeFailureIntelligence(...)` exposes the recommendation programmatically.
- **Regression coverage:** timeout, authentication and locator classification tests are included.

Example:

```java
ExecutionResult result = runner.execute(plan);
if (!result.passed()) {
    FailureIntelligence.Analysis analysis = runner.analyzeFailureIntelligence(result);
    System.out.println(analysis.category());
    System.out.println(analysis.recommendation());
    System.out.println(analysis.rationale());
}
```

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

Do not store production credentials in plans or datasets. Persisted diagnostics use the existing redaction pipeline. AI prompts use placeholders instead of invented secrets. Failure recommendations are bounded and never grant an AI provider unrestricted retry or selector-mutation control.

## Version history

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
