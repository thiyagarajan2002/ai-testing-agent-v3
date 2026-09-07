# AI Testing Agent — Complete Project Documentation

## 1. Project overview

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, JSON/CSV data-driven testing, environment profiles, assertions, retries, failure artifacts, HTML/JSON/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation, and non-executing preflight validation.

**Current version: 3.21.0**

## 2. Technology stack

- Java 21
- Maven
- Ollama
- Jackson
- REST Assured
- Playwright
- JUnit 5
- Apache Commons CSV
- iText 9.3.0
- SLF4J
- GitHub Actions

## 3. Repository structure

```text
ai-testing-agent-v3/
├── .github/                     # CI/CD and repository automation
├── config/                      # Environment profiles and reusable test data
├── docs/                        # Architecture, configuration, testing and releases
├── examples/
│   ├── plans/api/               # API examples
│   ├── plans/ui/                # UI examples
│   ├── plans/data-driven/       # Data-driven plans
│   ├── suites/                  # API/UI suite examples
│   ├── data/json/               # JSON datasets
│   ├── requirements/            # Natural-language requirements
│   └── legacy/                  # Earlier-release compatibility examples
├── scripts/ci/                  # CI helper scripts
├── src/main/java/com/thiyagarajan/agent/
│   ├── Main.java
│   ├── ai/
│   ├── config/
│   ├── io/                      # Durable execution logging
│   ├── model/
│   ├── report/
│   └── runtime/
├── src/main/resources/          # Runtime classpath resources
├── src/test/java/com/thiyagarajan/agent/
├── src/test/resources/          # Test-only fixtures
├── reports/                     # Generated runtime output
├── CONTRIBUTING.md
├── SECURITY.md
├── README.md
├── PROJECT_DETAILS.md
└── pom.xml
```

Repository organization was standardized in v3.20.0 without changing runtime behavior. The target Java package architecture for a later controlled migration is documented in `docs/architecture/package-structure.md`.

## 4. Main commands

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]"
mvn exec:java -Dexec.args="validate plan <file>"
mvn exec:java -Dexec.args="validate suite <file>"
mvn exec:java -Dexec.args="interactive"
```

Install Chromium for UI execution:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

## 5. Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless mode |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default execution timeout |
| `RETRIES` | `0` | Default API retry count |
| `PARALLELISM` | `4` | Suite and default data-driven worker count |
| `REPORTS_DIR` | `reports` | Report/history root |
| `SCREENSHOTS_DIR` | `screenshots` | Screenshot root |

`Config` validates URLs, model name, timeout, retries, parallelism and output directories. Invalid integer configuration values fail explicitly.

## 6. Core model and execution classes

### `TestPlan`

Contains `name`, `type` (`API`/`UI`), `baseUrl`, `variables`, and `steps`.

### `TestStep`

Supports API method/path, headers, query parameters, body, timeout, retry count, assertions, saved variables, and UI locator/value fields.

### `AgentRunner`

- `plan(requirement)` generates and validates an AI test plan.
- `execute(plan)` validates and executes API/UI plans.
- `executeSuite(...)` provides backward-compatible sequential suite execution.
- `analyzeFailure(plan,result)` sends redacted diagnostics to Ollama.

Each `execute(plan)` call creates the API or UI executor for that test, keeping executor state isolated.

### `TestOrchestrator`

Coordinates file loading, environment application, plan/suite/data-driven execution, reporting, validation and history. Important methods include `executePlan`, `executeSuite`, `executeDataDriven`, `validatePlan`, `validateSuite`, `plan`, `execute`, `analyzeIfFailed`, and `writeReport`.

## 7. Preflight validation

`TestPlanValidator` is shared by CLI validation and runtime execution. It validates plan type, base URL, steps, actions, timeout and retry settings and produces errors/warnings.

Validation errors are step-aware. For example, an invalid retry count is reported as `Step 1: retryCount cannot be negative`.

`validate plan` and `validate suite` do not send API requests, launch browsers, contact Ollama or execute tests.

Suite validation checks referenced files, rejects blank paths, allows sibling plan directories inside the suite workspace and blocks paths escaping that workspace.

## 8. Data-driven execution

JSON root arrays, JSON `rows` objects and CSV datasets are supported. `${data.key}` can be used throughout plans. `--filter key=value` performs exact matching and multiple filters are ANDed.

`DataDrivenRunner` supports bounded parallel execution with 1–64 workers, deep-copied plans, deterministic original-order results and safe executor shutdown.

`DataDrivenExecutionResult` records execution mode, worker count, duration, estimated sequential duration, estimated speedup and average iteration duration.

## 9. Reporting

The reporting layer supports JSON, CSV, HTML and PDF execution reports. Suite reporting aggregates test results. Data-driven reports are stored under `reports/data-driven/<safe-test-name>/`.

Dataset values are redacted and escaped before being written to reports.

## 10. API execution

`ApiExecutor` builds requests from plan values, applies headers/query/body, evaluates assertions, saves configured response values and retries according to policy.

Supported assertions include status, body contains/not-contains/regex, header equality, JSONPath exists/equality/contains/regex, response time and legacy `assertSpec` assertions.

### API execution logs — v3.21.0

Every API test attempts to create a durable log under:

```text
reports/api/logs/<safe-test-name>-<timestamp>.log
```

Each log records:

- timestamp
- step number
- attempt number
- HTTP action
- request URL
- request body
- response status
- response duration
- response body (bounded)
- errors
- completion marker

Sensitive data is redacted before API details are persisted. Logging failures never fail the API test.

## 11. UI execution

`UiExecutor` uses Playwright and supports:

- `navigate`
- `click`
- `fill`
- `press`
- `selectOption`
- `assertVisible`
- `assertText`
- `assertValue`
- `waitFor`
- `screenshot`

### Automatic screenshot evidence — v3.21.0

A screenshot is automatically captured after **every successful UI step**. Failed steps also capture a failure screenshot.

```text
reports/
└── screenshots/
    └── ui/
        ├── <test-name>/
        │   ├── 001-navigate.png
        │   ├── 002-fill.png
        │   ├── 003-click.png
        │   └── 004-assertvisible.png
        └── failures/
            └── <test-name>-step-4-failure.png
