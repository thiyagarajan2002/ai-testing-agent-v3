# AI Testing Agent — Complete Project Documentation

## Current version

**3.26.0 — Phase 5 AI Testing Intelligence**

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, JSON/CSV data-driven testing, environment profiles, assertions, retries, failure artifacts, HTML/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation, preflight validation and structured AI requirement intelligence.

## v3.26.0 AI testing intelligence

Phase 5 adds a structured requirement-to-test intelligence workflow on top of the existing single-plan AI planner.

### `AiProvider`

`AiProvider` is a minimal functional interface with `generate(String prompt)`. It separates AI orchestration from any specific model provider and allows tests to inject deterministic provider responses without running Ollama.

`OllamaClient` now implements `AiProvider`. Existing Ollama behavior is preserved, with added validation for blank base URL, model name and prompts.

### `PromptManager.intelligencePrompt(String requirement)`

Builds the Phase 5 structured intelligence prompt. The expected response contains:

- requirement summary
- multiple generated scenarios
- scenario ID, title, category, priority and objective
- one executable `TestPlan` per scenario
- requirement-to-test coverage mappings
- missing high-value tests
- duplicate scenario groups

Supported scenario categories are `positive`, `negative`, `boundary`, `authentication`, `validation` and `resilience`. The prompt explicitly prohibits invented credentials or secrets and instructs the model to use placeholders such as `${token}`.

### `AiIntelligenceResult`

Top-level model returned by Phase 5.

Fields:

- `summary` — concise requirement interpretation
- `scenarios` — generated executable scenarios
- `coverage` — atomic requirement clauses mapped to scenario IDs
- `missingTests` — important uncovered scenarios
- `duplicateGroups` — groups of redundant generated scenario IDs

Each `Scenario` contains `id`, `title`, `category`, `priority`, `objective` and an embedded `TestPlan`.

### `AgentRunner.intelligence(String requirement)`

Generates and validates the structured AI package. Validation rejects:

- blank requirements
- missing AI provider
- empty AI responses
- no generated scenarios
- blank or duplicate scenario IDs
- scenarios without executable plans
- embedded plans that fail `TestPlanValidator`
- coverage mappings that reference unknown scenario IDs
- duplicate groups that reference unknown scenario IDs

The existing `plan(String requirement)` method remains backward-compatible for single-plan generation.

### `TestOrchestrator.intelligence(String requirement)`

Exposes the Phase 5 workflow through the project orchestration layer. Applications can call it without bypassing the existing configuration/provider setup.

Programmatic example:

```java
try (TestOrchestrator orchestrator = new TestOrchestrator(Config.load(), new ObjectMapper(), null)) {
    AiIntelligenceResult intelligence = orchestrator.intelligence(requirementText);
    for (AiIntelligenceResult.Scenario scenario : intelligence.scenarios) {
        System.out.println(scenario.id + " | " + scenario.category + " | " + scenario.priority);
        TestPlan executable = scenario.plan;
    }
}
```

### Shared validation alignment

Phase 5 exposed an older shared-validator mismatch. `TestPlanValidator` has now been aligned with the actual executors:

- API actions: GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS
- UI actions: navigate, click, fill, press, selectOption, hover, check, uncheck, assertVisible, assertText, assertValue, assertTitle, assertUrl, waitFor, waitForVisible, waitForHidden, screenshot
- locator-required UI actions now fail preflight when no locator is supplied
- `waitFor` values are validated as non-negative milliseconds

This ensures generated Phase 5 plans use the same action contract as Phase 3 and Phase 4 execution.

## v3.25.0 UI execution

`UiExecutor` is the Playwright UI runtime entry point. Phase 4 hardens browser/context lifecycle, navigation waiting, locator actions, assertions, explicit state waits and evidence handling while retaining the existing `ExecutionResult` contract.

### Browser lifecycle

Each UI execution creates a Playwright browser, an isolated browser context and a page. The context and browser are closed in `finally` blocks so resources are released even when a UI step fails.

### Navigation and waits

Navigation waits for `DOMContentLoaded` instead of relying on an arbitrary sleep. Locator-based actions use Playwright's built-in action waiting. Explicit state waits are available through `waitforvisible` and `waitforhidden`. The existing `waitfor` timed wait remains supported for backward compatibility, but event/state waits are preferred.

### Supported UI actions

`navigate`, `click`, `fill`, `press`, `selectOption`, `hover`, `check`, `uncheck`, `assertVisible`, `assertText`, `assertValue`, `assertTitle`, `assertUrl`, `waitFor`, `waitForVisible`, `waitForHidden`, and `screenshot` are supported.

Locator-based actions fail with a clear diagnostic when the locator is missing. Step-specific `timeoutMs` overrides the configured default timeout.

### UI assertions

- `assertvisible` verifies the target is visible.
- `asserttext` verifies the target text contains the expected value.
- `assertvalue` verifies an input value exactly.
- `asserttitle` verifies the page title contains the expected value.
- `asserturl` verifies the current URL contains the expected value.

