# AI Testing Agent — Complete Project Documentation

## 1. Project overview

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama-based planning with REST Assured API execution, Playwright UI execution, JSON test plans, environment profiles, retries, assertions, failure artifacts, reports, suite execution, execution history, regression comparison, analytics, sensitive-data redaction, and CI automation.

Current version: **3.13.0**.

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
├── src/test/java/com/thiyagarajan/agent/
└── pom.xml
```

## 4. Main commands

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

Run all examples:

```bash
bash scripts/ci/run-examples.sh
```

Run interactive AI planning:

```bash
mvn exec:java
```

## 5. Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| OLLAMA_URL | http://localhost:11434 | Ollama server URL |
| OLLAMA_MODEL | llama3.2 | Ollama model |
| HEADLESS | true | Playwright headless mode |
| DEFAULT_TIMEOUT_MS | 30000 | Default execution timeout |
| RETRIES | 0 | Default API retry count |
| PARALLELISM | 4 | Suite worker count |
| REPORTS_DIR | reports | Report/history root |
| SCREENSHOTS_DIR | screenshots | Failure artifact root |

`Config` now validates all constructor values. Invalid timeout, retry, parallelism, blank Ollama settings, or blank output directories fail with a categorized configuration error. Invalid integer environment variables also fail explicitly instead of silently falling back.

## 6. Test-plan model

A `TestPlan` contains `name`, `type` (`API` or `UI`), `baseUrl`, `variables`, and `steps`.

A `TestStep` supports API method/path, headers, query parameters, request body, timeout, retry count, assertions, saved variables, and UI locator/value fields.

## 7. Placeholder resolution

Supported placeholders include:

```text
${profile.baseUrl}
${data.userId}
${env.API_TOKEN}
${version}
```

## 8. API execution

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

## 9. UI execution

The Playwright executor supports browser navigation and common actions/assertions such as `navigate`, `click`, `fill`, `assertVisible`, `assertText`, and `screenshot`.

Chromium is installed explicitly in CI before example execution.

## 10. Retry behavior

`retryCount` on an individual API step overrides the global retry setting. When omitted, `Config.retries()` is used. Negative retry counts are rejected.

## 11. Failure artifacts

Failed executions can produce screenshots and API evidence under the configured artifact directories. Artifact metadata is redacted before persistence.

## 12. Reporting

The reporting layer produces execution information in JSON, CSV, HTML and PDF formats. Suite reporting additionally aggregates individual test results. iText 9.3.0 is pinned for PDF compatibility; unsupported `setBold()` calls were removed from PDF writer code.

## 13. Security redaction

`SecurityRedactor` centralizes masking of passwords, secrets, tokens, API keys, client secrets, authorization headers, cookies and common sensitive environment variables. Redaction is applied before sensitive information is placed into execution results, reports, failure artifacts, history/analytics output, or AI failure-analysis prompts.

## 14. Suite execution

`SuiteExecutionEngine` loads multiple plan files, executes them with configurable parallelism, isolates executor state between tests, and aggregates results. A failed test does not prevent remaining tests from running.

The v3.13 engine additionally:

- converts malformed plan JSON into a `PLAN_VALIDATION` failure;
- preserves the original suite test index when an infrastructure error occurs;
- catches per-test execution errors without aborting unrelated tests;
- reports a categorized failure message;
- handles executor interruption correctly by restoring the thread interrupt flag;
- waits for worker termination and forces shutdown if necessary;
- validates plan paths against the suite root to prevent path traversal.

## 15. Standardized execution errors — v3.13

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

This makes CLI, suite, and diagnostic failures easier to classify and troubleshoot.

`AgentRunner` now uses these categories for plan validation, AI generation failures, API/UI execution failures, suite validation, and failure-analysis failures.

## 16. CLI error handling — v3.13

`Main.main` now separates the application entry point from `run()` and converts unexpected top-level failures into concise categorized CLI messages.

Exit codes:

- `0` — successful execution
- `1` — executed test/plan/suite failed
- `2` — configuration, validation, or infrastructure/application error

Malformed plan and suite JSON are reported as validation errors instead of exposing an unstructured parser stack trace.

## 17. Execution history

`RunHistoryManager` stores each suite execution under:

```text
reports/history/<run-id>/
```

Stable identity:

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

It records pass-rate and duration deltas and writes JSON, CSV and HTML comparison output.

## 18. Historical analytics

`HistoryAnalyticsManager` analyzes recent suite history (20 runs by default), calculates execution/pass/failure totals, detects flaky tests, and identifies the slowest tests.

Flakiness rate:

```text
statusChanges / (executions - 1)
```

A test must have at least two observations, both PASS and FAIL outcomes, and meet the configured threshold to be classified as flaky.

Outputs:

```text
reports/history/analytics.json
reports/history/analytics.csv
reports/history/analytics.html
```

## 19. CI/CD behavior

`.github/workflows/ci.yml` runs on every push to `main`, pull requests to `main`, and manual workflow dispatch.

CI stages:

1. Checkout with `actions/checkout@v5`.
2. Install Java 21 with `actions/setup-java@v5`.
3. Compile the project using `mvn clean -DskipTests compile`.
4. Install Chromium using the dedicated Maven `playwright-cli` execution.
5. Run `mvn verify`.
6. Run `scripts/ci/run-examples.sh`.
7. Upload reports, screenshots, Surefire output, and generated example files.

The compile-before-browser-install sequence fixes the previous clean-runner `ClassNotFoundException: com.thiyagarajan.agent.Main` problem. Artifact upload uses `if: always()` and `if-no-files-found: ignore` so artifact collection does not hide the original failure.

## 20. Every example is continuously verified

`run-examples.sh`:

- validates both requirement fixtures are non-empty;
- parses every JSON file under `examples/`;
- executes `v3-plan-file.json`;
- executes `v3-suite.json`;
- extracts and executes the API portion of `v2-execution.json`;
- extracts and executes the UI portion of `v2-execution.json`.

Requirement `.txt` files are AI inputs, not executable plans, so CI validates their content instead of requiring Ollama. This keeps CI deterministic while validating all example inputs.

## 21. Example files

### `examples/api-requirement.txt`

API requirement input for AI plan generation and CI content validation.

### `examples/ui-requirement.txt`

UI requirement input for AI plan generation and CI content validation.

### `examples/v2-execution.json`

Legacy combined API/UI example. CI extracts both sections and executes them through the current runner.

### `examples/v3-plan-file.json`

Current direct API plan demonstrating variables, headers, query parameters, retry count and assertions.

### `examples/v3-suite.json`

Current suite definition exercising suite orchestration, reporting, history, comparison and analytics.

## 22. Generated output

Typical output:

```text
reports/
├── execution.json
├── execution.csv
├── execution.html
├── execution.pdf
├── suite/
└── history/
    ├── index.json
    ├── index.html
    ├── analytics.json
    ├── analytics.csv
    ├── analytics.html
    └── <run-id>/
        ├── suite-execution.json
        ├── comparison.json
        ├── comparison.csv
        └── comparison.html

