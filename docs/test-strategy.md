# AI Testing Agent v3.45.0 Test Strategy

## Purpose

v3.45.0 strengthens sanity and regression coverage around AI-driven locatorless UI testing. The goal is to prevent an AI-generated selector from becoming executable code unless it is supported by the live DOM and meets the locator confidence policy.

## Test layers

| Layer | Scope | Network | Primary goal | Tag |
|---|---|---:|---|---|
| API sanity | Core API request/extraction path | Local test server | Fast health check | `sanity` |
| API regression | HTTP methods, headers, retry, assertions | Local test server | Protect existing API behavior | `regression` |
| UI sanity | Navigation and explicit Playwright actions | Local test server + Chromium | Verify basic browser execution | `sanity` |
| UI regression | Semantic target resolution and DOM state changes | Local test server + Chromium | Protect locatorless execution | `regression` |
| Locator quality sanity | Stable/accessibility-oriented selector resolution | Local test server + Chromium | Verify evidence-backed resolution | `sanity` |
| Locator quality regression | alternatives, confidence, CSS-only contract, context | Local test server + Chromium | Prevent unsafe AI selectors | `regression` |
| Planner/validation | Plan schema and semantic target rules | None | Reject invalid plans early | `sanity` / `regression` |
| Code generation | Concrete locator emission and unresolved-locator rejection | None | Prevent invalid generated Java | `regression` |
| Full suite | All unit and execution tests | Mixed | Final release confidence | No tag filter |
| Examples | Repository plans/suites and CLI examples | Depends on example | Validate user-facing workflows | N/A |

## Locator quality contract

A semantic locator is executable only when all of these conditions hold:

1. The AI returns a non-empty candidate.
2. Confidence is finite, normalized to `0..1`, and at least `0.50`.
3. The candidate is a CSS selector, not XPath, Java, markdown, or generated framework code.
4. Playwright can parse the selector.
5. The selector matches at least one element in the current live DOM.
6. The first matching element is visible.
7. If the primary candidate fails, evidence-backed alternatives may be evaluated in order.
8. A state-changing action must cause the next semantic target to be resolved against the new DOM rather than a stale selector snapshot.

## Semantic target coverage

Regression fixtures should include:

- stable attributes and accessible names
- exact text and structural relationships
- ordinal targets such as `first video`
- contextual references to previous steps
- invalid selectors and invented attributes
- XPath/Java/markdown selector injection attempts
- low-confidence responses
- alternative selector fallback
- DOM changes after clicks or navigation

## API

Sanity checks cover a representative GET request and variable extraction. Regression checks cover POST JSON, headers, all supported HTTP methods, retry recovery, and assertion failure handling.

## UI

Sanity checks prove browser navigation and basic actions work. Regression checks prove semantic targets are resolved against the live DOM, changing DOM state is re-resolved, and invalid AI selectors are rejected.

## Code generation

Generated UI code must contain concrete locators produced after live verification. Natural-language targets must never be emitted as Playwright selectors. Direct generation from an unresolved semantic target is rejected instead of producing a broken test.

## Maven commands

Focused suites:

```bash
mvn -B -Psanity test
mvn -B -Pregression test
```

Complete suite:

```bash
mvn -B verify
```

## CI release gate

A release candidate is healthy only when the sanity profile, regression profile, full `verify` suite, and repository examples all succeed. Focused suites are intentionally deterministic and use local fixtures where possible. No public website or external AI service is required by the locator quality regression tests.

## Future expansion

The next coverage additions should isolate:

- generated Java compilation tests
- CLI `generate` end-to-end tests with a deterministic AI provider
- AI planner contract tests
- self-healing and retry-history regression tests
- report/history integrity tests
- parallel execution regression tests
- locator ranking metrics and selector stability scoring
