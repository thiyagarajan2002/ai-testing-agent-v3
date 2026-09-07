# AI Testing Agent — Complete Project Documentation

## Current version

**3.21.0 — Comprehensive examples, UI evidence and durable execution logs**

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, JSON/CSV data-driven testing, environment profiles, assertions, retries, failure artifacts, HTML/JSON/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation, and preflight validation.

## v3.21.0 example library

The `examples/` directory now provides multiple scenarios instead of a single demonstration plan.

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

The regression suites demonstrate multi-plan execution and the full suite demonstrates combining API and UI scenarios.

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

This removes the need to add an explicit screenshot action after every step. The explicit `screenshot` action remains supported for custom evidence points. Screenshot paths are attached to step results so reports can display the corresponding evidence.

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

JSON, CSV, HTML and PDF reporting is supported. Data-driven reporting records iteration results, execution mode, workers, duration and estimated speedup. History and analytics provide run comparison, pass/fail totals, flaky-test detection and slow-test analysis.

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
