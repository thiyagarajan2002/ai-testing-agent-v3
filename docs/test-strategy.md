# AI Testing Agent v3.44.0 Test Strategy

## Purpose

v3.44.0 makes sanity and regression coverage explicit for API, UI, AI planning, locatorless execution, and model serialization. The strategy is designed to catch fast contract failures before the full Playwright and repository-example suite runs.

## Test layers

| Layer | Scope | Network | Primary goal | Tag |
|---|---|---:|---|---|
| API sanity | Core API request/extraction path | Local test server | Fast health check | `sanity` |
| API regression | HTTP methods, headers, retry, assertions | Local test server | Protect existing API behavior | `regression` |
| UI sanity | Navigation and explicit Playwright actions | Local test server + Chromium | Verify basic browser execution | `sanity` |
| UI regression | Semantic target resolution and DOM state changes | Local test server + Chromium | Protect locatorless/self-healing flow | `regression` |
| Planner/validation sanity | Plan schema and semantic target rules | None | Reject invalid plans early | `sanity` |
| Planner/serialization regression | Prompt contract and Jackson round-trip | None | Preserve model compatibility | `regression` |
| Full suite | All unit and execution tests | Mixed | Final release confidence | No tag filter |
| Examples | Repository plans/suites and CLI examples | Depends on example | Validate user-facing workflows | N/A |

## Required checks

### API

Sanity checks must cover a representative GET request and variable extraction. Regression checks must cover POST JSON, headers, all supported HTTP methods, retry recovery, and assertion failure handling.

### UI

Sanity checks must prove browser navigation and basic actions work. Regression checks must prove that semantic targets are resolved against the live DOM, that a state-changing action causes the next target to be resolved from the new DOM, and that an AI-provided selector that does not exist is rejected instead of executed.

### AI locatorless flow

The framework must keep natural-language `target` separate from concrete `locator`. A generated plan may contain a semantic target without a locator. During live execution/generation, the resolver must inspect current DOM evidence, validate the candidate selector, and only then execute the action. Generated Java code may contain the verified concrete locator.

### Code generation

Generated UI code must use concrete verified locators and retain the intended action sequence. Natural-language targets must not be emitted as Playwright selectors.

## Maven commands

Run the focused suites with:

```bash
mvn -B -Psanity test
mvn -B -Pregression test
```

Run the complete test suite with:

```bash
mvn -B verify
```

The focused profiles use JUnit 5 tags through Maven Surefire. The default build remains unfiltered so the complete suite is still the final regression gate.

## CI order

1. Validate CI scripts.
2. Compile the project.
3. Validate a representative API plan.
4. Validate a representative suite.
5. Install Playwright Chromium.
6. Run the focused sanity suite.
7. Run the focused regression suite.
8. Run the complete Maven verification suite.
9. Run all repository examples.
10. Upload reports, screenshots, and the runnable shaded JAR.

## Release gate

A release candidate is considered healthy only when the sanity profile, regression profile, full `verify` suite, and repository examples all succeed. A failed focused suite must be fixed rather than hidden by the full-suite run.

## Future expansion

The next coverage additions should tag and isolate:

- AI planner contract tests
- semantic locator ranking and alternative-selector tests
- code-generation compile tests
- CLI command tests
- self-healing and retry-history regression tests
- report/history integrity tests
- parallel execution regression tests

These additions should continue using deterministic local fixtures wherever possible so CI does not depend on public websites or external AI services.
