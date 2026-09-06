# AI Testing Agent — v3.1.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.1.0 — Test Suite execution

Version 3.1 adds deterministic multi-plan suite execution while retaining direct plan execution and the v2 safety model.

### New capabilities
- `TestSuite` model containing a suite name and ordered plan-file list.
- `suite <file>` command in the CLI.
- Plans are resolved relative to the suite file directory.
- Each plan is validated and executed through the same runtime used by direct plan execution.
- AI failure analysis is attempted for failed plans without stopping the remaining suite plans.
- Suite summary reports total, passed, and failed plans.
- CLI returns exit code `1` when any suite plan fails, making suite execution CI-friendly.

## Run a suite

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json"
```

Example suite:

```json
{
  "name": "Smoke Suite",
  "plans": [
    "../examples/v3-plan-file.json"
  ]
}
```

Plan paths are resolved relative to the suite file location.

## Run a JSON plan directly

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json"
```

A direct plan must match the `TestPlan` schema: `name`, `type`, `baseUrl`, optional `variables`, and `steps`.

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

This commit represents milestone `v3.1.0`.
