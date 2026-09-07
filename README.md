# AI Testing Agent — v3.18.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, retries, artifacts, reporting, suites, history/analytics, security redaction, preflight validation and data-driven execution.

## v3.18.0 — Advanced Data-Driven Dataset Management

v3.18 adds **CSV dataset support, dataset structure validation and row filtering** while preserving the existing JSON format.

### Supported datasets

JSON array:
```json
[{"userId":"1","expectedName":"Leanne Graham"}]
```

JSON object:
```json
{"rows":[{"userId":"1","expectedName":"Leanne Graham"}]}
```

CSV:
```csv
userId,expectedName
1,Leanne Graham
2,Ervin Howell
```

CSV headers must be non-blank and unique. Every row must contain the same number of columns as the header. Empty datasets are rejected.

### CLI

```bash
mvn exec:java -Dexec.args="data-driven examples/v3.16-data-driven-plan.json examples/users.csv"
```

Filter execution to matching dataset rows:

```bash
mvn exec:java -Dexec.args="data-driven examples/v3.16-data-driven-plan.json examples/users.csv --filter userId=2"
```

Multiple filters are supported:

```bash
mvn exec:java -Dexec.args="data-driven plan.json users.csv --filter country=IN --filter active=true"
```

Filters use exact string equality. If no rows match, execution fails with `PLAN_VALIDATION` rather than silently producing a zero-iteration pass.

### Data placeholders

Use `${data.key}` in plan name, base URL, variables, path, locator, value, body, headers, query, saved variables and assertions.

### Reporting

Data-driven executions continue to generate HTML, JSON and CSV reports under:

```text
reports/data-driven/<safe-test-name>/
```

Sensitive dataset values are redacted before report exposure.

## Architecture

```text
CLI
 |
 v
TestOrchestrator
 |
 +--> DataDrivenRunner
 |       |
 |       +--> DataDrivenDatasetReader --> JSON / CSV
 |       +--> row filters
 |       +--> isolated TestPlan execution
 |       +--> DataDrivenReportManager --> HTML / JSON / CSV
 +--> AgentRunner --> API / Playwright
 +--> SuiteExecutionEngine
 +--> History / Analytics
 +--> TestPlanValidator
```

## Commands

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file> [--filter key=value] [--env <name>]"
mvn exec:java -Dexec.args="validate plan <file>"
mvn exec:java -Dexec.args="validate suite <file>"
mvn exec:java -Dexec.args="interactive"
```

Install Chromium when UI execution is required:
```bash
mvn -DskipTests exec:java@playwright-cli -Dexec.args="install chromium"
```

## Security

Do not store production credentials in datasets. `SecurityRedactor` masks passwords, tokens, API keys, authorization headers, cookies and related sensitive values in report diagnostics.

## Version history

- **v3.18.0** — CSV datasets, dataset validation and exact row filtering
- v3.17.0 — Data-driven reporting and HTML dashboard
- v3.16.0 — Data-driven / parameterized test execution
- v3.15.0 — Preflight validation
- v3.14.0 — History analytics and flaky-test analysis
- v3.13.0 — Execution history and reliability improvements
- v3.12.0 — Execution history foundations
- v3.11.0 — Test execution history and run comparison
- v3.10.0 — Sensitive-data redaction
- v3.9.0 — Advanced assertions
- v3.8.0 — Environment profiles and test data
- v3.7.0 — GitHub Actions CI/CD
- v3.6.0 — Suite and parallel execution
- v3.5.0 — Advanced PDF/reporting
- v3.4.0 — Failure artifacts
- v3.3.0 — Advanced reporting
- v3.2.0 — Retry support

## Repository

urlAI Testing Agent v3 on GitHubhttps://github.com/thiyagarajan2002/ai-testing-agent-v3
