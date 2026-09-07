# AI Testing Agent — Complete Project Documentation

## 1. Project overview

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama-based planning with REST Assured API execution, Playwright UI execution, JSON test plans, environment profiles, retries, assertions, failure artifacts, reports, suite execution, execution history, regression comparison, analytics, and sensitive-data redaction.

Current version: **3.12.0**.

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

## 4. Main execution commands

Build and test:

```bash
mvn clean verify
```

Install Playwright Chromium reliably:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

Run a plan:

```bash
mvn exec:java -Dexec.args="plan examples/v3-plan-file.json"
```

Run a plan with an environment profile:

```bash
mvn exec:java -Dexec.args="plan examples/v3-plan-file.json --env qa"
```

Run a suite:

```bash
mvn exec:java -Dexec.args="suite examples/v3-suite.json"
```

Run all repository examples:

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

## 6. Test-plan model

A `TestPlan` contains:

- `name`
- `type` (`API` or `UI`)
- `baseUrl`
- `variables`
- `steps`

A `TestStep` supports API method/path, headers, query parameters, request body, timeout, retry count, assertions, saved variables, and UI locator/value fields.

## 7. Placeholder resolution

Environment/profile/data values can be referenced with:

```text
${profile.baseUrl}
${data.userId}
${env.API_TOKEN}
${version}
```

Unresolved environment/profile/data placeholders fail fast where required by the execution layer.

## 8. API execution

The API executor builds requests from the plan, applies headers/query/body values, performs the request, evaluates assertions, saves configured response values, retries failures according to the configured retry policy, and creates failure artifacts when execution fails.

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

The Playwright executor supports browser navigation and common actions/assertions such as:

- navigate
- click
- fill
- assertVisible
- assertText
- screenshot

Chromium is installed explicitly in CI before example execution.

## 10. Retry behavior

`retryCount` on an individual API step overrides the global retry setting. When omitted, `Config.retries()` is used. Negative retry counts are rejected.

## 11. Failure artifacts

Failed executions can produce screenshots and API evidence under the configured artifact directories. Artifact metadata is redacted before persistence.

## 12. Reporting

The reporting layer produces execution information in JSON, CSV, HTML and PDF formats. Suite reporting additionally aggregates individual test results. iText 9.3.0 is pinned for PDF compatibility; unsupported `setBold()` calls were removed from PDF writer code.

## 13. Security redaction

`SecurityRedactor` centralizes masking of sensitive values including passwords, secrets, tokens, API keys, client secrets, authorization headers, cookies and common sensitive environment variables. Redaction is applied before sensitive information is placed into execution results, reports, failure artifacts, history/analytics output, or AI failure-analysis prompts.

## 14. Suite execution

`SuiteExecutionEngine` loads multiple plan files, executes them with configurable parallelism, isolates executor state between tests, and aggregates results. A failed test does not prevent the remaining tests from running.

`SuiteReportManager` writes aggregate suite reports and individual test reports.

## 15. Execution history

`RunHistoryManager` stores each suite execution under:

```text
reports/history/<run-id>/
```

It maintains an index and compares the current run with the previous run using the stable identity:

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

## 16. Historical analytics

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

## 17. CI/CD behavior

`.github/workflows/ci.yml` runs on every push to `main`, pull requests to `main`, and manual workflow dispatch.

### CI stages

1. Checkout with `actions/checkout@v5`.
2. Install Java 21 with `actions/setup-java@v5`.
3. Compile the project using `mvn clean -DskipTests compile`.
4. Install Chromium using the dedicated Maven `playwright-cli` execution.
5. Run `mvn verify`.
6. Run `scripts/ci/run-examples.sh`.
7. Upload reports, screenshots, Surefire output, and generated example files.

### Why compilation happens before browser installation

The previous workflow invoked Maven `exec:java` before project compilation. On a clean GitHub runner, `target/classes` did not yet contain `com.thiyagarajan.agent.Main`, producing:

```text
ClassNotFoundException: com.thiyagarajan.agent.Main
```

The project now defines a dedicated `playwright-cli` execution in `exec-maven-plugin`, and CI explicitly compiles before invoking it. This makes the browser-install step deterministic and prevents the old classpath failure.

### Every example is continuously verified

`run-examples.sh`:

- validates both requirement fixtures are non-empty;
- parses every JSON file under `examples/`;
- executes `v3-plan-file.json`;
- executes `v3-suite.json`;
- extracts and executes the API portion of `v2-execution.json`;
- extracts and executes the UI portion of `v2-execution.json`.

Therefore a push is not CI-green unless the Maven test gate and all executable examples pass.

Requirement `.txt` files are inputs for AI plan generation, not executable plans, so CI validates their content instead of requiring Ollama. This keeps CI deterministic while still continuously validating all example inputs.

## 18. Example files

### `examples/api-requirement.txt`

