# AI Testing Agent — Complete Project Documentation

## Current version

**3.28.0 — Phase 7 Self-Healing & Adaptive Execution**

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, data-driven testing, environment profiles, assertions, bounded retries, failure artifacts, HTML/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation, preflight validation and structured AI requirement intelligence.

## v3.28.0 Self-Healing & Adaptive Execution

Phase 7 consumes deterministic failure intelligence without allowing unrestricted AI-controlled execution.

### `AdaptiveRetryPolicy`

`AdaptiveRetryPolicy.shouldRetry(...)` accepts only `RETRY` and `RETRY_WITH_BACKOFF` recommendations and requires a finite retry number within the configured retry budget. `delayMs(...)` calculates exponential backoff using 250 ms as the default base and 4000 ms as the hard cap.

This policy is deliberately side-effect free except for the explicit `sleep(...)` helper. It does not retry authentication, assertion, validation or locator-healing recommendations.

### `SelfHealingEngine`

`SelfHealingEngine.heal(Page, originalLocator)` inspects conservative selector alternatives. A candidate is accepted only when Playwright reports exactly one matching element and that element is visible.

Current safe transformations:

- `#id` → `[data-testid="id"]`
- `#id` → `[name="id"]`
- `#id` → `[aria-label="id"]`
- `[data-testid="id"]` → `[id="id"]`
- `[name="id"]` → `[id="id"]`

Candidates include confidence and rationale. Broad XPath, text guessing and arbitrary DOM mutation are intentionally excluded.

### `UiExecutor` healing flow

When a UI step fails, `UiExecutor` first classifies the failure with `FailureIntelligence`. Only a `HEAL_LOCATOR` recommendation can invoke `SelfHealingEngine`. If a unique visible candidate is found, the same action is executed once with the candidate. Successful healing is recorded in the step details with original locator, healed locator, confidence and reason. If healing fails, the original failure path creates the normal screenshot and metadata artifacts.

The test plan itself is not mutated, and no healing loop is allowed.

### Safety model

Phase 7 keeps execution bounded and auditable:

1. Failure classification is deterministic.
2. Retry requires an explicit finite budget.
3. Backoff is capped.
4. Locator healing is attempted at most once per failed step.
5. Candidates must resolve to one visible element.
6. Healing decisions are recorded in execution details.
7. AI providers do not receive credentials or unrestricted control over selectors/retries.

## v3.27.0 Failure Intelligence

`FailureIntelligence.analyze(ExecutionResult)` classifies failed executions into `ASSERTION`, `TIMEOUT`, `AUTHENTICATION`, `NETWORK`, `LOCATOR`, `VALIDATION`, `SERVER` or `UNKNOWN` and returns bounded retry/healing guidance.

## v3.26.0 AI testing intelligence

Phase 5 adds structured requirement-to-test intelligence: multiple positive/negative/boundary/authentication/validation/resilience scenarios, priorities, executable plans, requirement coverage, missing-test detection, duplicate groups and provider abstraction.

## v3.25.0 UI execution

`UiExecutor` provides Playwright browser/context/page execution, event-oriented waits, UI actions/assertions, screenshots and failure metadata.

## v3.24.0 API execution

`ApiExecutor` supports GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS, substituted request data, content types, forms, bearer/basic/API-key authentication, status/body/header/JSONPath/XMLPath/response-time assertions, response-variable extraction and retries.

## v3.23.0 reporting integrity

The deterministic report package is exactly `report.html`, `report.csv` and `report.pdf`. `ReportIntegrityValidator` validates the exact file set and artifact signatures.

## v3.22.0 stability and security

- `PARALLELISM` restricted to 1–64.
- Standard runtime error categories.
- Durable CLI logging and secure UTF-8 redaction.
- Dataset JSON arrays supported by repository validation.

## Core classes

- `Main` — CLI entry point and durable terminal-log lifecycle.
- `AiProvider` — provider-neutral AI generation contract.
- `OllamaClient` — Ollama implementation.
- `PromptManager` — planning, intelligence and failure-analysis prompts.
- `AiIntelligenceResult` — structured AI scenarios, coverage, gaps and duplicate groups.
- `FailureIntelligence` — deterministic failure classification and retry/healing recommendation.
- `AdaptiveRetryPolicy` — bounded exponential backoff policy.
- `SelfHealingEngine` — conservative unique-visible locator candidate engine.
- `AgentRunner` — AI planning/intelligence, execution and failure-analysis coordination.
- `TestPlanValidator` — shared API/UI preflight validation.
- `ApiExecutor` — API execution, retries, variables and evidence.
- `ApiAssertionEngine` — API assertions.
- `UiExecutor` — Playwright execution, waits, assertions, screenshots and safe locator healing.
- `TestOrchestrator` — configuration-aware orchestration entry point.
- `SuiteExecutionEngine` — suite execution and aggregation.
- `DataDrivenRunner` — dataset-driven execution.
- `ReportManager` — HTML/CSV/PDF reporting.
- `ReportIntegrityValidator` — report-package quality gate.
- `HistoryAnalyticsManager` — execution history and analytics.
- `SecurityRedactor` — persisted diagnostic protection.

## Running

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file> --parallelism 3"
```

## CI/CD

GitHub Actions performs Java 21 setup, Maven verification, example validation/execution, Playwright installation and artifact handling. A release is considered CI verified only after its corresponding workflow completes successfully.

## Technology stack

Java 21, Maven, Ollama, REST Assured 5.5.6, Playwright 1.55.0, Jackson 2.20.0, JUnit 5, Apache Commons CSV, iText 9.3.0, SLF4J and GitHub Actions.

## Release documentation rule

Every release updates `pom.xml`, `README.md`, `PROJECT_DETAILS.md`, `docs/releases/CHANGELOG.md`, relevant tests and examples/documentation.
