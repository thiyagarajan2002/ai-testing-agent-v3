# AI Testing Agent — v3.33.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, bounded retries, safe locator healing, healing history, retry history, artifacts, reporting, suites, execution history/analytics, trend analysis, security redaction, preflight validation, data-driven execution and structured AI test intelligence.

## v3.33.0 — Smart Retry v2

Phase 11 strengthens transient-failure retry behavior with explicit budgets and auditable retry history.

- **Category-aware retry:** retries are permitted only for `RETRY` and `RETRY_WITH_BACKOFF` recommendations.
- **No unsafe retries:** assertion, authentication, validation and locator-healing failures are not automatically retried by the retry policy.
- **Exponential backoff:** delays start at 250 ms and are capped at 4000 ms.
- **Retry budget:** every retry decision is bounded by the configured maximum retry count.
- **Retry history:** retry decisions and recovery outcomes are persisted under `reports/retries/history.json`.
- **Correlation:** retry history records run ID, test, action, retry number and failure category.
- **Security:** persisted rationale and identifiers pass through the existing redaction boundary.
- **Regression coverage:** retry-history persistence is covered by JUnit.

### Retry policy

`AdaptiveRetryPolicy` remains deterministic and bounded:

```text
retry 1 -> 250 ms
retry 2 -> 500 ms
retry 3 -> 1000 ms
retry 4 -> 2000 ms
retry 5+ -> 4000 ms maximum
```

## v3.32.0 — AI Self-Healing v2

Phase 10 strengthens UI locator healing with explicit safety boundaries, evidence and persistent audit history.

- **Confidence threshold:** only candidates meeting the default 0.90 confidence threshold are eligible.
- **Evidence capture:** each evaluated candidate records match count, visibility, confidence and reason.
- **Uniqueness gate:** a healed selector must resolve to exactly one visible element.
- **Conservative candidates:** healing remains limited to selectors deterministically derived from the original locator.
- **Healing history:** successful healing decisions are persisted under `reports/healing/history.json`.

## v3.31.0 — Report Comparison & Trend Analysis

Phase 9 extends execution history into a trend-oriented reporting layer with pass-rate, duration, regression and historical analytics trends.

## v3.30.0 — Execution History

Phase 8 formalizes persistent suite execution history, historical comparison and analytics.

## v3.29.0 — Run ID & Correlation ID

Phase 7 introduced unique Run IDs propagated through terminal logs and execution results, with test/step correlation fields.

## v3.28.0 — Phase 7 Self-Healing & Adaptive Execution

Phase 7 adds bounded adaptive retry behavior and conservative UI locator healing.
