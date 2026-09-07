# AI Testing Agent — v3.12.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, Playwright, and iText reporting.

## v3.12.0 — Historical Analytics & Flaky-Test Detection

Version 3.12 extends v3.11 execution history with historical analytics, flaky-test detection, and a dedicated analytics dashboard.

### PDF compatibility fix

The project uses iText `9.3.0`. The available iText API does not provide `setBold()` on `Paragraph` or `Text` in this dependency setup. PDF headings therefore use the supported `Paragraph` API without the unsupported method. This fixes the GitHub Actions Maven compiler failure.

### New capabilities
- Historical suite execution analytics across the latest 20 runs by default.
- Flaky-test detection based on pass/fail status transitions.
- Configurable flaky threshold between `0.0` and `1.0`.
- Top 10 slowest tests by average execution duration.
- `analytics.html`, `analytics.json`, and `analytics.csv`.
- Existing regression/fixed/new/removed comparison from v3.11 remains available.
- Sensitive values are redacted before analytics output.

### Flaky detection

For a test with `N` executions:

```text
flakinessRate = statusChanges / (N - 1)
```

A test is considered flaky when it has at least two executions, both PASS and FAIL results, and its rate is at or above the configured threshold.

## v3.11.0 — Test Execution History & Run Comparison

- Every suite execution is snapshotted under `reports/history/<run-id>/`.
- Stable comparison identity uses `planFile + testName`.
- Categories: `REGRESSION`, `FIXED`, `PASSED_UNCHANGED`, `FAILED_UNCHANGED`, `NEW_TEST`, `REMOVED_TEST`.
- Tracks pass-rate and duration deltas.
- Writes comparison JSON/CSV/HTML.
- Maintains browsable history index.
- Duplicate stable test identities are rejected.
- History failures cannot change the suite pass/fail result.
- Global `RETRIES` is used when an API step omits `retryCount`.
- Interactive EOF is handled safely.

## v3.10.0 — Secure Secrets & Sensitive Data Redaction

Centralized `SecurityRedactor` masks passwords, secrets, tokens, API keys, client secrets, authorization values, cookies, common authentication headers, and sensitive environment-variable values before logs, reports, failure artifacts, or AI failure-analysis prompts are persisted/transmitted.

## v3.9.0 — Advanced Assertions & Validation Diagnostics

Supported API assertions include status, body contains/not-contains, regex, response headers, JSONPath exists/equality/contains/regex, and response-time limits. Legacy `assertSpec` remains supported.

## v3.8.0 — Test Data Management & Environment Profiles

Profiles can provide base URLs, headers, variables, timeouts, and JSON test data. Placeholders: `${profile.X}`, `${data.X}`, `${env.X}`.

## v3.7.0 — CI/CD & GitHub Actions

`.github/workflows/ci.yml` uses Java 21 and runs `mvn -B --no-transfer-progress clean verify`. Reports are uploaded when available. Browser installation is kept out of the unit-build gate; install Chromium before executing UI plans.

## v3.6.0 — Test Suite & Parallel Execution

Multiple JSON plans execute in parallel with configurable workers. Each test gets isolated executor state. Suite reports are produced in HTML, CSV, PDF, and JSON with individual test reports.

## v3.5.0 — PDF Reporting & Dashboard

Executions produce HTML, CSV, PDF, JSON, and execution logs.

## Architecture

```text
Requirement → PromptManager → OllamaClient → TestPlan
                                      ↓
EnvironmentManager → AgentRunner → API/UI Executors
                                      ↓
ExecutionResult / SuiteExecutionResult
                                      ↓
SecurityRedactor → Reports
                                      ↓
RunHistoryManager → HistoryAnalyticsManager
                                      ↓
History / Comparison / Analytics dashboards
```

## Configuration

| Variable | Default | Purpose |
|---|---:|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless execution |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default API/UI timeout |
| `RETRIES` | `0` | Default API retries |
| `PARALLELISM` | `4` | Maximum suite workers |
| `REPORTS_DIR` | `reports` | Report/history root |
| `SCREENSHOTS_DIR` | `screenshots` | Failure-artifact directory |

## Build and test

```bash
mvn -B clean verify
```

For UI execution, install Chromium first:

```bash
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## Version history

- v3.2.0 — Retry & resilience
- v3.3.0 — Advanced reporting and logs
- v3.4.0 — Failure artifacts
- v3.5.0 — PDF reporting and dashboard
- v3.6.0 — Suite and parallel execution
- v3.7.0 — CI/CD integration
- v3.8.0 — Environment profiles and test data
- v3.9.0 — Advanced assertions
- v3.10.0 — Sensitive-data redaction
- v3.11.0 — Execution history and run comparison
- v3.12.0 — Historical analytics, flaky-test detection, and iText PDF compatibility
