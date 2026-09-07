# AI Testing Agent — v3.12.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.12.0 — Historical Analytics & Flaky-Test Detection

Version 3.12 extends v3.11 execution history with historical analytics, flaky-test detection, and a dedicated analytics dashboard.

### New capabilities
- Analyzes the most recent 20 recorded suite runs by default.
- Tracks each stable test by `planFile + testName`.
- Calculates executions, passes, failures, status changes, and average duration.
- Detects flaky tests when a test has both PASS and FAIL observations and its status-change rate meets the configured threshold.
- Default flaky threshold is `0.50` (50% status changes between consecutive observations).
- Lists the top 10 slowest tests by average execution duration.
- Writes `reports/history/analytics.json`, `analytics.csv`, and `analytics.html`.
- Keeps the existing `index.html` history dashboard and per-run comparison pages.
- Analytics/reporting failures remain isolated from the actual suite pass/fail result.

Analytics layout:

```text
reports/history/
├── index.json
├── index.html
├── analytics.json
├── analytics.csv
├── analytics.html
└── <run-id>/
    ├── suite-execution.json
    ├── comparison.json
    ├── comparison.csv
    └── comparison.html
```

### Flaky detection formula

For a test observed in `N` runs:

`flakinessRate = statusChanges / (N - 1)`

A test is reported as flaky when:
1. It has at least two observations.
2. It has at least one PASS and one FAIL.
3. Its flakiness rate is at least the configured threshold.

Example: `PASS → FAIL → PASS → FAIL` produces 3 status changes across 3 transitions, so the flakiness rate is 100%.

### Analytics output

`analytics.html` contains:
- Runs analyzed
- Number of unique tests observed
- Flaky test table
- Slowest test table
- Links to JSON/CSV/history dashboards

`analytics.json` is intended for automation and future dashboard integrations. `analytics.csv` is suitable for spreadsheet/BI analysis.

## v3.11.0 — Test Execution History & Run Comparison

Every suite execution is snapshotted under `reports/history/<run-id>/`. Stable comparison identity uses `planFile + testName`. Categories are `REGRESSION`, `FIXED`, `PASSED_UNCHANGED`, `FAILED_UNCHANGED`, `NEW_TEST`, and `REMOVED_TEST`. Pass-rate and duration deltas are tracked and written to JSON/CSV/HTML comparison reports.

## v3.10.0 — Secure Secrets & Sensitive Data Redaction

Centralized `SecurityRedactor` protection masks passwords, secrets, tokens, API keys, client secrets, authorization values, cookies, common auth headers, and sensitive environment-variable values before logs, reports, failure artifacts, or AI failure-analysis prompts are persisted/transmitted.

## v3.9.0 — Advanced Assertions & Validation Diagnostics

Supported API assertion types:

| Type | Purpose |
|---|---|
| `status` | Validate HTTP status |
| `bodyContains` | Body contains text |
| `bodyNotContains` | Body excludes text |
| `bodyRegex` | Body regex validation |
| `headerEquals` | Response header equality |
| `jsonPathExists` | JSONPath presence |
| `jsonPathEquals` | JSONPath equality |
| `jsonPathContains` | JSONPath contains text |
| `jsonPathRegex` | JSONPath regex validation |
| `responseTimeMs` | Maximum response time |

Legacy `assertSpec` remains supported.

## v3.8.0 — Test Data Management & Environment Profiles

Profiles live under `config/environments/` and can provide base URLs, headers, variables, timeouts, and JSON test data. Supported placeholders are `${profile.X}`, `${data.X}`, and `${env.X}`.

Example commands:

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json --env qa"
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json --env qa"
```

## v3.7.0 — CI/CD & GitHub Actions

`.github/workflows/ci.yml` runs for pushes/pull requests to `main` and manual dispatch. It uses Java 21 Temurin and executes `mvn -B --no-transfer-progress clean verify`, uploading `reports/` when present.

Playwright browser installation is intentionally not part of the unit-build gate because the current CI tests do not require a browser. Install Chromium before executing UI plans:

```bash
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## v3.6.0 — Test Suite & Parallel Execution

