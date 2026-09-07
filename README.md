# AI Testing Agent — v3.14.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, Playwright, and advanced reporting.

## v3.14.0 — Execution Architecture Refactor

v3.14 introduces a dedicated `TestOrchestrator` layer between the CLI and runtime components. The goal is to keep `Main` focused on command-line concerns while centralizing execution lifecycle, environment handling, reporting, and suite history coordination.

### Improvements
- Added `TestOrchestrator` as the execution coordination layer.
- Reduced `Main` to CLI dispatch, interactive input, output, and exit-code handling.
- Centralized plan file validation and JSON loading.
- Centralized suite file validation and suite execution/reporting.
- Centralized optional environment-profile application.
- Centralized failure-analysis handling for plan execution.
- Preserved deterministic exit codes: `0` success, `1` executed test failure, `2` application/configuration/validation/infrastructure error.
- Added orchestrator validation tests for null dependencies and missing plan/suite files.
- Existing `AgentRunner`, API/UI executors, suite engine, reports, history, analytics, and redaction remain reusable components.

### Architecture

```text
CLI
 │
 ▼
Main
 │  command dispatch / interactive input / exit code
 ▼
TestOrchestrator
 ├── Config
 ├── EnvironmentManager
 ├── AgentRunner
 ├── ReportManager
 └── SuiteReportManager
      │
      ▼
 AgentRunner
 ├── API Executor
 └── UI Executor
      │
      ▼
 ExecutionResult / SuiteExecutionResult
      │
      ├── Reports
      ├── Failure Analysis
      └── History / Analytics
```

## v3.13.0 — Execution Reliability & Error Handling

- Standardized `AgentExecutionException` with stable error categories.
- Strict runtime configuration validation.
- Plan and suite JSON validation diagnostics.
- Structured CLI error handling.
- Per-test suite failure isolation.
- Safe executor interruption and shutdown.
- Suite-root path traversal protection.
- Validation unit tests.

## v3.12.0 — Historical Analytics & Flaky-Test Detection

- Historical suite analytics across recent runs.
- Flaky-test detection based on PASS/FAIL status transitions.
- Top slowest tests by average duration.
- `analytics.html`, `analytics.json`, and `analytics.csv`.
- Regression/fixed/new/removed comparison remains available.
- Sensitive values are redacted before analytics output.

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

## All examples are executed

`scripts/ci/run-examples.sh` validates the requirement fixtures, parses every JSON example, executes the v3 plan and suite, and executes both API and UI sections from the legacy v2 example.

Requirement `.txt` files are validated as AI input fixtures rather than requiring Ollama in CI.

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

Interactive mode:

```bash
mvn exec:java -Dexec.args="interactive"
```

## Exit codes

```text
0 = successful execution
1 = executed plan/test/suite failed
2 = configuration, validation, application, or infrastructure error
```

## Reporting

The framework produces JSON, CSV, HTML and PDF execution reports plus suite/history/analytics dashboards and failure artifacts.

## Security

`SecurityRedactor` masks passwords, secrets, tokens, API keys, client secrets, authorization values, cookies, and sensitive environment-variable values before persistence or AI failure analysis.

## Complete documentation

`PROJECT_DETAILS.md` remains the canonical detailed implementation and operations guide. The v3.14 architecture change adds `TestOrchestrator` as the lifecycle coordination layer and keeps `Main` as a thin CLI entry point.

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
- v3.14.0 — TestOrchestrator execution architecture refactor and CLI simplification
