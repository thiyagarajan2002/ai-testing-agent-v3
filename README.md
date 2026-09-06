# AI Testing Agent — v2.2.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v2.2.0 — Reporting Reliability

- Failed executions are AI-analyzed before the report is written.
- The final report can therefore contain the generated failure analysis.
- If AI failure analysis itself fails, the original execution result is still reported.
- Maven project version is `2.2.0`.

## v2.1.0 reliability improvements

- API variables are reset and initialized from `plan.variables` for every execution.
- API and UI execution validate required `baseUrl` and steps.
- UI actions are case-insensitive.
- AgentRunner validates plan type and supported actions before execution.
- Blank requirements and empty LLM responses are rejected clearly.
- Fail-fast execution and UI failure screenshots are retained.

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

The LLM is restricted to a fixed test-plan schema. The runtime executes only explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.

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

This commit represents milestone `v2.2.0`.
