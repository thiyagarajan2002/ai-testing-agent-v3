# AI Testing Agent — v3.0.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.0.0 — Runtime & deterministic execution

Version 3 introduces centralized runtime configuration and deterministic execution of JSON test plans from files, while retaining the v2 safety model.

### New capabilities
- Centralized `Config` loaded from environment variables.
- Configurable Ollama URL and model.
- Configurable Playwright headless mode.
- Configurable default timeout.
- Configurable screenshot directory.
- Direct JSON plan-file execution without requiring an LLM-generated plan.
- Clear failure when a requested plan file does not exist.
- API timeout uses the per-step timeout when supplied, otherwise the global default.
- Existing variables, assertions, request chaining, fail-fast behavior, UI actions, screenshots, and AI failure analysis are retained.

## Environment configuration

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless execution |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default API/UI timeout |
| `REPORTS_DIR` | `reports` | Report root directory |
| `SCREENSHOTS_DIR` | `screenshots` | Screenshot subdirectory |

## Run AI interactive mode

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=interactive"
```

## Run a JSON plan directly

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v2-execution.json"
```

A direct plan must match the `TestPlan` schema: `name`, `type`, `baseUrl`, optional `variables`, and `steps`.

## Supported API actions
GET, POST, PUT, PATCH, DELETE

API fields: `path`, `headers`, `query`, `body`, `assertSpec`, `save`, `timeoutMs`.

Assertions: HTTP status, body contains, JSONPath existence/equality, and maximum response time.

## Supported UI actions
navigate, click, fill, press, selectOption, assertVisible, assertText, assertValue, waitFor, screenshot.

## Safety

The LLM remains restricted to a fixed JSON test-plan schema. Runtime execution is limited to explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.

## Build and test

```bash
mvn clean test
mvn clean compile
```

## Versioning

This commit represents milestone `v3.0.0`.
