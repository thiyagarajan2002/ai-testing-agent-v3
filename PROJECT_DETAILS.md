# AI Testing Agent — Complete Project Documentation

## 1. Project overview

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, JSON/CSV data-driven testing, environment profiles, assertions, retries, failure artifacts, HTML/JSON/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation, and non-executing preflight validation.

**Current version: 3.19.0**

## 2. Technology stack

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

## 3. Repository structure

```text
ai-testing-agent-v3/
├── .github/workflows/ci.yml
├── config/environments/
├── config/test-data/
├── examples/
├── scripts/ci/
├── src/main/java/com/thiyagarajan/agent/
│   ├── Main.java
│   ├── ai/
│   ├── config/
│   ├── model/
│   ├── report/
│   └── runtime/
└── src/test/java/com/thiyagarajan/agent/
```

## 4. Main commands

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]"
mvn exec:java -Dexec.args="validate plan <file>"
mvn exec:java -Dexec.args="validate suite <file>"
mvn exec:java -Dexec.args="interactive"
```

Install Chromium for UI execution:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

## 5. Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless mode |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default execution timeout |
| `RETRIES` | `0` | Default API retry count |
| `PARALLELISM` | `4` | Suite and default data-driven worker count |
| `REPORTS_DIR` | `reports` | Report/history root |
| `SCREENSHOTS_DIR` | `screenshots` | Failure artifact root |

`Config` validates URLs, model name, timeout, retries, parallelism and output directories. Invalid integer configuration values fail explicitly.

## 6. Core model and execution classes

### `TestPlan`

Contains `name`, `type` (`API`/`UI`), `baseUrl`, `variables`, and `steps`.

### `TestStep`

Supports API method/path, headers, query parameters, body, timeout, retry count, assertions, saved variables, and UI locator/value fields.

### `AgentRunner`

- `plan(requirement)` generates and validates an AI test plan.
- `execute(plan)` validates and executes API/UI plans.
- `executeSuite(...)` provides backward-compatible sequential suite execution.
- `analyzeFailure(plan,result)` sends redacted diagnostics to Ollama.

Each `execute(plan)` call creates the API or UI executor for that test, keeping executor state isolated.

### `TestOrchestrator`

Coordinates file loading, environment application, plan/suite/data-driven execution, reporting, validation and history. Important methods include `executePlan`, `executeSuite`, `executeDataDriven`, `validatePlan`, `validateSuite`, `plan`, `execute`, `analyzeIfFailed`, and `writeReport`.

## 7. Preflight validation

`TestPlanValidator` is shared by CLI validation and runtime execution. It validates plan type, base URL, steps, actions, timeout and retry settings and produces errors/warnings.

`validate plan` and `validate suite` do not send API requests, launch browsers, contact Ollama or execute tests.

Suite validation also checks referenced files, rejects blank paths, blocks paths escaping the suite directory and validates each referenced plan.

Exit codes:

```text
0 = successful
1 = test/validation failure
2 = configuration/infrastructure error
```

## 8. Data-driven execution

### Dataset formats

JSON root array:

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

`DataDrivenDatasetReader` validates file existence, JSON structure, CSV headers, duplicate headers, row column counts and non-empty datasets.

### Placeholders

`${data.key}` can be used in plan name, base URL, variables, path, locator, value, body, headers, query, saved variables and assertions.

### Filtering

`--filter key=value` performs exact string matching. Multiple filters are ANDed. Filtering occurs before worker creation. No matching rows is a `PLAN_VALIDATION` failure.

## 9. v3.19.0 — Data-driven parallel execution & performance

`DataDrivenRunner.execute(template,dataFile,filters,parallelism)` supports bounded parallel execution using a fixed worker pool.

Rules:

- `1` worker means sequential execution.
- `2–64` workers enable bounded parallel execution.
- Values above the selected row count are reduced to the row count.
- Invalid values outside `1–64` are rejected.
- Every iteration receives a deep-copied `TestPlan` before data substitution.
- Iterations execute independently through `AgentRunner`.
- Worker completion order does not affect report order; results are restored to original dataset order.
- The worker pool is shut down in a `finally` block.
- Interrupted callers restore the interrupt flag.

CLI example:

```bash
mvn exec:java -Dexec.args="data-driven plan.json users.csv --parallelism 8"
```

Override the configured `PARALLELISM` only for one data-driven run with `--parallelism N`.

### Performance metrics

`DataDrivenExecutionResult` now records:

- `executionMode` — `SEQUENTIAL` or `PARALLEL`.
- `parallelism` — actual worker count.
- `durationMs` — wall-clock duration.
- `estimatedSequentialDurationMs` — sum of measured iteration durations.
- `estimatedSpeedup` — estimated sequential work divided by wall-clock duration.
- `averageIterationDurationMs()` — average measured iteration duration.

Speedup is an estimate, not a benchmark guarantee; network latency, browsers, scheduling, test infrastructure and report generation affect the result.

## 10. Reporting

`DataDrivenReportManager` writes:

```text
reports/data-driven/<safe-test-name>/data-driven-report.html
reports/data-driven/<safe-test-name>/data-driven-report.json
reports/data-driven/<safe-test-name>/data-driven-report.csv
```

The dashboard includes iteration totals, pass rate, wall duration, mode, worker count, average iteration duration, estimated sequential work, estimated speedup and per-iteration diagnostics.

JSON dataset values are sanitized through `SecurityRedactor`; CSV/HTML dataset values are also redacted and escaped.

The general reporting layer supports JSON, CSV, HTML and PDF execution reports. Suite reporting aggregates test results.

## 11. API execution

API execution builds requests from plan values, applies headers/query/body, evaluates assertions, saves configured response values, retries according to retry policy and creates failure evidence.

Supported advanced assertions include status, body contains/not-contains/regex, header equality, JSONPath exists/equality/contains/regex, response time, plus legacy `assertSpec` assertions.

## 12. UI execution

Playwright execution supports navigation and actions/assertions including `navigate`, `click`, `fill`, `press`, `selectOption`, `assertVisible`, `assertText`, `assertValue`, `waitFor`, and `screenshot`.

## 13. Retry behavior

A step-level `retryCount` overrides global `RETRIES`. Negative retry counts are rejected.

## 14. Failure artifacts

Failed API/UI executions can produce screenshots and API evidence under configured artifact directories. Sensitive metadata is redacted before persistence and artifact filenames are sanitized.

## 15. Security redaction

`SecurityRedactor` masks passwords, secrets, tokens, access tokens, API keys, client secrets, authorization headers, cookies and related sensitive values. Redaction is applied to execution diagnostics, reports, failure artifacts, history/analytics and AI failure-analysis prompts.

Production credentials must not be placed in datasets.

## 16. Suite execution

`SuiteExecutionEngine` loads multiple plans, uses configurable parallelism, isolates test execution state, aggregates results and continues after an individual test failure. It validates suite-relative paths to prevent traversal and safely shuts down worker resources.

## 17. Execution history and analytics

`RunHistoryManager` stores suite runs under:

```text
reports/history/<run-id>/
```

Stable identity is `planFile + "::" + testName`.

Comparison categories include `REGRESSION`, `FIXED`, `PASSED_UNCHANGED`, `FAILED_UNCHANGED`, `NEW_TEST`, and `REMOVED_TEST`.

`HistoryAnalyticsManager` calculates recent execution/pass/failure totals, detects flaky tests and identifies slow tests, producing JSON/CSV/HTML analytics reports.

## 18. Standardized errors

`AgentExecutionException` categorizes failures as:

```text
CONFIGURATION
PLAN_VALIDATION
SUITE_VALIDATION
API_EXECUTION
UI_EXECUTION
ASSERTION
AI_GENERATION
REPORTING
INFRASTRUCTURE
```

## 19. CI/CD

GitHub Actions performs Java 21 setup, compile, Playwright/Chromium installation, Maven verification, example execution/validation and report/artifact upload. Compilation occurs before the Playwright CLI is invoked so a clean runner has the required classes available.

## 20. Version history

- v3.19.0 — bounded parallel data-driven execution, ordered results, performance metrics and CLI worker override
- v3.18.0 — CSV datasets, dataset validation and exact row filtering
- v3.17.0 — data-driven reporting and HTML dashboard
- v3.16.0 — data-driven / parameterized execution
- v3.15.0 — preflight validation and dry-run checks
- v3.14.0 — history analytics and flaky-test analysis
- v3.13.0 — execution reliability and standardized errors
- v3.12.0 — execution history foundations
- v3.11.0 — history and run comparison
- v3.10.0 — sensitive-data redaction
- v3.9.0 — advanced assertions
- v3.8.0 — environment profiles and test data
- v3.7.0 — GitHub Actions CI/CD
- v3.6.0 — suite and parallel execution
- v3.5.0 — advanced PDF/reporting
- v3.4.0 — failure artifacts
- v3.3.0 — advanced reporting
- v3.2.0 — retry support

## 21. Development/release rule

For every release:

1. Update `pom.xml` version.
2. Update `Main.java` CLI version.
3. Update `README.md`.
4. Update `PROJECT_DETAILS.md`.
5. Add/update tests for new behavior.
6. Verify Maven locally or verify the corresponding GitHub Actions run.

A release must not be described as build-verified unless Maven/CI actually completed successfully.
