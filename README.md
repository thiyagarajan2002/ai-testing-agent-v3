# AI Testing Agent — v2.1.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v2.1.0 reliability improvements

- API variables are reset and initialized from `plan.variables` for every execution.
- API execution validates the plan and required `baseUrl` before making requests.
- UI execution validates the plan and required `baseUrl` before opening the browser.
- UI actions are case-insensitive, so `GET`-style casing differences do not break supported action matching.
- AgentRunner validates plan type, base URL, steps, and supported actions before execution.
- Blank LLM requirements and empty LLM responses are rejected clearly.
- Existing fail-fast behavior is retained.
- Existing UI failure screenshots are retained.

## Supported API actions

GET, POST, PUT, PATCH, DELETE

API step fields include `path`, `headers`, `query`, `body`, `assertSpec`, `save`, and `timeoutMs`.

Assertions include HTTP status, body contains, JSONPath existence/equality, and maximum response time.

## Supported UI actions

navigate, click, fill, press, selectOption, assertVisible, assertText, assertValue, waitFor, screenshot.

## Variable flow

Define initial variables in a plan:

```json
"variables": {"environment": "test"}
```

Use them as `${environment}`. API `save` entries extract JSONPath values from a response for later steps. Variables are isolated per API execution.

## Safety

The LLM is restricted to a fixed test-plan schema. The Java runtime executes only explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.

## Build

```bash
mvn clean test
mvn clean compile
```

## Run

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=interactive"
```

## Versioning

This commit represents milestone `v2.1.0`.
