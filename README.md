# AI Testing Agent — v3.25.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, retries, artifacts, reporting, suites, history/analytics, security redaction, preflight validation and data-driven execution.

## v3.25.0 — Phase 4 UI Testing Enhancement

Phase 4 strengthens Playwright execution with reliable lifecycle handling, event-oriented waiting and broader UI actions/assertions.

- **Browser lifecycle:** explicit browser context creation and guaranteed context/browser cleanup.
- **Navigation:** waits for `DOMContentLoaded` instead of using a fixed sleep after navigation.
- **Locator execution:** locator-required actions fail clearly when a locator is missing.
- **UI actions:** click, fill, press, selectOption, hover, check and uncheck.
- **Assertions:** visible, text, input value, page title and URL.
- **Event-oriented waits:** wait for locator visibility/hidden state; the legacy timed `waitfor` action remains available for compatibility.
- **Timeouts:** plan-level default timeout remains available and each step can override it.
- **Evidence:** successful steps receive full-page screenshots; failed steps receive failure screenshots and metadata.
- **Diagnostics:** UI exceptions are redacted before being placed in execution results.
- **Regression safety:** the implementation keeps the existing `ExecutionResult`, report and artifact integration intact.

### UI action example

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

## v3.24.0 — Phase 3 API Testing Enhancement

- HTTP methods: GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS.
- Request construction with substituted path/query/header values, JSON/text bodies, content types and URL-encoded forms.
- Bearer, basic, API-key-header and API-key-query authentication.
- Status, body, header, JSONPath, XMLPath and response-time assertions.
- Response-variable extraction, retries, diagnostics and secure logging.

## v3.23.0 — Phase 2 Reporting Standardization & Integrity

- Every execution report contains exactly `report.html`, `report.csv` and `report.pdf`.
- HTML, CSV and PDF derive core execution data from the same `ExecutionResult`.
- `ReportIntegrityValidator` validates the report package and regression coverage protects incomplete packages.

## v3.22.0 — Phase 1 Stability & Security

- `PARALLELISM` restricted to 1–64.
- Standardized runtime error categories.
- Durable CLI logging and secure UTF-8 redaction.
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

- **v3.25.0** — Phase 4 UI testing enhancement: Playwright lifecycle, event-oriented waits, actions, assertions and evidence hardening
- **v3.24.0** — Phase 3 API testing enhancement
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
