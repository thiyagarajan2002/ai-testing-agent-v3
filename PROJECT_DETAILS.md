# AI Testing Agent — Complete Project Documentation

## Current version

**3.21.0 — Comprehensive examples, UI evidence, durable execution logs and unified reports**

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, JSON/CSV data-driven testing, environment profiles, assertions, retries, failure artifacts, HTML/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation, and preflight validation.

## v3.21.0 example library

The `examples/` directory provides multiple API, UI, suite and data-driven scenarios.

### API plans

```text
examples/plans/api/
├── get-user.json
├── create-resource.json
├── save-variable.json
├── v3-plan-file.json
├── 02-post.json
├── 03-put.json
├── 04-delete.json
├── 05-query-headers.json
└── 06-negative-status.json
```

Coverage includes GET, POST, PUT, DELETE, query parameters, headers, request bodies, response assertions, response-variable reuse, retry behavior and negative status validation.

### UI plans

```text
examples/plans/ui/
├── homepage-smoke.json
├── login-flow.json
├── search-flow.json
├── negative-login.json
├── 01-navigation.json
├── 02-login.json
├── 03-search-and-cart.json
└── 04-negative-login.json
```

Coverage includes navigation, form entry, clicks, assertions, authentication, product/cart interaction and negative authentication.

### Suites

```text
examples/suites/
├── api-smoke-suite.json
├── ui-smoke-suite.json
├── api-regression-suite.json
├── ui-regression-suite.json
└── full-regression-suite.json
```

### Data-driven example

```text
examples/plans/data-driven/users-api.json
examples/data/json/users.json
```

Run with multiple workers or exact row filtering:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="data-driven examples/plans/data-driven/users-api.json examples/data/json/users.json --parallelism 3"
mvn -B --no-transfer-progress exec:java -Dexec.args="data-driven examples/plans/data-driven/users-api.json examples/data/json/users.json --filter userId=2"
```

## Running examples

API:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="plan examples/plans/api/get-user.json"
```

UI:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="plan examples/plans/ui/02-login.json"
```

API regression suite:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="suite examples/suites/api-regression-suite.json"
```

UI regression suite:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="suite examples/suites/ui-regression-suite.json"
```

Full regression suite:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="suite examples/suites/full-regression-suite.json"
```

Validate without executing:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="validate plan examples/plans/api/get-user.json"
mvn -B --no-transfer-progress exec:java -Dexec.args="validate plan examples/plans/ui/02-login.json"
mvn -B --no-transfer-progress exec:java -Dexec.args="validate suite examples/suites/api-regression-suite.json"
mvn -B --no-transfer-progress exec:java -Dexec.args="validate suite examples/suites/ui-regression-suite.json"
```

## API execution evidence

API executions persist request/response details under:

```text
reports/api/logs/
├── <test-name>-<timestamp>.log
└── terminal-<timestamp>-plan.log
```

The API execution log records timestamp, step number, attempt number, HTTP method, URL, request information, response status, duration, bounded response information and errors. Sensitive values are redacted. Logging failures do not fail the test.

## UI screenshot evidence

Every successful UI step automatically captures a screenshot. A failed UI step also captures failure evidence.

```text
reports/screenshots/ui/
└── <test-name>/
    ├── 001-navigate.png
    ├── 002-fill.png
    ├── 003-fill.png
    ├── 004-click.png
    └── 005-assertvisible.png