Multiple JSON plans can execute in parallel with configurable worker count. Each test gets fresh API/UI executor state. Suite reports are generated in HTML/CSV/PDF/JSON plus individual test reports. A failing suite exits non-zero for CI.

## v3.5.0 — PDF Reporting & Dashboard

Executions produce HTML, CSV, PDF, JSON and execution logs. Failure artifacts are linked from HTML reports.

## v3.4.0 — Failure Artifacts

UI failures can create screenshots/metadata. API failures capture request/response diagnostics. v3.10 redaction protects sensitive evidence.

## v3.3.0 — Advanced Reporting

Execution results contain per-step status, duration, diagnostic details, failure analysis, and artifact references.

## v3.2.0 — Retry & Resilience

API steps support `retryCount`. `0` means one attempt and `2` means up to three attempts. If omitted, v3.11 uses global `RETRIES`.

## Architecture

```text
Requirement
    ↓
PromptManager → OllamaClient
    ↓
TestPlan JSON
    ↓
EnvironmentManager
    ↓
AgentRunner
    ├── ApiExecutor → ApiAssertionEngine → REST Assured
    └── UiExecutor → Playwright
    ↓
ExecutionResult / SuiteExecutionResult
    ↓
SecurityRedactor
    ↓
ReportManager / SuiteReportManager
    ↓
RunHistoryManager → comparison/history
    ↓
HistoryAnalyticsManager → analytics/flaky/slow tests
```

## Project structure

```text
src/main/java/com/thiyagarajan/agent/
├── ai/
├── config/
├── model/
├── report/
└── runtime/
    ├── ApiAssertionEngine.java
    ├── ApiExecutor.java
    ├── ExecutionResult.java
    ├── FailureArtifactManager.java
    ├── HistoryAnalyticsManager.java
    ├── RunComparisonResult.java
    ├── RunHistoryManager.java
    ├── SecurityRedactor.java
    ├── SuiteExecutionEngine.java
    └── UiExecutor.java
```

## Configuration

| Variable | Default | Purpose |
|---|---:|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless execution |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default API/UI timeout |
| `RETRIES` | `0` | Default API retries when step `retryCount` is omitted |
| `PARALLELISM` | `4` | Maximum suite workers |
| `REPORTS_DIR` | `reports` | Report/history root |
| `SCREENSHOTS_DIR` | `screenshots` | Failure-artifact subdirectory |

## Supported actions

API: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.

UI: `navigate`, `click`, `fill`, `press`, `selectOption`, `assertVisible`, `assertText`, `assertValue`, `waitFor`, `screenshot`.

## Build and test

```bash
mvn -B clean verify
```

## Security checklist

1. Store secrets in CI/environment variables, not Git files.
2. Prefer `${env.NAME}` for secret injection.
3. Do not disable redaction when publishing reports.
4. Treat failure artifacts as potentially sensitive even after masking.
5. Review custom assertion/test-data values before sharing reports.
6. The LLM remains restricted to the fixed test-plan schema; arbitrary shell, JavaScript, SQL, or Java execution is not introduced.

## v3.11 PDF compilation fix

The CI compiler error caused by calling `Paragraph.setBold()` was fixed. In the iText version used by this project, bold styling is applied to the contained `Text` element instead:

```java
new Paragraph(new Text("Heading").setBold()).setFontSize(18)
```

The same correction is applied to both `PdfReportWriter` and `SuiteReportManager`.

## Version history

- v3.2.0 — Retry & resilience engine
- v3.3.0 — Advanced execution reporting and logs
- v3.4.0 — Failure artifacts & screenshot management
- v3.5.0 — Real PDF reporting & execution dashboard
- v3.6.0 — Test suite & parallel execution engine
- v3.7.0 — CI/CD & GitHub Actions integration
- v3.8.0 — Test data management & environment profiles
- v3.9.0 — Advanced assertions & validation diagnostics
- v3.10.0 — Secure secrets & sensitive-data redaction
- v3.11.0 — Execution history, run comparison, regression detection, CI/bug fixes
- v3.12.0 — Historical analytics, flaky-test detection, slow-test analysis, PDF compilation fix