screenshots/
target/
├── surefire-reports/
└── ci-examples/
```

## 23. Failure-analysis flow

When a test fails, the agent can pass the redacted plan and execution result to Ollama for AI-assisted failure analysis. AI analysis failure is isolated from the underlying test result so reporting can still complete.

## 24. Important implementation methods

### `Main.main`
Top-level entry point. Converts categorized application failures into deterministic CLI output and exit code `2`.

### `Main.run`
Loads configuration, resolves an optional environment profile, dispatches commands, or starts interactive mode.

### `Main.executeFile`
Loads and validates a JSON plan, applies the environment profile, executes it, performs optional failure analysis, writes reports, and returns exit code `1` for a failed test.

### `Main.executeSuite`
Loads and validates a suite, executes it using configured parallelism, writes suite reports, records history, performs analytics, and returns exit code `1` for a failed suite.

### `Config.load`
Loads environment variables and validates all runtime configuration values.

### `AgentRunner.plan`
Converts a natural-language requirement into a structured test plan using Ollama and categorizes invalid AI output as `AI_GENERATION`.

### `AgentRunner.execute`
Validates a plan, selects API or UI execution, and categorizes execution failures.

### `AgentRunner.executeSuite`
Backward-compatible sequential suite execution with the same validation/error model.

### `AgentRunner.analyzeFailure`
Redacts plan/result data and requests AI failure analysis while categorizing AI errors.

### `SuiteExecutionEngine.execute`
Executes suite plans concurrently using the configured worker count and isolates per-test failures.

### `SuiteExecutionEngine.executeSafely`
Converts an individual plan execution exception into a failed `TestExecution` so other suite tasks can continue.

### `SuiteExecutionEngine.resolvePlan`
Normalizes and validates a plan path and prevents it from escaping the suite directory.

### `SuiteReportManager.writeAll`
Generates aggregate suite reports and individual test reports.

### `RunHistoryManager.recordSuite`
Persists a suite snapshot, compares it with the previous run, and updates the history index.

### `HistoryAnalyticsManager.writeReports`
Loads recent run history, computes analytics/flakiness/slow tests, and writes dashboard data and reports.

### `SecurityRedactor.redactText`
Masks recognized sensitive values before output persistence or AI analysis.

## 25. Tests added in v3.13

`ConfigValidationTest` verifies:

- invalid timeout rejection;
- negative retry rejection;
- blank model rejection.

`AgentRunnerValidationTest` verifies:

- null plan rejection;
- unsupported plan type rejection;
- invalid API action rejection;
- correct `PLAN_VALIDATION` categorization.

Existing suite/history/analytics/reporting tests remain part of the Maven test gate.

## 26. Development workflow

Source/configuration changes:

```bash
mvn clean verify
```

Example changes:

```bash
bash scripts/ci/run-examples.sh
```

UI/browser changes:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

Push to `main` and GitHub Actions repeats compilation, browser setup, Maven verification, and all example executions.

## 27. Current bug fixes and hardening

### Fixed: Playwright CLI `ClassNotFoundException`

Cause: Maven `exec:java` was invoked on a clean runner before project classes were compiled.

Fix:

- dedicated `playwright-cli` Maven execution;
- explicit compile step before CLI execution;
- CI invokes `exec:java@playwright-cli`.

### Fixed: CI action runtime warnings

The workflow uses the v5 releases of checkout, setup-java, and upload-artifact.

### Fixed: PDF `setBold()` incompatibility

Unsupported `setBold()` calls were removed from the iText PDF writer and iText 9.3.0 is pinned.

### Fixed: `Config` constructor mismatch

Tests were updated for the current eight-field configuration record.

### Fixed: weak configuration validation — v3.13

Configuration is now validated at construction time. Invalid numeric environment variables produce an explicit categorized error instead of being silently replaced with a default.

### Fixed: unstructured execution errors — v3.13

The new `AgentExecutionException` provides stable error categories across configuration, validation, execution, AI, reporting, and infrastructure failures.

### Fixed: suite failure propagation — v3.13

Individual plan parsing/execution failures are converted into failed test results so independent suite tests continue running. Executor interruption and shutdown are handled explicitly.

### Artifact handling

Artifact upload runs with `if: always()` and ignores missing paths so an earlier failure remains the primary CI failure.

## 28. CI troubleshooting

If Maven compilation fails, fix the Java/compiler error first.

If Playwright reports a missing browser, verify the compile and dedicated `playwright-cli` installation stages completed.

If a plan/suite JSON is malformed, the CLI now reports `PLAN_VALIDATION` or `SUITE_VALIDATION` instead of an unclassified parser stack trace.

If `OLLAMA_URL`, `OLLAMA_MODEL`, `DEFAULT_TIMEOUT_MS`, `RETRIES`, or `PARALLELISM` is invalid, correct the environment variable; v3.13 no longer silently accepts malformed integer configuration.

If an external API/UI example fails because its public service is unavailable, CI correctly reports the example failure; investigate service availability before changing application code.

If a test fails because of a constructor/API change, search the repository for all usages of the changed constructor or method before updating individual tests.

Node/punycode messages from third-party action internals are separate from Java build failures.

## 29. Version history

- v3.2.0 — Retry and resilience
- v3.3.0 — Advanced reporting and execution logs
- v3.4.0 — Failure artifacts
- v3.5.0 — PDF reporting/dashboard
- v3.6.0 — Suite execution and parallelism
- v3.7.0 — GitHub Actions CI/CD
- v3.8.0 — Environment profiles and test data
- v3.9.0 — Advanced assertions and diagnostics
- v3.10.0 — Sensitive-data redaction
- v3.11.0 — Execution history and run comparison
- v3.12.0 — Historical analytics, flaky-test detection, PDF compatibility, CI/example execution hardening, and Playwright CLI classpath fix
- v3.13.0 — Standardized execution errors, strict configuration validation, plan/suite validation hardening, per-test suite failure isolation, executor lifecycle cleanup, structured CLI errors, and validation tests

## 30. Documentation maintenance rule

This file is the canonical complete project guide. Every future version/change must update `PROJECT_DETAILS.md` in the same change, including:

- version number;
- new/changed classes and methods;
- configuration changes;
- example changes;
- CI changes;
- report/output changes;
- test changes;
- bug fixes and compatibility fixes;
- usage commands;
- troubleshooting updates.

`README.md` remains the quick-start/project overview; `PROJECT_DETAILS.md` contains the detailed implementation and operational documentation.
