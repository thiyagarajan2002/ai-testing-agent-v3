# AI Testing Agent — v3.17.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST ASSURED, Playwright, environment profiles, retries, assertions, failure artifacts, reporting, suite execution, history/analytics, security redaction, preflight validation, and data-driven execution.

## v3.17.0 — Data-Driven Reporting & HTML Dashboard

v3.17 adds reporting around the v3.16 data-driven runner. A single data-driven execution now produces an aggregate dashboard plus machine-readable JSON and CSV output. Failed iterations retain step diagnostics and AI failure analysis when available.

### Data-Driven Flow

```text
Plan JSON + Dataset JSON
          |
          v
    DataDrivenRunner
          |
   +------+------+ 
   |      |      |
 Row 1  Row 2  Row N
   |      |      |
   +------+------+ 
          |
          v
DataDrivenExecutionResult
          |
          v
DataDrivenReportManager
   |       |       |
 HTML    JSON     CSV
```

### Reports

Reports are written under:

```text
reports/data-driven/<safe-test-name>/
```

Files:

- `data-driven-report.html` — interactive browser-friendly dashboard.
- `data-driven-report.json` — complete aggregate and iteration structure.
- `data-driven-report.csv` — one row per iteration for spreadsheet/CI processing.

The HTML dashboard displays:

- overall PASS/FAIL status
- total, passed and failed iterations
- pass percentage
- total duration
- dataset values per iteration
- failed-row diagnostics
- step-level status, duration and details
- links to JSON and CSV reports

Sensitive dataset values are redacted before being rendered in CSV/HTML diagnostics.

## Data-Driven CLI

```bash
mvn exec:java -Dexec.args="data-driven examples/v3.16-data-driven-plan.json examples/v3.16-data.json"
```

With an environment profile:

```bash
mvn exec:java -Dexec.args="data-driven examples/v3.16-data-driven-plan.json examples/v3.16-data.json --env qa"
```

The CLI prints the generated report directory, for example:

```text
Reports: ./reports/data-driven/Get_user_-_data_driven/
```

## Data-Driven Dataset

Supported JSON formats:

```json
[
  {"userId":"1", "expectedName":"Leanne Graham"},
  {"userId":"2", "expectedName":"Ervin Howell"}
]
```

or:

```json
{
  "rows": [
    {"userId":"1", "expectedName":"Leanne Graham"},
    {"userId":"2", "expectedName":"Ervin Howell"}
  ]
}
```

Use `${data.key}` placeholders in plan fields including path, body, headers, query, variables, locators and assertions.

## Result Semantics

A data-driven execution passes only when every dataset iteration passes and at least one iteration exists. Each iteration contains its row index, row data, duration and original `ExecutionResult`.

Failed iterations are analyzed through the existing AI failure-analysis pipeline before the aggregate reports are generated. If AI analysis is unavailable, a safe fallback diagnostic is retained.

## Security

Do not put production credentials directly into plans or datasets. The centralized `SecurityRedactor` masks recognized passwords, tokens, API keys, authorization headers, cookies and other sensitive values before report diagnostics are exposed.

## Existing Commands

```bash
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file>"
mvn exec:java -Dexec.args="validate plan <file>"
mvn exec:java -Dexec.args="validate suite <file>"
mvn exec:java -Dexec.args="interactive"
```

## Build and Test

```bash
mvn clean verify
```

Playwright Chromium installation for UI execution:

```bash
mvn -DskipTests exec:java@playwright-cli -Dexec.args="install chromium"
```

## Architecture

```text
CLI (Main)
   |
   v
TestOrchestrator
   |
   +--> AgentRunner --------> Ollama
   |       |
   |       +---------------> API Executor / UI Executor
   |
   +--> DataDrivenRunner ---> isolated iteration execution
   |          |
   |          +-------------> DataDrivenReportManager
   |                         |--> HTML Dashboard
   |                         |--> JSON
   |                         +--> CSV
   |
   +--> SuiteExecutionEngine
   |
   +--> ReportManager / SuiteReportManager
   |
   +--> RunHistoryManager / HistoryAnalyticsManager
   |
   +--> TestPlanValidator
```

## Version History

- v3.17.0 — Data-driven reporting, HTML dashboard, JSON/CSV export and failed-row diagnostics
- v3.16.0 — Data-driven / parameterized test execution
- v3.15.0 — Preflight validation and dry-run validation
- v3.14.0 — History analytics and flaky-test analysis
- v3.13.0 — Execution history and run comparison enhancements
- v3.12.0 — Execution history foundations
- v3.11.0 — Test execution history and run comparison
- v3.10.0 — Secure secrets and sensitive-data redaction
- v3.9.0 — Advanced assertions and validation diagnostics
- v3.8.0 — Test data and environment profiles
- v3.7.0 — CI/CD and GitHub Actions integration
- v3.6.0 — Suite and parallel execution
- v3.5.0 — Advanced PDF/reporting
- v3.4.0 — Failure artifacts
- v3.3.0 — Advanced reporting
- v3.2.0 — Retry support

## Repository

urlAI Testing Agent v3 on GitHubhttps://github.com/thiyagarajan2002/ai-testing-agent-v3