```

The explicit `screenshot` action remains supported for custom evidence points. Screenshot paths are attached to step results so reports can display the corresponding evidence.

## Terminal run logging

`RunLogManager` tees application stdout and stderr: the output remains visible in the terminal and is simultaneously persisted to disk.

API-oriented commands:

```text
reports/api/logs/terminal-<timestamp>-<command>.log
```

Interactive UI-oriented commands:

```text
reports/ui/logs/terminal-<timestamp>-interactive.log
```

Other commands:

```text
reports/terminal/logs/
```

Each terminal log contains the command, application stdout/stderr and final exit code. The original console streams are restored when the run finishes.

## Unified reporting

`ReportManager.writeAll(ExecutionResult)` is the single execution-report entry point. It normalizes the result once and generates exactly three report files from the same execution data:

```text
reports/<run>/
├── report.html
├── report.csv
└── report.pdf
```

Runtime logs and screenshots are evidence, not extra report formats. They remain in their dedicated folders and are referenced by step artifacts when available.

### `ReportManager.writeAll(ExecutionResult)`

1. Validates and normalizes the supplied `ExecutionResult`.
2. Captures one generated timestamp shared by all report formats.
3. Calls the HTML writer.
4. Calls the CSV writer.
5. Calls `PdfReportWriter.write(...)`.

The method no longer writes `execution.log` as part of the report bundle.

### HTML report

The HTML report includes:

- execution/test name and overall PASS/FAIL status
- total, passed and failed step KPI cards
- pass rate, total duration and average duration
- search across execution rows
- PASS/FAIL filtering
- slow-step filtering using a calculated threshold
- sortable action/status/duration columns
- expandable step details containing input/output text
- artifact links for API logs, UI screenshots or other evidence
- failure analysis
- light/dark theme toggle
- print support
- direct links to `report.csv` and `report.pdf`

### CSV report

The CSV report uses UTF-8 with BOM and CSV-safe quoting. Each execution row contains:

```text
test,action,status,duration_ms,details,artifacts
```

A summary footer records generated time, overall status, total steps, passed/failed totals, pass rate, total/average/min/max duration and failure analysis.

### PDF report

`PdfReportWriter` creates the printable counterpart of the HTML/CSV reports. It contains:

- generated timestamp
- test name and overall status
- total steps and passed/failed totals
- pass rate
- total, average, minimum and maximum duration
- step-by-step action/status/duration/details
- input/output text carried in step details
- artifact references
- failure analysis

### Reporting consistency rule

HTML, CSV and PDF must always be produced from the same `ExecutionResult`. New reporting fields should be added to the shared execution model first, then rendered consistently in all supported formats.

### Regression test

`ReportManagerTest.writeAllGeneratesOnlyHtmlCsvAndPdfReports()` verifies that a report run creates exactly `report.html`, `report.csv` and `report.pdf`, that the PDF is non-empty, that HTML includes the expected dashboard/detail content, and that CSV includes its summary and failure analysis.

## Technology stack

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

## Repository structure

```text
ai-testing-agent-v3/
├── .github/
├── config/
├── docs/
├── examples/
│   ├── plans/api/
│   ├── plans/ui/
│   ├── plans/data-driven/
│   ├── suites/
│   ├── data/json/
│   ├── requirements/
│   └── legacy/
├── scripts/
├── src/main/java/com/thiyagarajan/agent/
├── src/main/resources/
├── src/test/java/com/thiyagarajan/agent/
├── src/test/resources/
├── reports/
├── README.md
├── PROJECT_DETAILS.md
├── CONTRIBUTING.md
├── SECURITY.md
└── pom.xml
```

## Core execution

`TestPlan` contains the test name, type, base URL, variables and steps. `TestStep` supports API method/path, headers, query parameters, body, timeout, retries, assertions and UI locator/value fields.

`AgentRunner` plans requirements with Ollama, validates plans and executes API/UI tests. `TestOrchestrator` coordinates file loading, environment application, execution, validation, reporting and history.

`ApiExecutor` builds requests, evaluates assertions, saves configured response values and applies retry policy. `UiExecutor` executes Playwright actions and captures automatic evidence.

`SuiteExecutionEngine` validates suite paths, loads multiple plans, executes independently with configurable parallelism, aggregates results and safely shuts down workers.

## Validation and security

`TestPlanValidator` performs preflight checks before execution. `AgentExecutionException` standardizes configuration, plan, suite, API, UI, assertion, AI, reporting and infrastructure failures.

`SecurityRedactor` masks passwords, secrets, tokens, access tokens, API keys, authorization headers, cookies and related sensitive values before diagnostics, reports, artifacts, history, API logs and AI analysis.

## Reporting and history

HTML, CSV and PDF are the supported execution-report formats. Data-driven reporting records iteration results, execution mode, workers, duration and estimated speedup. History/analytics remain separate runtime features for run comparison, pass/fail totals, flaky-test detection and slow-test analysis.

## CI/CD

GitHub Actions performs Java 21 setup, compilation, Playwright/Chromium installation, Maven verification, example validation/execution and artifact handling. A release is only described as build-verified when the corresponding Maven or CI run actually completes successfully.

## Release documentation rule

Every release updates:

1. `pom.xml`
2. CLI version
3. `README.md`
4. `PROJECT_DETAILS.md`
5. `docs/releases/CHANGELOG.md`
6. Tests for new behavior
7. Example documentation when applicable
