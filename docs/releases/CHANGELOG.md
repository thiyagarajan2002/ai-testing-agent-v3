# Changelog

All notable project changes are documented here at release level. The complete implementation history and method-level details remain in `PROJECT_DETAILS.md`.

## 3.28.0

- Added bounded `AdaptiveRetryPolicy` with deterministic exponential backoff and a hard delay cap.
- Restricted automatic retry eligibility to transient `RETRY` and `RETRY_WITH_BACKOFF` recommendations.
- Added `SelfHealingEngine` for conservative UI locator alternatives with confidence and rationale.
- Added uniqueness and visibility gates before accepting a healed locator.
- Integrated one-attempt locator healing into `UiExecutor` without mutating the test plan.
- Added healing audit details containing original locator, healed locator, confidence and reason.
- Added regression tests for retry budgets/backoff and locator candidate generation.
- Updated Maven version and complete documentation to 3.28.0.

## 3.27.0

- Added deterministic `FailureIntelligence` for classifying execution failures.
- Added categories for assertion, timeout, authentication, network, locator, validation, server and unknown failures.
- Added bounded retry recommendations: retry, retry with backoff, do not retry and locator healing.
- Added `AgentRunner.analyzeFailureIntelligence(...)` for programmatic access to recommendations.
- Kept recommendations deterministic and independent of the LLM to prevent unsafe/unbounded AI-controlled retries.
- Added regression tests for timeout, authentication and locator classifications.
- Updated complete project documentation and Maven version to 3.27.0.

## 3.26.0

- Added provider-neutral `AiProvider` abstraction and made `OllamaClient` implement it.
- Added structured `AiIntelligenceResult` for requirement summaries, generated scenarios, coverage, missing-test detection and duplicate groups.
- Added multi-scenario positive, negative, boundary, authentication, validation and resilience generation.
- Added `AgentRunner.intelligence(...)` and `TestOrchestrator.intelligence(...)` with strict validation.
- Added regression tests with deterministic injected AI responses.
- Aligned `TestPlanValidator` with Phase 3 API and Phase 4 UI action contracts.
- Updated Maven and documentation to 3.26.0.

## 3.25.0

- Hardened Playwright browser/context lifecycle with guaranteed cleanup.
- Added `DOMContentLoaded` navigation waiting and locator state waits.
- Added hover, check, uncheck, title/URL assertions and automatic screenshots.
- Applied security redaction to UI failure details.

## 3.24.0

- Expanded API execution to GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS.
- Added forms, authentication, JSONPath/XMLPath assertions and response-variable extraction.

## 3.23.0

- Standardized the three-file report package and added report integrity validation.

## 3.22.0

- Hardened stability, configuration bounds, standardized errors and secure durable logging.

## 3.21.0

- Expanded API/UI/suite/data-driven examples, screenshots, logs and unified reporting.

## 3.20.0

- Standardized repository organization and engineering support documentation.
