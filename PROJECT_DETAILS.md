# AI Testing Agent — Complete Project Documentation

## Current version

**3.27.0 — Phase 6 Failure Intelligence & Smart Retry Guidance**

AI Testing Agent is a Java 21 automation framework for AI-assisted API and UI test planning/execution. It combines Ollama planning with REST Assured API execution, Playwright UI execution, data-driven testing, environment profiles, assertions, retries, failure artifacts, HTML/CSV/PDF reporting, suite execution, history/analytics, security redaction, CI automation, preflight validation and structured AI requirement intelligence.

## v3.27.0 Failure Intelligence

Phase 6 adds a deterministic safety layer for failed executions before any optional AI-driven remediation.

### `FailureIntelligence`

`FailureIntelligence.analyze(ExecutionResult)` classifies failed executions into `ASSERTION`, `TIMEOUT`, `AUTHENTICATION`, `NETWORK`, `LOCATOR`, `VALIDATION`, `SERVER` or `UNKNOWN`.

It returns a `RetryRecommendation`:

- `RETRY` — safe candidate for a bounded retry.
- `RETRY_WITH_BACKOFF` — likely transient timing/network/server failure.
- `HEAL_LOCATOR` — UI selector failure is a candidate for selector healing/review.
- `DO_NOT_RETRY` — authentication, assertion or validation failures should normally be corrected rather than blindly repeated.

The classifier is deterministic and does not send execution data to an AI provider. This provides predictable behavior for CI quality gates and protects against unsafe retry loops.

### `AgentRunner.analyzeFailureIntelligence(ExecutionResult)`

Exposes the deterministic recommendation through the main runtime orchestration layer. Existing `analyzeFailure(...)` remains the optional LLM-based natural-language failure analysis method.

### Safety model

Phase 6 does not automatically mutate locators or retry indefinitely. It produces a bounded recommendation that later self-healing/retry orchestration can consume. This separation keeps diagnosis deterministic and prevents an AI response from directly controlling unbounded execution.

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
- `AgentRunner` — AI planning/intelligence, execution and failure-analysis coordination.
- `TestPlanValidator` — shared API/UI preflight validation.
- `ApiExecutor` — API execution, retries, variables and evidence.
- `ApiAssertionEngine` — API assertions.
- `UiExecutor` — Playwright execution, waits, assertions and screenshots.
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
