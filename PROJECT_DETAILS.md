# AI Testing Agent — Complete Project Documentation

## Current version

**3.24.0 — Phase 3 API Testing Enhancement**

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, JSON/CSV data-driven testing, environment profiles, assertions, retries, failure artifacts, HTML/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation and preflight validation.

## v3.24.0 API execution

`ApiExecutor` is the API runtime entry point. It now separates request construction, authentication, method dispatch, assertions, variable extraction, retries and failure evidence while retaining the existing `ExecutionResult` contract.

### Supported HTTP methods

`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `HEAD`, and `OPTIONS` are supported. Unsupported methods fail with an explicit diagnostic rather than silently falling back to another method.

### Request construction

`TestStep` now supports:

- `path` — relative or absolute HTTP(S) URL
- `query` — URL-encoded query parameters with variable substitution
- `headers` — substituted request headers
- `body` — raw request body with variable substitution
- `contentType` — explicit Content-Type; JSON is the default when a body is supplied without a type
- `form` — URL-encoded form fields
- `timeoutMs` — per-step timeout

Variables use `${name}` syntax and are resolved from plan variables or values saved from earlier responses.

### Authentication

`TestStep.auth` supports:

| Type | Fields | Behavior |
|---|---|---|
| `none` | — | No authentication added |
| `bearer` | `token` | Adds OAuth2/Bearer authentication |
| `basic` | `username`, `password` | Adds preemptive HTTP Basic authentication |
| `apiKeyHeader` | `key`, `value` | Sends the API key as a request header |
| `apiKeyQuery` | `key`, `value` | Sends the API key as a query parameter |

Authentication values are not intentionally included in the request log by the executor. Existing persisted-log redaction remains the final safety layer for sensitive diagnostics.

Example:

```json
{
  "action": "GET",
  "path": "/users/1",
  "auth": { "type": "bearer", "token": "${API_TOKEN}" },
  "assertions": [
    { "type": "status", "expected": "200" },
    { "type": "jsonPathExists", "path": "id", "expected": "true" }
  ]
}
```

### Assertions

`ApiAssertionEngine` supports legacy `assertSpec` plus typed `assertions`.

Typed assertion types:

- `status`
- `bodyContains`
- `bodyNotContains`
- `bodyRegex`
- `headerEquals`
- `jsonPathExists`
- `jsonPathEquals`
- `jsonPathContains`
- `jsonPathRegex`
- `xmlPathExists`
- `xmlPathEquals`
- `responseTimeMs`

Failures are returned as diagnostics and are included in the step result and failure artifact when retries are exhausted.

### Response variable extraction

`save` maps a variable name to a JSONPath. Successful extraction stores the value in the executor variable context for subsequent steps.

```json
"save": {
  "userId": "id"
}
```

A missing extraction value is treated as an execution failure rather than silently saving `null`.

### Retry behavior

`retryCount` overrides the configured global retry count for an individual step. The first request is attempt 1; `retryCount: 2` therefore allows up to 3 total attempts. Assertion failures and request exceptions both participate in the retry loop.

### API evidence

API request/response logs remain under `reports/api/logs/`. Failure artifacts contain the request URL/body, diagnostic details and bounded response information. Existing redaction is applied before persisted sensitive diagnostics are consumed by reporting/history/AI analysis.

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

API plans are under `examples/plans/api/` and include GET, POST, PUT, DELETE, query/header, negative-status, save-variable and retry scenarios. Phase 3 adds authentication, form and assertion patterns to the documented API model.

UI plans are under `examples/plans/ui/` and use Playwright with automatic screenshots after successful and failed steps.

Suites are under `examples/suites/` and data-driven plans under `examples/plans/data-driven/`.

## Running

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan examples/plans/api/get-user.json"
mvn exec:java -Dexec.args="suite examples/suites/api-regression-suite.json"
mvn exec:java -Dexec.args="data-driven examples/plans/data-driven/users-api.json examples/data/json/users.json --parallelism 3"
mvn exec:java -Dexec.args="validate plan examples/plans/api/get-user.json"
```

## Core classes

- `Main` — CLI entry point and durable terminal-log lifecycle.
- `AgentRunner` — AI planning, validation and execution coordination.
- `TestPlan` — test name, type, base URL, variables and steps.
- `TestStep` — API/UI action model, request data, authentication, retries and assertions.
- `ApiExecutor` — API request execution, retries, variable extraction and evidence.
- `ApiAssertionEngine` — API status/body/header/JSON/XML/performance assertions.
- `UiExecutor` — Playwright execution and automatic screenshot evidence.
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

Every release updates `pom.xml`, CLI/version metadata, `README.md`, `PROJECT_DETAILS.md`, `docs/releases/CHANGELOG.md`, relevant tests and examples/documentation. Phase 3 specifically added API model/runtime changes plus `ApiExecutorPhase3Test` regression coverage.