### UI evidence

After every successful step, a full-page screenshot is attempted and attached to the corresponding `ExecutionResult.StepResult`. Failed steps capture a failure screenshot and failure metadata. Screenshot failures are deliberately non-fatal so evidence problems do not hide the original test failure.

### UI diagnostics and security

Exception text stored in execution results is passed through `SecurityRedactor`. Failure metadata is created through the existing `FailureArtifactManager`, preserving the project's persisted-evidence security model.

## v3.24.0 API execution

`ApiExecutor` supports GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS, substituted path/query/header values, JSON/text bodies, content types, URL-encoded forms, bearer/basic/API-key authentication, status/body/header/JSONPath/XMLPath/response-time assertions, response-variable extraction and retries.

## v3.23.0 reporting integrity

The report contract is exactly:

```text
reports/<run>/
├── report.html
├── report.csv
└── report.pdf
```

`ReportIntegrityValidator` checks the exact file set, non-empty artifacts, HTML markers, UTF-8 BOM/summary in CSV and the PDF signature. HTML, CSV and PDF are generated from the same `ExecutionResult`.

## v3.22.0 stability and security

- `PARALLELISM` is restricted to 1–64.
- Runtime failures use `AgentExecutionException` categories.
- CLI exception handling keeps durable terminal logging active until resources close.
- Persisted terminal logs redact sensitive values and preserve UTF-8 output.
- Repository JSON validation allows arrays under `examples/data/` while executable plan/suite JSON remains object-only.

## Examples

API plans are under `examples/plans/api/` and include GET, POST, PUT, DELETE, query/header, negative-status, save-variable, authentication and assertion scenarios.

UI plans are under `examples/plans/ui/` and use Playwright with automatic screenshots after successful and failed steps. New UI plans should prefer `waitforvisible`/`waitforhidden` over fixed `waitfor` sleeps.

Suites are under `examples/suites/` and data-driven plans under `examples/plans/data-driven/`.

## Running

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan examples/plans/api/get-user.json"
mvn exec:java -Dexec.args="plan examples/plans/ui/login-flow.json"
mvn exec:java -Dexec.args="suite examples/suites/api-regression-suite.json"
mvn exec:java -Dexec.args="data-driven examples/plans/data-driven/users-api.json examples/data/json/users.json --parallelism 3"
mvn exec:java -Dexec.args="validate plan examples/plans/ui/login-flow.json"
```

Install Chromium when required:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

## Core classes

- `Main` — CLI entry point and durable terminal-log lifecycle.
- `AiProvider` — provider-neutral AI generation contract.
- `OllamaClient` — Ollama implementation of `AiProvider`.
- `PromptManager` — single-plan, structured-intelligence and failure-analysis prompts.
- `AiIntelligenceResult` — generated scenarios, coverage, gaps and duplicate groups.
- `AgentRunner` — AI planning/intelligence, validation, execution and failure-analysis coordination.
- `TestPlan` — test name, type, base URL, variables and steps.
- `TestStep` — API/UI action model, request data, authentication, retries and assertions.
- `TestPlanValidator` — non-executing shared API/UI plan validation.
- `ApiExecutor` — API request execution, retries, variable extraction and evidence.
- `ApiAssertionEngine` — API status/body/header/JSON/XML/performance assertions.
- `UiExecutor` — Playwright browser/context/page execution, waits, actions, assertions and screenshots.
- `TestOrchestrator` — configuration-aware entry point for plan, suite, data-driven and AI intelligence workflows.
- `SuiteExecutionEngine` — suite loading, validation, parallel workers and aggregation.
- `DataDrivenRunner` — dataset-driven execution and metrics.
- `ReportManager` — unified HTML/CSV/PDF report generation.
- `ReportIntegrityValidator` — report-package quality gate.
- `HistoryAnalyticsManager` — run history, reliability and flaky/slow analysis.
- `SecurityRedactor` — sensitive-value protection across persisted diagnostics.

## Reporting

HTML provides the interactive dashboard, CSV provides machine-readable execution data, and PDF provides the printable report. Runtime logs/screenshots remain evidence and are referenced from step artifacts rather than becoming additional report formats.

## CI/CD

GitHub Actions performs Java 21 setup, Maven verification, example validation/execution, Playwright installation and artifact handling. A release is only described as CI/build verified after the corresponding workflow actually completes successfully.

## Technology stack

- Java 21
- Maven
- Ollama
- REST Assured 5.5.6
- Playwright 1.55.0
- Jackson 2.20.0
- JUnit 5
- Apache Commons CSV
- iText 9.3.0
- SLF4J
- GitHub Actions

## Release documentation rule

Every release updates `pom.xml`, CLI/version metadata, `README.md`, `PROJECT_DETAILS.md`, `docs/releases/CHANGELOG.md`, relevant tests and examples/documentation.