```

The screenshot path is stored in the step execution result so reporting layers can expose the evidence. Screenshots are full-page captures. The explicit `screenshot` action remains supported for backward compatibility.

## 12. Terminal run logging — v3.21.0

The CLI now preserves terminal output while the application is running. `RunLogManager` tees stdout and stderr to both the original terminal and a timestamped file.

API-oriented commands write terminal logs to:

```text
reports/api/logs/terminal-<timestamp>-<command>.log
```

Interactive runs write to:

```text
reports/ui/logs/terminal-<timestamp>-interactive.log
```

Other commands use:

```text
reports/terminal/logs/
```

The terminal log includes the command, all application stdout/stderr and the final exit code. Closing the log restores the original console streams and closes the file safely.

## 13. Retry behavior

A step-level `retryCount` overrides global `RETRIES`. Negative retry counts are rejected before execution, with the step number included in validation messages.

## 14. Failure artifacts

Failed API/UI executions can produce screenshots and API evidence under configured artifact directories. Sensitive metadata is redacted before persistence and artifact filenames are sanitized.

## 15. Security redaction

`SecurityRedactor` masks passwords, secrets, tokens, access tokens, API keys, client secrets, authorization headers, cookies and related sensitive values. Redaction is applied to execution diagnostics, reports, failure artifacts, history/analytics, API logs and AI failure-analysis prompts.

Production credentials must not be placed in examples or datasets.

## 16. Suite execution

`SuiteExecutionEngine` loads multiple plans, uses configurable parallelism, isolates test execution state, aggregates results and continues after an individual test failure. It validates suite-relative paths and safely shuts down worker resources.

## 17. Execution history and analytics

`RunHistoryManager` stores suite runs under `reports/history/<run-id>/`. `HistoryAnalyticsManager` calculates recent execution/pass/failure totals, detects flaky tests and identifies slow tests, producing JSON/CSV/HTML analytics reports.

## 18. Standardized errors

`AgentExecutionException` categorizes failures as `CONFIGURATION`, `PLAN_VALIDATION`, `SUITE_VALIDATION`, `API_EXECUTION`, `UI_EXECUTION`, `ASSERTION`, `AI_GENERATION`, `REPORTING` and `INFRASTRUCTURE`.

## 19. CI/CD

GitHub Actions performs Java 21 setup, compile, Playwright/Chromium installation, Maven verification, example execution/validation and report/artifact upload. A release must not be described as build-verified unless Maven/CI actually completed successfully.

## 20. v3.21.0 — Comprehensive examples, UI evidence and durable logs

This release expands the example library and improves execution observability.

### New API examples

- `examples/plans/api/get-user.json`
- `examples/plans/api/create-resource.json`
- `examples/plans/api/save-variable.json`
- `examples/suites/api-smoke-suite.json`

### New UI examples

- `examples/plans/ui/homepage-smoke.json`
- `examples/plans/ui/login-flow.json`
- `examples/plans/ui/search-flow.json`
- `examples/plans/ui/negative-login.json`
- `examples/suites/ui-smoke-suite.json`

### New implementation capabilities

1. API request/response/error logging.
2. Terminal stdout/stderr persistence while preserving live console output.
3. Automatic screenshot after every successful UI step.
4. Automatic screenshot on UI failure.
5. Screenshot paths attached to step results.
6. Dedicated API/UI example documentation.
7. Version updated to 3.21.0.

## 21. v3.20.0 — Repository organization & engineering standards

This release reorganized repository-level assets into clear ownership boundaries while preserving application behavior. Documentation, examples, resources, test fixtures and engineering standards were separated into dedicated locations. The target Java package architecture was documented before a future controlled package migration.

## 22. Version history

- v3.21.0 — comprehensive API/UI examples, automatic UI screenshots, API execution logs and terminal log persistence
- v3.20.0 — repository organization, documentation structure and engineering standards
- v3.19.0 — bounded parallel data-driven execution, ordered results, performance metrics and CLI worker override
- v3.18.0 — CSV datasets, dataset validation and exact row filtering
- v3.17.0 — data-driven reporting and HTML dashboard
- v3.16.0 — data-driven / parameterized execution
- v3.15.0 — preflight validation and dry-run checks
- v3.14.0 — history analytics and flaky-test analysis
- v3.13.0 — execution reliability and standardized errors
- v3.12.0 — execution history foundations
- v3.11.0 — history and run comparison
- v3.10.0 — sensitive-data redaction
- v3.9.0 — advanced assertions
- v3.8.0 — environment profiles and test data
- v3.7.0 — GitHub Actions CI/CD
- v3.6.0 — suite and parallel execution
- v3.5.0 — advanced PDF/reporting
- v3.4.0 — failure artifacts
- v3.3.0 — advanced reporting
- v3.2.0 — retry support

## 23. Development/release rule

For every release:

1. Update `pom.xml` version.
2. Update `Main.java` CLI version.
3. Update `README.md`.
4. Update `PROJECT_DETAILS.md`.
5. Update `docs/releases/CHANGELOG.md`.
6. Add/update tests for new behavior.
7. Verify Maven locally or verify the corresponding GitHub Actions run.

A release must not be described as build-verified unless Maven/CI actually completed successfully.
