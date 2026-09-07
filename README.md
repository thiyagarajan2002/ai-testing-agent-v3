# AI Testing Agent — v3.16.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, Playwright, environment profiles, retries, assertions, failure artifacts, advanced reports, suite execution, history/analytics, security redaction, preflight validation, and data-driven execution.

## v3.16.0 — Data-Driven Test Execution

v3.16 adds parameterized test execution. A single API/UI test plan can now be executed repeatedly against a JSON dataset, with each row isolated from the next iteration.

### Features

- `DataDrivenRunner` executes one plan per data row.
- JSON dataset formats supported:
  - a root array: `[{...},{...}]`
  - an object containing `rows`: `{ "rows": [{...},{...}] }`
- `${data.key}` placeholders can be used in:
  - test name
  - base URL
  - variables
  - path
  - locator
  - value
  - request body
  - headers
  - query parameters
  - saved variables
  - legacy assertions
  - v3.9 typed assertions
- Each iteration receives a deep copy of the original plan, preventing data mutation between iterations.
- Aggregate result reports total, passed, failed iterations and total duration.
- Sensitive values continue to pass through the existing redaction layer.
- Non-data-driven `plan` and `suite` commands remain unchanged.

## Data-Driven CLI

```bash
mvn exec:java -Dexec.args="data-driven examples/v3.16-data-driven-plan.json examples/v3.16-data.json"
```

With an environment profile:

```bash
mvn exec:java -Dexec.args="data-driven examples/v3.16-data-driven-plan.json examples/v3.16-data.json --env qa"
```

Example dataset:

```json
{
  "rows": [
    {"userId":"1", "expectedName":"Leanne Graham"},
    {"userId":"2", "expectedName":"Ervin Howell"}
  ]
}
```

Example plan:

```json
{
  "name": "Get user - data driven",
  "type": "API",
  "baseUrl": "https://jsonplaceholder.typicode.com",
  "steps": [
    {
      "action": "GET",
      "path": "/users/${data.userId}",
      "assertions": [
        {"type":"status", "expected":"200"},
        {"type":"bodyContains", "expected":"${data.expectedName}"}
      ]
    }
  ]
}
```

## Result Semantics

A data-driven execution passes only when every dataset iteration passes. Individual iteration results retain their original `ExecutionResult`, making failures traceable to the corresponding row/index.

## Validation

The existing v3.15 preflight commands remain available:

```bash
mvn exec:java -Dexec.args="validate plan examples/v3-plan-file.json"
mvn exec:java -Dexec.args="validate suite examples/v3-suite.json"
```

## Build and Test

```bash
mvn clean verify
```

Playwright Chromium installation for UI execution:

```bash
mvn -DskipTests exec:java@playwright-cli -Dexec.args="install chromium"
```

## Version History

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
   +--> DataDrivenRunner ---> repeated isolated TestPlan executions
   |
   +--> SuiteExecutionEngine
   |
   +--> ReportManager / SuiteReportManager
   |
   +--> RunHistoryManager / HistoryAnalyticsManager
   |
   +--> TestPlanValidator
```

## Security

Do not place production credentials directly in plans or datasets. Use environment variables/environment profiles where appropriate. The centralized `SecurityRedactor` masks recognized passwords, tokens, API keys, authorization headers, cookies, and other sensitive values before they are written to reports or supplied to failure analysis.

## Repository

urlAI Testing Agent v3 on GitHubhttps://github.com/thiyagarajan2002/ai-testing-agent-v3
