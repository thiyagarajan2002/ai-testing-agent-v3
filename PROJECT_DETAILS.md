# AI Testing Agent — Complete Project Documentation

## Current version

**3.25.0 — Phase 4 UI Testing Enhancement**

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, JSON/CSV data-driven testing, environment profiles, assertions, retries, failure artifacts, HTML/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation and preflight validation.

## v3.25.0 UI execution

`UiExecutor` is the Playwright UI runtime entry point. Phase 4 hardens browser/context lifecycle, navigation waiting, locator actions, assertions, explicit state waits and evidence handling while retaining the existing `ExecutionResult` contract.

### Browser lifecycle

Each UI execution creates a Playwright browser, an isolated browser context and a page. The context and browser are closed in `finally` blocks so resources are released even when a UI step fails.

### Navigation and waits

Navigation waits for `DOMContentLoaded` instead of relying on an arbitrary sleep. Locator-based actions use Playwright's built-in action waiting. Explicit state waits are available through `waitforvisible` and `waitforhidden`. The existing `waitfor` timed wait remains supported for backward compatibility, but event/state waits are preferred.

### Supported UI actions

`navigate`, `click`, `fill`, `press`, `selectOption`, `hover`, `check`, `uncheck`, `assertVisible`, `assertText`, `assertValue`, `assertTitle`, `assertUrl`, `waitFor`, `waitForVisible`, `waitForHidden`, and `screenshot` are supported.

Locator-based actions fail with a clear diagnostic when the locator is missing. Step-specific `timeoutMs` overrides the configured default timeout.

### UI assertions

- `assertvisible` verifies the target is visible.
- `asserttext` verifies the target text contains the expected value.
- `assertvalue` verifies an input value exactly.
- `asserttitle` verifies the page title contains the expected value.
- `asserturl` verifies the current URL contains the expected value.

### UI evidence

After every successful step, a full-page screenshot is attempted and attached to the corresponding `ExecutionResult.StepResult`. Failed steps capture a failure screenshot and failure metadata. Screenshot failures are deliberately non-fatal so evidence problems do not hide the original test failure.

### UI diagnostics and security

Exception text stored in execution results is passed through `SecurityRedactor`. Failure metadata is created through the existing `FailureArtifactManager`, preserving the project's persisted-evidence security model.

Example:

```json
{
  "name": "Login smoke",
  "type": "UI",
  "baseUrl": "https://example.com",
  "steps": [
    { "action": "navigate", "value": "/login" },
    { "action": "waitforvisible", "locator": "#username" },
    { "action": "fill", "locator": "#username", "value": "demo" },
    { "action": "fill", "locator": "#password", "value": "${PASSWORD}" },
    { "action": "click", "locator": "button[type=submit]" },
    { "action": "assertvisible", "locator": ".dashboard" },
    { "action": "asserttitle", "value": "Dashboard" }
  ]
}
```

## v3.24.0 API execution

`ApiExecutor` supports GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS, substituted path/query/header values, JSON/text bodies, content types, URL-encoded forms, bearer/basic/API-key authentication, status/body/header/JSONPath/XMLPath/response-time assertions, response-variable extraction and retries.

## v3.23.0 reporting integrity

The report contract is exactly:

```text
reports/<run>/
├── report.html
├── report.csv
└── report.pdf
```

`ReportIntegrityValidator` checks the exact file set, non-empty artifacts, HTML markers, UTF-8 BOM/summary in CSV and the PDF signature. HTML, CSV and PDF are generated from the same `ExecutionResult`.

## v3.22.0 stability and security

- `PARALLELISM` is restricted to 1–64.
- Runtime failures use `AgentExecutionException` categories.
- CLI exception handling keeps durable terminal logging active until resources close.
- Persisted terminal logs redact sensitive values and preserve UTF-8 output.
- Repository JSON validation allows arrays under `examples/data/` while executable plan/suite JSON remains object-only.

## Examples

API plans are under `examples/plans/api/` and include GET, POST, PUT, DELETE, query/header, negative-status, save-variable, authentication and assertion scenarios.

UI plans are under `examples/plans/ui/` and use Playwright with automatic screenshots after successful and failed steps. New UI plans should prefer `waitforvisible`/`waitforhidden` over fixed `waitfor` sleeps.

Suites are under `examples/suites/` and data-driven plans under `examples/plans/data-driven/`.

## Running

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan examples/plans/api/get-user.json"
mvn exec:java -Dexec.args="plan examples/plans/ui/login-flow.json"
mvn exec:java -Dexec.args="suite examples/suites/api-regression-suite.json"
mvn exec:java -Dexec.args="data-driven examples/plans/data-driven/users-api.json examples/data/json/users.json --parallelism 3"
mvn exec:java -Dexec.args="validate plan examples/plans/ui/login-flow.json"
```

Install Chromium when required:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

## Core classes

- `Main` — CLI entry point and durable terminal-log lifecycle.
- `AgentRunner` — AI planning, validation and execution coordination.
- `TestPlan` — test name, type, base URL, variables and steps.
- `TestStep` — API/UI action model, request data, authentication, retries and assertions.
- `ApiExecutor` — API request execution, retries, variable extraction and evidence.
- `ApiAssertionEngine` — API status/body/header/JSON/XML/performance assertions.
- `UiExecutor` — Playwright browser/context/page execution, waits, actions, assertions and screenshots.
- `SuiteExecutionEngine` — suite loading, validation, parallel workers and aggregation.
- `DataDrivenRunner` — dataset-driven execution and metrics.
- `ReportManager` — unified HTML/CSV/PDF report generation.
- `ReportIntegrityValidator` — report-package quality gate.
- `HistoryAnalyticsManager` — run history, reliability and flaky/slow analysis.
- `SecurityRedactor` — sensitive-value protection across persisted diagnostics.

## Reporting

HTML provides the interactive dashboard, CSV provides machine-readable execution data, and PDF provides the printable report. Runtime logs/screenshots remain evidence and are referenced from step artifacts rather than becoming additional report formats.

## CI/CD

GitHub Actions performs Java 21 setup, Maven verification, example validation/execution, Playwright installation and artifact handling. A release is only described as CI/build verified after the corresponding workflow actually completes successfully.

## Technology stack

- Java 21
- Maven
- Ollama
- REST Assured 5.5.6
- Playwright 1.55.0
- Jackson 2.20.0
- JUnit 5
- Apache Commons CSV
- iText 9.3.0
- SLF4J
- GitHub Actions

## Release documentation rule

Every release updates `pom.xml`, CLI/version metadata, `README.md`, `PROJECT_DETAILS.md`, `docs/releases/CHANGELOG.md`, relevant tests and examples/documentation.
