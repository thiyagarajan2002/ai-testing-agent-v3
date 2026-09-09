# AI Testing Agent — v3.31.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, bounded retries, safe locator healing, artifacts, reporting, suites, execution history/analytics, trend analysis, security redaction, preflight validation, data-driven execution and structured AI test intelligence.

## v3.31.0 — Report Comparison & Trend Analysis

Phase 9 extends execution history into a trend-oriented reporting layer.

- **Pass-rate trends:** compare the first and latest analyzed runs in percentage points.
- **Duration trends:** track execution-duration delta and peak duration across runs.
- **Regression trends:** aggregate regressions across the selected history window.
- **Flaky-test trend:** calculate the cumulative number of tests meeting the existing 50% status-change threshold at each run; a single run is never treated as proof of flakiness.
- **Trend artifacts:** `reports/history/trends.json`, `trends.csv` and `trends.html` are generated from persisted history.
- **Report navigation:** trend rows link directly to each run's `comparison.html`; the trend dashboard links back to history and flaky/slow analytics.
- **CLI:** `history [--limit N]` now generates and prints the trend dashboard location.
- **Security:** generated trend text passes through the existing security redaction boundary.
- **Regression coverage:** trend calculations and artifact generation are covered by JUnit tests.

### Execution history and trend command

```text
mvn exec:java -Dexec.args="history"
mvn exec:java -Dexec.args="history --limit 20"
```

The command remains read-only with respect to existing run records; it only refreshes derived analytics/trend artifacts.

## v3.30.0 — Execution History

Phase 8 formalizes persistent suite execution history and exposes it from the CLI.

- **Persistent history:** suite executions are stored under `reports/history/<run-id>/`.
- **Run index:** `reports/history/index.json` keeps a compact chronological summary.
- **Run artifacts:** each recorded suite run contains `suite-execution.json`, `comparison.json`, `comparison.csv` and `comparison.html`.
- **Historical comparison:** the current run is compared with the previous recorded run to identify regressions, fixed tests, new tests, removed tests and unchanged results.
- **Analytics:** historical analytics identify flaky tests and slowest tests over a configurable number of runs.
- **Dashboards:** `index.html` provides history navigation and `analytics.html` provides historical analytics.

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
