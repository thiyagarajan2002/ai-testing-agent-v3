# AI Testing Agent — v3.15.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, Playwright, environment profiles, retries, assertions, failure artifacts, advanced reports, suite execution, history/analytics, security redaction, and preflight validation.

## v3.15.0 — Preflight Validation & Dry Run

v3.15 adds a reusable validation layer that checks a test plan **without executing API requests, opening a browser, or contacting Ollama**.

### What was added

- `TestPlanValidator` with reusable validation rules.
- Validation of plan type, base URL, steps, actions, timeout, and retry configuration.
- Non-blocking warnings for suspicious but executable configurations.
- `validate plan <file>` CLI command.
- `validate suite <file>` CLI command that validates every referenced plan.
- Suite-root path traversal checks during preflight.
- Environment profile application before validation when `--env` is supplied.
- `AgentRunner` now uses the same validator as the preflight command, preventing validation drift.
- JUnit coverage for valid plans, multiple diagnostics, warnings, and null plans.

### Why it matters

Use preflight validation in CI or locally before a real execution. It catches configuration and plan mistakes early while producing actionable errors without causing API side effects or requiring a browser.

## CLI

```bash
# Validate one plan without executing it
mvn exec:java -Dexec.args="validate plan examples/v3-plan-file.json"

# Validate a plan with an environment profile
mvn exec:java -Dexec.args="validate plan examples/v3-plan-file.json --env qa"

# Validate every plan referenced by a suite
mvn exec:java -Dexec.args="validate suite examples/v3-suite.json"

# Normal execution remains unchanged
mvn exec:java -Dexec.args="plan examples/v3-plan-file.json"
mvn exec:java -Dexec.args="suite examples/v3-suite.json"
```

Validation exit codes:

```text
0 = valid
1 = validation completed but errors were found
2 = validation could not be performed because of configuration/file/infrastructure error
```

## Architecture

```text
CLI
 │
 ▼
Main
 │  command dispatch / validation / interactive input / exit codes
 ▼
TestOrchestrator
 ├── Config
 ├── EnvironmentManager
 ├── AgentRunner
 │    ├── TestPlanValidator
 │    ├── API Executor
 │    └── UI Executor
 ├── ReportManager
 └── SuiteReportManager
      │
      ▼
 ExecutionResult / SuiteExecutionResult
      ├── Reports
      ├── Failure Analysis
      ├── History / Comparison
      └── Historical Analytics
```

## Major capabilities

### API testing

- GET, POST, PUT, PATCH, DELETE
- Headers and query parameters
- Request bodies
- Saved response variables
- Multiple typed assertions
- Response-time validation
- Configurable retries
- Failure evidence

### UI testing

Playwright-based actions include navigation, click, fill, press, select option, visibility/text/value assertions, waits, and screenshots.

### Assertions

- `status`
- `bodyContains`
- `bodyNotContains`
- `bodyRegex`
- `headerEquals`
- `jsonPathExists`
- `jsonPathEquals`
- `jsonPathContains`
- `jsonPathRegex`
- `responseTimeMs`
- legacy `assertSpec`

### Environment and test data

Supported placeholders include:

```text
${profile.baseUrl}
${data.userId}
${env.API_TOKEN}
${version}
```

Environment profiles can define base URL, timeout, variables, headers, and test-data file settings.

### Retry behavior

A step-level `retryCount` overrides the global `RETRIES` setting. Retries are validated so negative values fail before execution.

### Failure artifacts

Failed tests can produce screenshots and API evidence. Sensitive fields are redacted before persistence.

### Reporting

Execution and suite reports support JSON, CSV, HTML, and PDF. Suite runs also produce history and analytics dashboards.

### History and comparison

Suite runs are stored under:

```text
reports/history/<run-id>/
```

Comparisons classify tests as:

- REGRESSION
- FIXED
- PASSED_UNCHANGED
- FAILED_UNCHANGED
- NEW_TEST
- REMOVED_TEST

Historical analytics also detects flaky tests and identifies slow tests.

### Security

`SecurityRedactor` masks passwords, secrets, tokens, API keys, client secrets, authorization values, cookies, and sensitive environment-variable values before reports, artifacts, history, analytics, and AI failure-analysis prompts.

## Configuration

| Variable | Default | Purpose |
|---|---:|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless execution |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default timeout |
| `RETRIES` | `0` | Default API retry count |
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

Run all repository examples:

```bash
bash scripts/ci/run-examples.sh
```

## CI/CD

GitHub Actions runs on pushes to `main`, pull requests to `main`, and manual dispatch. The workflow compiles before installing Playwright, runs the Maven verification suite, validates examples, and uploads reports/artifacts.

## Project structure

```text
ai-testing-agent-v3/
├── .github/workflows/ci.yml
├── config/
│   ├── environments/
│   └── test-data/
├── examples/
├── scripts/ci/run-examples.sh
├── src/main/java/com/thiyagarajan/agent/
│   ├── Main.java
│   ├── ai/
│   ├── config/
│   ├── model/
│   ├── report/
│   └── runtime/
│       ├── TestPlanValidator.java
│       ├── TestOrchestrator.java
│       ├── AgentRunner.java
│       ├── ApiExecutor.java
│       ├── UiExecutor.java
│       ├── SuiteExecutionEngine.java
│       ├── RunHistoryManager.java
│       ├── HistoryAnalyticsManager.java
│       └── SecurityRedactor.java
├── src/test/java/com/thiyagarajan/agent/
└── pom.xml
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
- v3.12.0 — Historical analytics, flaky-test detection, PDF compatibility, and CI hardening
- v3.13.0 — Execution reliability, standardized errors, strict configuration validation, suite failure isolation, executor lifecycle hardening, structured CLI errors, and validation tests
- v3.14.0 — TestOrchestrator execution architecture refactor and CLI simplification
- **v3.15.0 — Preflight validation, dry-run plan checks, suite validation, and shared validation logic**

## Canonical documentation

`PROJECT_DETAILS.md` is the detailed implementation and operations guide. Update it together with each release so the repository documentation remains aligned with the code.
