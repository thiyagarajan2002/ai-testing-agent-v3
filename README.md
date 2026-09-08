# AI Testing Agent — v3.24.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, retries, artifacts, reporting, suites, history/analytics, security redaction, preflight validation and data-driven execution.

## v3.24.0 — Phase 3 API Testing Enhancement

Phase 3 expands the API execution layer into a more complete HTTP testing engine:

- **HTTP methods:** GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS.
- **Request construction:** substituted path/query/header values, JSON/text request bodies, configurable `Content-Type` and `application/x-www-form-urlencoded` form data.
- **Authentication:** bearer token, preemptive basic authentication, API key in a header, and API key in a query parameter.
- **Assertions:** status, body contains/not-contains/regex, header equality, JSONPath exists/equality/contains/regex, XMLPath exists/equality and response-time limits.
- **Variable reuse:** response JSON extraction can save values for later steps.
- **Retries:** global or step-specific retry counts continue to work with the enhanced request pipeline.
- **Diagnostics:** request/response logs, bounded response evidence and failure artifacts remain integrated.
- **Security:** authentication values are not explicitly written into request logs; persisted diagnostics continue through the existing redaction pipeline.
- **Regression coverage:** local HTTP-server tests cover bearer authentication, JSON requests, form requests and API assertions.

### API authentication example

```json
{
  "action": "GET",
  "path": "/users/1",
  "auth": {
    "type": "bearer",
    "token": "${API_TOKEN}"
  },
  "assertions": [
    { "type": "status", "expected": "200" },
    { "type": "jsonPathExists", "path": "id", "expected": "true" }
  ]
}
```

Supported `auth.type` values are `none`, `bearer`, `basic`, `apiKeyHeader`, and `apiKeyQuery`.

### Request body example

```json
{
  "action": "POST",
  "path": "/users",
  "contentType": "application/json",
  "body": "{\"name\":\"${userName}\"}",
  "assertSpec": { "status": 201 }
}
```

For URL-encoded forms use:

```json
{
  "action": "POST",
  "path": "/login",
  "form": { "username": "${username}", "password": "${password}" }
}
```

## v3.23.0 — Phase 2 Reporting Standardization & Integrity

- Every execution report contains exactly `report.html`, `report.csv` and `report.pdf`.
- HTML, CSV and PDF derive their core execution data from the same `ExecutionResult`.
- `ReportIntegrityValidator` validates the exact package, non-empty outputs, HTML markers, CSV BOM/summary and PDF signature.
- Regression coverage protects complete, incomplete, unexpected-file and invalid-PDF report packages.

## v3.22.0 — Phase 1 Stability & Security

- `PARALLELISM` is restricted to 1–64.
- Runtime failures use standardized `AgentExecutionException` categories.
- CLI exception handling preserves durable logs and stack traces before process exit.
- Persisted terminal logs redact sensitive values and preserve UTF-8 output.
- JSON fixture validation accepts arrays under `examples/data/` while executable JSON remains object-only.

## Unified execution reports

```text
reports/<run>/
├── report.html
├── report.csv
└── report.pdf
```

Runtime API logs, terminal logs and UI screenshots remain separate evidence and can be referenced by report step artifacts.

## API examples

```text
examples/plans/api/
├── v3-plan-file.json
├── get-user.json
├── create-resource.json
├── save-variable.json
├── 02-post.json
├── 03-put.json
├── 04-delete.json
├── 05-query-headers.json
└── 06-negative-status.json
```

Run an API plan:

```bash
mvn exec:java -Dexec.args="plan examples/plans/api/get-user.json"
```

Run API regression:

```bash
mvn exec:java -Dexec.args="suite examples/suites/api-regression-suite.json"
```

## UI examples

```text
examples/plans/ui/
├── homepage-smoke.json
├── login-flow.json
├── search-flow.json
└── negative-login.json
```

Run:

```bash
mvn exec:java -Dexec.args="plan examples/plans/ui/login-flow.json"
```

Install Chromium when required:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

Successful UI steps automatically capture screenshots; failed UI steps capture failure evidence.

## Core commands

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]"
mvn exec:java -Dexec.args="validate plan <file>"
mvn exec:java -Dexec.args="validate suite <file>"
mvn exec:java -Dexec.args="interactive"
```

## Documentation

- `PROJECT_DETAILS.md` — complete implementation and method-level behavior.
- `docs/architecture/` — architecture and package boundaries.
- `docs/configuration/` — configuration guidance.
- `docs/testing/` — testing standards.
- `docs/releases/` — release history.
- `examples/README.md` — API/UI examples and evidence guide.

## Security

Do not store production credentials in plans or datasets. Authentication and sensitive execution diagnostics are protected by the existing redaction/logging pipeline.

## Version history

- **v3.24.0** — Phase 3 API testing enhancement: HTTP methods, authentication, request bodies/content types/forms and JSON/XML assertions
- **v3.23.0** — Phase 2 reporting standardization and report package integrity validation
- **v3.22.0** — Phase 1 stability, configuration bounds, standardized errors and secure logging
- v3.21.0 — API/UI examples, screenshots, execution logs and unified reports
- v3.20.0 — repository organization and engineering standards
- v3.19.0 — parallel data-driven execution and performance metrics
- v3.18.0 — CSV datasets and validation
- v3.17.0 — data-driven reporting
- v3.16.0 — data-driven execution
- v3.15.0 — preflight validation
- v3.14.0 — history analytics
- v3.13.0 — execution history
- v3.12.0 — execution history foundations
- v3.11.0 — run comparison
- v3.10.0 — sensitive-data redaction
- v3.9.0 — advanced assertions
- v3.8.0 — environment profiles
- v3.7.0 — GitHub Actions CI/CD
- v3.6.0 — suite and parallel execution
- v3.5.0 — advanced reporting
- v3.4.0 — failure artifacts
- v3.3.0 — advanced reporting
- v3.2.0 — retry support

## Repository

https://github.com/thiyagarajan2002/ai-testing-agent-v3
