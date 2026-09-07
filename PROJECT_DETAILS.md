# AI Testing Agent — Complete Project Documentation

## 1. Project overview

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama-based planning with REST Assured API execution, Playwright UI execution, JSON test plans, environment profiles, retries, assertions, failure artifacts, reports, suite execution, execution history, regression comparison, historical analytics, sensitive-data redaction, CI automation, and non-executing preflight validation.

**Current version: 3.15.0**

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
├── config/
│   ├── environments/
│   └── test-data/
├── examples/
│   ├── api-requirement.txt
│   ├── ui-requirement.txt
│   ├── v2-execution.json
│   ├── v3-plan-file.json
│   └── v3-suite.json
├── scripts/ci/run-examples.sh
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

Build and test:

```bash
mvn clean verify
```

Install Chromium:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

Run a plan:

```bash
mvn exec:java -Dexec.args="plan examples/v3-plan-file.json"
```

Run a plan with an environment:

```bash
mvn exec:java -Dexec.args="plan examples/v3-plan-file.json --env qa"
```

Run a suite:

```bash
mvn exec:java -Dexec.args="suite examples/v3-suite.json"
```

Validate a plan without execution:

```bash
mvn exec:java -Dexec.args="validate plan examples/v3-plan-file.json"
```

Validate a plan with an environment:

```bash
mvn exec:java -Dexec.args="validate plan examples/v3-plan-file.json --env qa"
```

Validate a complete suite without executing its plans:

```bash
mvn exec:java -Dexec.args="validate suite examples/v3-suite.json"
```

Interactive AI planning:

```bash
mvn exec:java -Dexec.args="interactive"
```

## 5. Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server URL |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless mode |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default execution timeout |
| `RETRIES` | `0` | Default API retry count |
| `PARALLELISM` | `4` | Suite worker count |
| `REPORTS_DIR` | `reports` | Report/history root |
| `SCREENSHOTS_DIR` | `screenshots` | Failure artifact root |

`Config` validates timeout, retry, parallelism, Ollama settings, and output directories. Invalid integer environment variables fail explicitly rather than silently falling back.

## 6. Test-plan model

A `TestPlan` contains `name`, `type` (`API` or `UI`), `baseUrl`, `variables`, and `steps`.

A `TestStep` supports API method/path, headers, query parameters, request body, timeout, retry count, assertions, saved variables, and UI locator/value fields.

## 7. Preflight validation — v3.15

`TestPlanValidator` provides a reusable non-executing validation layer.

It validates:

- plan existence and JSON parsing through the orchestrator;
- plan type (`API` or `UI`);
- required `baseUrl`;
- presence of steps;
- required step actions;
- supported API actions;
- supported UI actions;
- positive `timeoutMs`;
- non-negative `retryCount`.

It also reports warnings for suspicious but executable configurations, such as a blank API path or missing UI locator where appropriate.

### Dry-run behavior

`validate plan` and `validate suite` do **not**:

- send API requests;
- launch Playwright browsers;
- contact Ollama;
- generate execution reports;
- modify test results.

With `--env`, the environment profile is applied before validation so the same effective configuration is checked as during execution.

### Suite preflight

`validate suite` checks every referenced plan, rejects blank paths, verifies that files exist, prevents paths from escaping the suite directory, parses each plan, applies the optional environment profile, and returns per-plan errors/warnings.

### Exit codes

```text
0 = validation successful
1 = validation completed and one or more validation errors were found
2 = validation could not be performed because of configuration/file/infrastructure error
```

## 8. Shared validation architecture

`AgentRunner` calls the same `TestPlanValidator` used by the CLI preflight command. This prevents a plan from passing a standalone validator and then failing because the runtime applies different rules.

The architecture is:

```text
Main
  │
  └── TestOrchestrator
       ├── validatePlan ──> TestPlanValidator
       ├── validateSuite ─> TestPlanValidator
       └── executePlan ───> AgentRunner ──> TestPlanValidator ──> Executor
```

## 9. Placeholder resolution

Supported placeholders include:

```text
${profile.baseUrl}
${data.userId}
${env.API_TOKEN}
${version}
```

## 10. API execution

The API executor builds requests from the plan, applies headers/query/body values, performs requests, evaluates assertions, saves configured response values, retries failures according to the configured retry policy, and creates failure artifacts when execution fails.

### Supported assertions

- status
- bodyContains
- bodyNotContains
- bodyRegex
- headerEquals
- jsonPathExists
- jsonPathEquals
- jsonPathContains
- jsonPathRegex
- responseTimeMs
- legacy `assertSpec`

