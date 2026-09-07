# AI Testing Agent — v3.2.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.2.0 — Retry & resilience engine

Version 3.2 adds controlled API retry behavior while retaining the v3.1 suite execution and safety model.

### New capabilities
- `retryCount` can be configured independently on each test step.
- `retryCount` means the number of retries **after** the initial request.
- A step with `retryCount: 2` can therefore execute up to 3 attempts.
- API request exceptions and failed response assertions are retried until the attempt limit is reached.
- Response-time assertions are evaluated against the duration of the current attempt, not previous attempts.
- Attempt number, maximum attempts, and total step duration are included in execution details.
- Response variables are saved only after a successful assertion, preventing failed attempts from corrupting extracted state.
- Negative retry values are rejected during plan validation before execution.
- Existing plans remain compatible: omitted `retryCount` means no retry (`0`).

### Retry example

```json
{
  "action": "GET",
  "path": "/health",
  "retryCount": 2,
  "assertSpec": {
    "status": 200
  }
}
```

With `retryCount: 2`, execution is:

```text
Attempt 1 -> failure -> retry
Attempt 2 -> failure -> retry
Attempt 3 -> success/final failure
```

## Run the retry example

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json"
```

## Run a suite

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json"
```

## Run AI interactive mode

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=interactive"
```

## Environment configuration

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless execution |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default API/UI timeout |
| `REPORTS_DIR` | `reports` | Report root directory |
| `SCREENSHOTS_DIR` | `screenshots` | Screenshot subdirectory |

## Supported API actions
GET, POST, PUT, PATCH, DELETE

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

This commit represents milestone `v3.2.0` — Retry & resilience engine.
