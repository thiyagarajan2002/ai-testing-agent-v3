# AI Testing Agent — v3.13.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, Playwright, and advanced reporting.

## v3.13.0 — Execution Reliability & Error Handling

v3.13 hardens the execution pipeline without changing the existing API/UI test-plan model.

### Improvements
- Added standardized `AgentExecutionException` with stable error categories.
- Added strict `Config` validation for Ollama settings, timeout, retries, parallelism, and output directories.
- Invalid integer environment variables now produce explicit configuration errors instead of silently falling back.
- Added plan and suite JSON validation diagnostics.
- Added structured CLI error handling with deterministic exit code `2` for application/configuration/infrastructure errors.
- Suite test failures are isolated so one broken plan does not stop independent plans.
- Suite executor interruption is handled safely and the interrupt flag is restored.
- Executor shutdown now waits for worker termination and forces shutdown when necessary.
- Plan paths are normalized and prevented from escaping the suite directory.
- Added configuration and plan-validation unit tests.

### Error categories

```text
CONFIGURATION
PLAN_VALIDATION
SUITE_VALIDATION
API_EXECUTION
UI_EXECUTION
ASSERTION
AI_GENERATION
REPORTING
INFRASTRUCTURE
```

### Exit codes

```text
0 = success
1 = executed test/plan/suite failed
2 = configuration, validation, or infrastructure/application error
```

## v3.12.0 — Historical Analytics & Flaky-Test Detection

- Historical suite analytics across recent runs.
- Flaky-test detection based on PASS/FAIL status transitions.
- Top slowest tests by average duration.
- `analytics.html`, `analytics.json`, and `analytics.csv`.
- Regression/fixed/new/removed comparison remains available.
- Sensitive values are redacted before analytics output.

Flakiness rate:

```text
statusChanges / (executions - 1)
```

## CI/CD

The GitHub Actions workflow runs on every push to `main`, pull requests to `main`, and manual dispatch.

CI order:

1. Checkout with `actions/checkout@v5`.
2. Java 21 with `actions/setup-java@v5`.
3. Compile with `mvn clean -DskipTests compile`.
4. Install Chromium using `exec:java@playwright-cli`.
5. Run `mvn verify`.
6. Run all repository examples.
7. Upload reports and test artifacts.

The compile-before-Playwright step fixes the previous clean-runner `ClassNotFoundException: com.thiyagarajan.agent.Main` problem.

### All examples are executed

`scripts/ci/run-examples.sh` validates the requirement fixtures, parses every JSON example, executes the v3 plan and suite, and executes both API and UI sections from the legacy v2 example.

Requirement `.txt` files are validated as AI input fixtures rather than requiring Ollama in CI.

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
| `DEFAULT_TIMEOUT_MS` | `30000` | Default timeout |
| `RETRIES` | `0` | Default API retries |
| `PARALLELISM` | `4` | Maximum suite workers |
| `REPORTS_DIR` | `reports` | Report/history root |
| `SCREENSHOTS_DIR` | `screenshots` | Failure-artifact root |

Invalid configuration values now fail explicitly during startup.

## Build and test

```bash
mvn -B clean verify
```

Install Chromium locally:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

Run all examples:

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

## Reporting

The framework produces JSON, CSV, HTML and PDF execution reports plus suite/history/analytics dashboards and failure artifacts.

## Security

`SecurityRedactor` masks passwords, secrets, tokens, API keys, client secrets, authorization values, cookies, and sensitive environment-variable values before persistence or AI failure analysis.

## Troubleshooting

### ClassNotFoundException during Playwright installation

Compile before invoking the dedicated Playwright CLI:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

### Invalid configuration

Check `OLLAMA_URL`, `OLLAMA_MODEL`, `DEFAULT_TIMEOUT_MS`, `RETRIES`, `PARALLELISM`, `REPORTS_DIR`, and `SCREENSHOTS_DIR`. v3.13 reports the configuration category and invalid value instead of silently accepting malformed numeric settings.

### Suite contains one failing plan

v3.13 isolates individual plan execution failures and continues independent suite tasks. The final suite remains `FAIL`, while the report identifies the failed plan and its category.

### External example service failure

The executable examples use public API/UI services. CI correctly reports service availability failures rather than hiding or skipping them.

## Complete documentation

`PROJECT_DETAILS.md` is the canonical implementation and operations guide. Every future version must update it with code changes, methods, configuration, tests, CI behavior, bugs, commands, and troubleshooting.

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
- v3.12.0 — Historical analytics, flaky-test detection, PDF compatibility, and CI hardening
- v3.13.0 — Execution reliability, standardized errors, strict configuration validation, suite failure isolation, executor lifecycle hardening, structured CLI errors, and validation tests