## 11. UI execution

The Playwright executor supports browser navigation and common actions/assertions such as `navigate`, `click`, `fill`, `press`, `selectOption`, `assertVisible`, `assertText`, `assertValue`, `waitFor`, and `screenshot`.

Chromium is installed explicitly in CI before example execution.

## 12. Retry behavior

`retryCount` on an individual API step overrides the global retry setting. When omitted, `Config.retries()` is used. Negative retry counts are rejected.

## 13. Failure artifacts

Failed executions can produce screenshots and API evidence under the configured artifact directories. Artifact metadata is redacted before persistence, including test names used in artifact filenames.

## 14. Reporting

The reporting layer produces execution information in JSON, CSV, HTML, and PDF formats. Suite reporting aggregates individual test results and writes suite-level reports.

iText 9.3.0 is pinned for PDF compatibility.

## 15. Security redaction

`SecurityRedactor` centralizes masking of passwords, secrets, tokens, API keys, client secrets, authorization headers, cookies, and common sensitive environment variables.

Redaction is applied before sensitive information is placed into execution results, reports, failure artifacts, history/analytics output, or AI failure-analysis prompts.

## 16. Suite execution

`SuiteExecutionEngine` loads multiple plan files, executes them with configurable parallelism, isolates executor state between tests, and aggregates results. A failed test does not prevent remaining tests from running.

The suite engine also validates plan paths against the suite root to prevent path traversal, isolates per-test infrastructure errors, handles interruption correctly, and shuts down workers safely.

## 17. Standardized execution errors

`AgentExecutionException` provides a common runtime error type with categories:

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

## 18. CLI and orchestration

`TestOrchestrator` is the lifecycle coordination layer between `Main` and runtime components.

`Main` handles command dispatch, interactive input, output, and exit codes. The orchestrator handles file loading, environment application, execution, reporting, and suite history coordination.

Exit codes for normal execution:

- `0` — successful execution
- `1` — executed test/plan/suite failed
- `2` — configuration, validation, application, or infrastructure error

## 19. Execution history

`RunHistoryManager` stores suite executions under:

```text
reports/history/<run-id>/
```

Stable test identity:

```text
planFile + "::" + testName
```

Comparison categories:

- REGRESSION
- FIXED
- PASSED_UNCHANGED
- FAILED_UNCHANGED
- NEW_TEST
- REMOVED_TEST

It records pass-rate and duration deltas and writes comparison JSON, CSV, and HTML output.

## 20. Historical analytics

`HistoryAnalyticsManager` analyzes recent suite history, calculates execution/pass/failure totals, detects flaky tests, and identifies slow tests.

It produces:

```text
reports/history/analytics.json
reports/history/analytics.csv
reports/history/analytics.html
```

## 21. CI/CD behavior

`.github/workflows/ci.yml` runs on pushes to `main`, pull requests to `main`, and manual workflow dispatch.

CI stages include:

1. Checkout.
2. Java 21 setup.
3. Compile before Playwright CLI usage.
4. Chromium installation.
5. Maven verification.
6. Repository example validation/execution.
7. Report and artifact upload.

The compile-before-Playwright step prevents a clean-runner classpath failure when invoking the Playwright CLI.

## 22. Version history

- v3.2.0 — Retry & resilience
- v3.3.0 — Advanced reporting and logs
- v3.4.0 — Failure artifacts
- v3.5.0 — PDF reporting and dashboard
- v3.6.0 — Suite and parallel execution
- v3.7.0 — CI/CD integration
- v3.8.0 — Environment profiles and test data
- v3.9.0 — Advanced assertions
- v3.10.0 — Sensitive-data redaction
- v3.11.0 — Execution history and run comparison
- v3.12.0 — Historical analytics, flaky-test detection, PDF compatibility, and CI hardening
- v3.13.0 — Execution reliability, standardized errors, strict configuration validation, suite failure isolation, executor lifecycle hardening, structured CLI errors, and validation tests
- v3.14.0 — TestOrchestrator execution architecture refactor and CLI simplification
- **v3.15.0 — Preflight validation, dry-run plan checks, suite validation, and shared validation logic**

## 23. Development rule for future releases

Every version change must update:

1. `pom.xml` version.
2. CLI version in `Main.java`.
3. `README.md`.
4. `PROJECT_DETAILS.md`.
5. Unit tests for the new behavior.
6. CI/build verification where available.

Do not mark a release as build-verified unless the Maven build/tests or CI run has actually completed successfully.
