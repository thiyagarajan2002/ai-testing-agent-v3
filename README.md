# AI Testing Agent — v3.30.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, bounded retries, safe locator healing, artifacts, reporting, suites, execution history/analytics, security redaction, preflight validation, data-driven execution and structured AI test intelligence.

## v3.30.0 — Execution History

Phase 8 formalizes persistent suite execution history and exposes it from the CLI.

- **Persistent history:** suite executions are stored under `reports/history/<run-id>/`.
- **Run index:** `reports/history/index.json` keeps a compact chronological summary.
- **Run artifacts:** each recorded suite run contains `suite-execution.json`, `comparison.json`, `comparison.csv` and `comparison.html`.
- **Historical comparison:** the current run is compared with the previous recorded run to identify regressions, fixed tests, new tests, removed tests and unchanged results.
- **Analytics:** historical analytics identify flaky tests and slowest tests over a configurable number of runs.
- **Dashboards:** `index.html` provides history navigation and `analytics.html` provides historical analytics.
- **CLI:** `history [--limit N]` prints recent runs and analytics locations.
- **Security:** history and generated comparison text continue to pass through the existing security redaction boundary.
- **Regression coverage:** history recording and regression/fixed-test comparison are covered by JUnit tests.

### Execution history command

```text
mvn exec:java -Dexec.args="history"
mvn exec:java -Dexec.args="history --limit 20"
```

The history command is read-only; it does not alter previously recorded runs.

## v3.29.0 — Run ID & Correlation ID

Phase 7 introduced a unique Run ID for every CLI execution, propagated through terminal logs and execution results, with test/step correlation fields.

## v3.28.0 — Phase 7 Self-Healing & Adaptive Execution

Phase 7 adds bounded adaptive retry behavior and conservative UI locator healing.

- **Adaptive retry policy:** deterministic exponential backoff with a hard delay cap.
- **Retry safety:** only transient retry recommendations are eligible for automatic retry.
- **Locator healing:** failed locator actions may try a safe alternative derived from the original selector.
- **Confidence scoring:** healing candidates carry confidence and rationale.
- **Uniqueness gate:** a candidate is accepted only when Playwright finds exactly one visible element.

## v3.27.0 — Phase 6 Failure Intelligence & Smart Retry Guidance

Phase 6 adds deterministic failure classification for assertion, timeout, authentication, network, locator, validation, server and unknown failures.

## v3.26.0 — Phase 5 AI Testing Intelligence

Phase 5 adds requirement summaries, positive/negative/boundary/authentication/validation/resilience scenarios, priorities, executable plans, requirement coverage, missing-test detection, duplicate groups and provider abstraction.
