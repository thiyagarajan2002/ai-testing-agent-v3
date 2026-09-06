# AI Testing Agent — v2.0.0

## What changed from v1.0.0

Version 2 adds stronger execution controls while keeping the v1 safety model:

- API headers and query parameters
- runtime variable initialization and `${variable}` substitution
- response-time assertions
- response-body capture in execution details
- fail-fast step execution
- UI `assertValue` and `waitFor`
- configurable per-step timeout
- automatic failure screenshots for UI tests
- explicit validation for missing API/UI base URLs
- richer example plan

## Supported API actions
GET, POST, PUT, PATCH, DELETE

API step fields include `path`, `headers`, `query`, `body`, `assertSpec`, `save`, and `timeoutMs`.

Assertions include HTTP status, body contains, JSONPath existence/equality, and maximum response time.

## Supported UI actions
navigate, click, fill, press, selectOption, assertVisible, assertText, assertValue, waitFor, screenshot.

## Variable flow

A plan can define initial variables:

```json
"variables": {"environment": "test"}
```

Use them anywhere as `${environment}`. API `save` entries extract JSONPath values from a response for later steps.

## Failure handling

Execution stops after the first failed step. UI failures attempt to capture a screenshot under `reports/screenshots/`.

## Safety

The LLM is still restricted to a fixed JSON test-plan schema. No arbitrary shell, Java, JavaScript, or SQL execution is introduced.

## Example

See `examples/v2-execution.json` for API query/header usage and the enhanced UI flow.

## Versioning

This commit represents milestone `v2.0.0`.
