# AI Testing Agent — v3.19.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, retries, artifacts, reporting, suites, history/analytics, security redaction, preflight validation and data-driven execution.

## v3.19.0 — Data-Driven Parallel Execution & Performance

v3.19 adds configurable parallel execution for data-driven rows while preserving dataset order in reports. It also records measured iteration work, wall-clock duration and an approximate speedup metric.

### Parallel execution

The existing `PARALLELISM` configuration controls the default worker count for data-driven execution. Use `--parallelism` to override it for one run. Supported values are `1` through `64`.

```bash
mvn exec:java -Dexec.args="data-driven examples/v3.16-data-driven-plan.json examples/users.csv --parallelism 4"
```

`--parallelism 1` forces sequential execution. Values greater than the number of selected rows are automatically reduced to the row count.

### Filters + parallelism

```bash
mvn exec:java -Dexec.args="data-driven plan.json users.csv --filter country=IN --filter active=true --parallelism 8"
```

Filters are applied before worker creation, so only matching rows consume execution workers.

### Performance metrics

Every data-driven result records:

- `executionMode` — `SEQUENTIAL` or `PARALLEL`.
- `parallelism` — actual worker count used.
- `durationMs` — total wall-clock duration.
- `estimatedSequentialDurationMs` — sum of measured iteration durations.
- `estimatedSpeedup` — estimated sequential work divided by wall-clock duration.
- `averageIterationDurationMs` — average measured iteration duration.

The speedup is an approximation, not a benchmark guarantee, because it includes test-system, network, browser, scheduling and reporting effects.

### Isolation and ordering

Each worker deep-copies the template before substituting `${data.key}` values. Iterations may finish in any order, but the final result and reports are restored to original dataset order. API/UI executor state is created per test execution by `AgentRunner`.

## v3.18.0 — Advanced Data-Driven Dataset Management

v3.18 added CSV dataset support, dataset structure validation and row filtering while preserving JSON datasets.

Supported datasets:

```json
[{"userId":"1","expectedName":"Leanne Graham"}]
```

```json
{"rows":[{"userId":"1","expectedName":"Leanne Graham"}]}
```

```csv
userId,expectedName
1,Leanne Graham
2,Ervin Howell
```

CSV headers must be non-blank and unique. Every row must contain the same number of columns as the header. Empty datasets are rejected.

## Data placeholders

Use `${data.key}` in plan name, base URL, variables, path, locator, value, body, headers, query, saved variables and assertions.

## Reporting

Data-driven executions generate HTML, JSON and CSV reports under:

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
 |       +--> sequential OR bounded parallel workers
 |       +--> ordered aggregate + performance metrics
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
mvn exec:java -Dexec.args="data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]"
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

- **v3.19.0** — Parallel data-driven execution, ordered results and performance metrics
- v3.18.0 — CSV datasets, dataset validation and exact row filtering
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