API requirement input used for AI plan generation and checked for non-empty content in CI.

### `examples/ui-requirement.txt`

UI requirement input used for AI plan generation and checked for non-empty content in CI.

### `examples/v2-execution.json`

Legacy combined API/UI example. CI extracts both sections and executes them through the current runner.

### `examples/v3-plan-file.json`

Current direct API plan demonstrating variables, headers, query parameters, retry count and assertions.

### `examples/v3-suite.json`

Current suite definition referencing the v3 plan and exercising suite orchestration, reporting, history, comparison and analytics.

## 19. Generated output

Typical output directories:

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

Exact report filenames can vary by report manager/version.

## 20. Failure-analysis flow

When a test fails, the agent can pass the redacted plan and execution result to Ollama for AI-assisted failure analysis. AI analysis failure is isolated from the underlying test result so reporting can still complete.

## 21. Important implementation methods

### `Main.main`
Loads configuration, resolves an optional environment profile, dispatches `plan` and `suite` commands, or starts interactive mode.

### `Main.executeFile`
Loads a JSON test plan, applies an environment profile, executes it, performs optional AI failure analysis, writes reports, and exits with failure when the test fails.

### `Main.executeSuite`
Loads a suite, executes it using configured parallelism, writes suite reports, records history, performs analytics, prints summary paths, and exits with failure when the suite fails.

### `EnvironmentManager.load/apply`
Loads an environment profile, validates paths, loads optional test data, and merges its base URL, headers, variables and timeout into a test plan.

### `AgentRunner.plan`
Converts a natural-language requirement into a structured test plan using the configured Ollama client.

### `AgentRunner.execute`
Selects API or UI execution based on the test-plan type and returns an `ExecutionResult`.

### `ApiExecutor.execute`
Runs API steps, applies retries, substitutions, assertions, saved variables and failure artifacts.

### `ApiAssertionEngine`
Evaluates supported API assertion types and generates diagnostic failure messages.

### `SuiteExecutionEngine.execute`
Executes suite plans concurrently using the configured worker count and isolates failures per test.

### `SuiteReportManager.writeAll`
Generates aggregate suite report formats and individual test reports.

### `RunHistoryManager.recordSuite`
Persists a suite snapshot, compares it with the previous run, and updates the history index.

### `HistoryAnalyticsManager.writeReports`
Loads recent run history, computes analytics/flakiness/slow tests, and writes dashboard data and reports.

### `SecurityRedactor.redactText`
Removes or masks recognized sensitive values before output persistence or AI analysis.

## 22. Development workflow

For source/configuration changes:

```bash
mvn clean verify
```

For example changes:

```bash
bash scripts/ci/run-examples.sh
```

For UI changes:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

Commit and push to `main`. GitHub Actions automatically repeats the compile, browser setup, full Maven test gate, and all example executions.

## 23. Current bug fixes and hardening

### Fixed: Playwright CLI `ClassNotFoundException`

Cause: Maven `exec:java` was invoked on a clean runner before project classes were compiled.

Fix:

- dedicated `playwright-cli` Maven execution;
- explicit compile step before CLI execution;
- CI invokes `exec:java@playwright-cli`.

### Fixed: obsolete GitHub Actions Node 20 warnings

The workflow was migrated from checkout/setup-java/upload-artifact v4 to the v5 action releases so the workflow uses the current Node 24-compatible action runtime.

### Fixed: PDF `setBold()` incompatibility

Removed unsupported `setBold()` calls from iText PDF writer code and pinned iText 9.3.0.

### Fixed: `Config` constructor mismatch

Tests using the old six-argument constructor were updated for the current eight-field configuration record (`ollamaUrl`, `ollamaModel`, `headless`, `defaultTimeoutMs`, `retries`, `parallelism`, `reportsDir`, `screenshotsDir`).

### Artifact handling

Artifact upload runs with `if: always()` so failures still attempt collection. `if-no-files-found: ignore` prevents a secondary artifact-upload failure when an earlier step fails before producing output.

## 24. CI troubleshooting

If Maven compilation fails, fix the Java/compiler error before considering example execution.

If Playwright reports a missing browser, verify the compile and dedicated `playwright-cli` installation stages completed.

If an external API/UI example fails because its public service is unavailable, CI correctly reports the example failure; investigate service availability before changing application code.

If a test fails because of a constructor/API change, search the repository for all usages of the changed constructor or method before updating individual tests.

The GitHub Actions Node/punycode messages are action-runtime deprecation warnings and are separate from Java build failures.

## 25. Version history

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
- v3.12.0 — Historical analytics, flaky-test detection, iText PDF compatibility, CI/example execution hardening, and Playwright CLI classpath fix

## 26. Documentation maintenance rule

This file is the canonical complete project guide. **Every future version/change must update `PROJECT_DETAILS.md` in the same change/commit**, including:

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
