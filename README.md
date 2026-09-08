# AI Testing Agent — v3.23.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, retries, artifacts, reporting, suites, history/analytics, security redaction, preflight validation and data-driven execution.

## v3.23.0 — Phase 2 Reporting Standardization & Integrity

Phase 2 strengthens the unified reporting package established in v3.21/v3.22:

- **Single report package contract:** every execution report is expected to contain exactly `report.html`, `report.csv` and `report.pdf`.
- **Common execution source:** HTML, CSV and PDF continue to derive their core status, step, duration, detail, artifact and failure-analysis data from the same `ExecutionResult`.
- **HTML dashboard:** responsive interactive dashboard with KPI metrics, outcome visualization, latency profile, search/filter/sort, expandable step details, artifacts, theme toggle and print support.
- **CSV export:** UTF-8 BOM, machine-readable execution rows and summary metrics for downstream processing.
- **PDF export:** professional color-coded printable report with execution summary, performance profile, step results, details/artifacts and failure analysis.
- **Package integrity validation:** `ReportIntegrityValidator` verifies the exact three-file package, non-empty outputs, HTML package markers, CSV BOM/summary and PDF signature.
- **Regression coverage:** report-package validation covers complete, incomplete, unexpected-file and invalid-PDF scenarios.

The validator provides a deterministic quality gate for report artifacts and is intended to prevent incomplete report bundles from being treated as successful output.

## v3.22.0 — Phase 1 Stability & Security

Phase 1 hardens the execution foundation before new feature work:

- **Build stability:** Java 21/Maven compilation and unit-test gates remain mandatory in CI.
- **Configuration validation:** `PARALLELISM` is restricted to 1–64.
- **Standardized errors:** runtime failures use `AgentExecutionException` categories, and invalid/null categories are rejected.
- **CLI lifecycle safety:** execution exceptions are captured inside the durable-log scope, stack traces are preserved in the log, and the process exits only after log resources are closed.
- **Secure terminal logging:** stdout/stderr remain visible while persisted terminal logs redact credentials and sensitive values.
- **UTF-8-safe logging:** redaction buffers complete log lines so Unicode output is preserved correctly.
- **Regression coverage:** configuration bounds, persisted-log redaction, UTF-8 output and execution-result serialization are covered by tests.
- **CI validation:** repository JSON fixtures accept either objects or arrays under `examples/data/`, while executable plan/suite JSON remains object-only.

## v3.21.0 — Examples, UI Evidence & Durable Logs

This release adds a broader example library and stronger execution evidence.

### API examples

```text
examples/plans/api/
├── v3-plan-file.json
├── get-user.json
├── create-resource.json
└── save-variable.json
```

API suite: `examples/suites/api-smoke-suite.json`

Run:

```bash
mvn exec:java -Dexec.args="plan examples/plans/api/get-user.json"
```

Every API test creates a durable request/response log under:

```text
reports/api/logs/<test-name>-<timestamp>.log
```

The log contains timestamp, step, attempt, action, URL, request body, response status, duration, bounded response body and errors. Sensitive values are redacted before persistence.

### UI examples

```text
examples/plans/ui/
├── homepage-smoke.json
├── login-flow.json
├── search-flow.json
└── negative-login.json
```

UI suite: `examples/suites/ui-smoke-suite.json`

Run:

```bash
mvn exec:java -Dexec.args="plan examples/plans/ui/login-flow.json"
```

Install Chromium first when needed:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

### Automatic screenshot after every UI step

Every successful UI step automatically captures a full-page screenshot. Failed UI steps capture a failure screenshot too.

```text
reports/screenshots/ui/
├── <test-name>/
│   ├── 001-navigate.png
│   ├── 002-fill.png
│   ├── 003-click.png
│   └── 004-assertvisible.png
└── failures/
    └── <test-name>-step-4-failure.png
```

The screenshot path is attached to the step result. The existing explicit `screenshot` action remains supported for compatibility.

### Terminal run logs

The application tees stdout/stderr: output remains visible in the terminal and is simultaneously persisted to a timestamped log. Persisted logs are redacted for known credential patterns and sensitive environment values.

## Unified execution reports

Each execution report bundle contains exactly these three report formats:

```text
reports/<run>/
├── report.html
├── report.csv
└── report.pdf
```

All three are generated from the same `ExecutionResult`, so test status, step counts, durations, details, artifacts and failure analysis stay consistent across formats.

- **HTML** — interactive dashboard with KPI cards, search, PASS/FAIL filtering, slow-step filtering, sortable step table, expandable details, theme toggle, print support and links to CSV/PDF.
- **CSV** — UTF-8 BOM/RFC4180-compatible execution rows plus summary metrics such as overall status, totals, pass rate, duration, min/max/average latency and failure analysis.
- **PDF** — printable execution summary with pass/fail metrics, duration statistics, step-by-step details, input/output text, artifact references and failure analysis.

API request/response logs, terminal logs and UI screenshots remain separate runtime evidence. When they are attached to a step, the reports include the artifact reference rather than creating additional report formats.

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

## Repository layout

```text
ai-testing-agent-v3/
├── .github/                 # CI/CD
├── config/                  # Environment profiles and reusable data
├── docs/                    # Architecture, configuration, testing and releases
├── examples/                # API/UI plans, suites, datasets and requirements
├── scripts/                 # Developer/CI helpers
├── src/main/java/           # Production Java source
├── src/main/resources/      # Runtime resources
├── src/test/java/           # Automated tests
├── src/test/resources/      # Test fixtures
├── reports/                 # Generated runtime evidence
├── pom.xml
├── README.md
├── PROJECT_DETAILS.md
├── CONTRIBUTING.md
└── SECURITY.md
```

## Documentation

- `PROJECT_DETAILS.md` — complete implementation, methods, execution behavior and release history.
- `docs/architecture/` — architecture and package boundaries.
- `docs/configuration/` — configuration guidance.
- `docs/testing/` — testing standards.
- `docs/releases/` — release history.
- `examples/README.md` — API/UI examples and evidence/logging guide.

## Security

Do not store production credentials in plans or datasets. Sensitive execution diagnostics, API logs, terminal logs and AI failure-analysis input are redacted before persistence.

## Version history

- **v3.23.0** — Phase 2 reporting standardization, color-coded HTML/CSV/PDF contract and report package integrity validation
- **v3.22.0** — Phase 1 stability, configuration bounds, standardized error handling, secure UTF-8 terminal logging and regression coverage
- v3.21.0 — comprehensive API/UI examples, automatic UI screenshots, API execution logs, terminal log persistence and unified HTML/CSV/PDF execution reports
- v3.20.0 — repository organization and engineering standards
- v3.19.0 — parallel data-driven execution, ordered results and performance metrics
- v3.18.0 — CSV datasets, dataset validation and exact row filtering
- v3.17.0 — data-driven reporting and HTML dashboard
- v3.16.0 — data-driven / parameterized test execution
- v3.15.0 — preflight validation
- v3.14.0 — history analytics and flaky-test analysis
- v3.13.0 — execution history and reliability improvements
- v3.12.0 — execution history foundations
- v3.11.0 — execution history and run comparison
- v3.10.0 — sensitive-data redaction
- v3.9.0 — advanced assertions
- v3.8.0 — environment profiles and test data
- v3.7.0 — GitHub Actions CI/CD
- v3.6.0 — suite and parallel execution
- v3.5.0 — advanced PDF/reporting
- v3.4.0 — failure artifacts
- v3.3.0 — advanced reporting
- v3.2.0 — retry support

## Repository

https://github.com/thiyagarajan2002/ai-testing-agent-v3
