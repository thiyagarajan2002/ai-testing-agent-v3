# AI Testing Agent — v3.12.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, Playwright, and iText reporting.

## v3.12.0 — Historical Analytics & Flaky-Test Detection

Version 3.12 extends v3.11 execution history with historical analytics, flaky-test detection, and a dedicated analytics dashboard.

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

### PDF compatibility fix

The project uses iText `9.3.0`. The available API in this dependency setup does not provide `setBold()` on `Paragraph` or `Text`; PDF headings therefore use supported APIs only.

## CI/CD — every push validates every example

The GitHub Actions workflow runs on every push to `main`, pull requests to `main`, and manual dispatch.

The pipeline now deliberately **compiles the project before invoking the Playwright Java CLI**. This prevents the previous `ClassNotFoundException: com.thiyagarajan.agent.Main` failure caused by invoking the Maven exec goal before `target/classes` had been created.

CI stages:

1. Checkout with `actions/checkout@v5`.
2. Install Java 21 with `actions/setup-java@v5`.
3. `mvn clean -DskipTests compile`.
4. Install Chromium through the dedicated Maven `playwright-cli` execution.
5. `mvn verify` for the full unit/integration test gate.
6. Run `scripts/ci/run-examples.sh`.
7. Upload reports, screenshots, Surefire results, and generated example files.

### Every example is executed

The example runner:

1. Validates `examples/api-requirement.txt`.
2. Validates `examples/ui-requirement.txt`.
3. Parses every JSON example under `examples/`.
4. Executes `examples/v3-plan-file.json`.
5. Executes `examples/v3-suite.json`.
6. Extracts and executes the API section from `examples/v2-execution.json`.
7. Extracts and executes the UI section from `examples/v2-execution.json`.

The requirement `.txt` files are AI input fixtures, so CI validates their content rather than invoking Ollama. This keeps CI deterministic while continuously checking every repository example.

## Complete documentation

`PROJECT_DETAILS.md` is the canonical detailed project guide. It documents architecture, package responsibilities, important methods, configuration, test-plan schema, assertions, retries, UI/API execution, reports, failure artifacts, security redaction, suite/history/analytics behavior, all examples, CI behavior, troubleshooting, development commands, bug fixes, and the rule to update documentation with every future change.

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

Java 21, Maven verification, report artifacts, and Playwright support were introduced. The current workflow additionally executes all examples after the unit-test gate.

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
| `SCREENSHOTS_DIR` | `screenshots` | Failure-artifact root |

## Build and test

```bash
mvn -B clean verify
```

Install Chromium locally:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

Run all examples locally:

```bash
bash scripts/ci/run-examples.sh
```

Run a plan:

```bash
mvn exec:java -Dexec.args="plan examples/v3-plan-file.json"
```

Run a suite:

```bash
mvn exec:java -Dexec.args="suite examples/v3-suite.json"
```

## CI troubleshooting

### `ClassNotFoundException: com.thiyagarajan.agent.Main`

Do not invoke `exec:java` before the project has been compiled. Use the configured Playwright execution after compilation:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

The workflow follows the same order.

### No artifacts found

Artifact upload is intentionally `if: always()` and `if-no-files-found: ignore`. If an earlier build step fails before generating reports, no artifact is expected. Once the test/example stages run, generated output is collected automatically.

### GitHub Actions Node deprecation warnings

The workflow uses the current v5 releases of checkout, setup-java, and upload-artifact to avoid the older Node 20 action runtime warning. Third-party/transitive Node warnings from action internals may still appear if GitHub changes runner behavior; they are not Java project failures.

### External example service failure

The v2/v3 executable examples use public API/UI services. If those services are unavailable, CI correctly fails the example stage. Investigate service availability rather than hiding or skipping the example.

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
- v3.12.0 — Historical analytics, flaky-test detection, iText PDF compatibility, and CI/example execution hardening
