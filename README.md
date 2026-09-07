# AI Testing Agent — v3.9.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.9.0 — Advanced Assertions & Validation Diagnostics

Version 3.9 extends API validation from a small fixed set of checks to a typed assertion engine. Existing `assertSpec` plans remain compatible, while the new `assertions` array supports multiple checks on the same response and reports exactly which validations failed.

### New capabilities
- Multiple typed API assertions per step.
- HTTP status validation.
- Response-body contains / not-contains validation.
- Regular-expression validation against the full response body.
- Case-sensitive response-header equality validation.
- JSONPath existence validation.
- JSONPath exact-value validation.
- JSONPath contains validation.
- JSONPath regular-expression validation.
- Response-time limit validation.
- Placeholder substitution inside assertion expected values.
- Detailed assertion failure diagnostics in execution results and failure artifacts.
- Backward compatibility with the existing `assertSpec` object.

### Advanced assertion schema

A step can contain multiple assertions:

```json
{
  "action": "GET",
  "path": "/users/1",
  "assertions": [
    {"type": "status", "expected": "200"},
    {"type": "headerEquals", "path": "Content-Type", "expected": "application/json"},
    {"type": "jsonPathExists", "path": "$.id", "expected": "true"},
    {"type": "jsonPathEquals", "path": "$.id", "expected": "1"},
    {"type": "jsonPathRegex", "path": "$.name", "expected": ".+"},
    {"type": "bodyNotContains", "expected": "error"},
    {"type": "responseTimeMs", "expected": "1000"}
  ]
}
```

### Supported assertion types

| Type | `path` | `expected` | Purpose |
|---|---|---|---|
| `status` | not used | HTTP status | Validates status code |
| `bodyContains` | not used | text | Body must contain text |
| `bodyNotContains` | not used | text | Body must not contain text |
| `bodyRegex` | not used | regex | Body must match regex |
| `headerEquals` | header name | value | Validates a response header |
| `jsonPathExists` | JSONPath | `true` / `false` | Checks JSONPath presence |
| `jsonPathEquals` | JSONPath | value | Exact JSONPath value |
| `jsonPathContains` | JSONPath | text | JSONPath value contains text |
| `jsonPathRegex` | JSONPath | regex | JSONPath value matches regex |
| `responseTimeMs` | not used | milliseconds | Maximum allowed response time |

### Legacy compatibility

The existing format continues to work:

```json
{
  "assertSpec": {
    "status": 200,
    "contains": "success",
    "jsonPath": "$.id",
    "equals": "1",
    "responseTimeMs": 1000
  }
}
```

The runtime evaluates both `assertSpec` and the new `assertions` list when both are present.

### Assertion diagnostics

When validation fails, the step details include an `assertionFailures` field, for example:

```text
HTTP 200; attempt=1/1; durationMs=143; assertionFailures=status expected=201, actual=200 | jsonPath '$.id' expected='99', actual='1'
```

This makes reports and AI failure analysis more useful because the execution result identifies the failed condition rather than returning only `passed=false`.

### Environment profiles

v3.8 environment profiles remain available:

```text
config/
├── environments/
│   └── qa.json
└── test-data/
    └── qa.json
```

Run a plan with an environment:

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json --env qa"
```

Run a suite with an environment:

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json --env qa"
```

Placeholders supported by environment profiles:

| Placeholder | Source |
|---|---|
| `${profile.tenant}` | Profile variables |
| `${data.resourceId}` | Profile test-data JSON |
| `${env.API_TOKEN}` | Operating-system environment variable |

Never commit credentials, tokens, cookies, passwords, or production secrets to profile/data files.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless execution |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default API/UI timeout |
| `PARALLELISM` | `4` | Maximum parallel suite workers |
| `REPORTS_DIR` | `reports` | Report root directory |
| `SCREENSHOTS_DIR` | `screenshots` | Failure-artifact subdirectory |

### CI/CD

GitHub Actions remains enabled through `.github/workflows/ci.yml`. CI runs Java 21, installs Playwright Chromium, executes `mvn -B clean verify`, and uploads generated reports.

### Reports

Single-test reports are written under `reports/`. Suite execution produces:

```text
reports/suite/
├── suite-report.html
├── suite-report.csv
├── suite-report.pdf
├── suite-execution.json
└── tests/
    ├── 1/
    │   ├── report.html
    │   ├── report.csv
    │   ├── report.pdf
    │   ├── execution.json
    │   └── execution.log
    └── 2/
        └── ...
```

Failed API steps also create API evidence artifacts containing the request body, URL, response body, and assertion diagnostics. Review artifact handling before publishing reports.

### Supported actions

API: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.

UI: `navigate`, `click`, `fill`, `press`, `selectOption`, `assertVisible`, `assertText`, `assertValue`, `waitFor`, `screenshot`.

### Project structure

```text
src/main/java/com/thiyagarajan/agent/
├── ai/
├── config/
├── model/
├── report/
└── runtime/
    ├── ApiAssertionEngine.java
    ├── ApiExecutor.java
    ├── SuiteExecutionEngine.java
    └── UiExecutor.java
```

### Version history

- v3.2.0 — Retry & resilience engine
- v3.3.0 — Advanced execution reporting and logs
- v3.4.0 — Failure artifacts & screenshot management
- v3.5.0 — Real PDF reporting & execution dashboard
- v3.6.0 — Test suite & parallel execution engine
- v3.7.0 — CI/CD & GitHub Actions integration
- v3.8.0 — Test data management & environment profiles
- v3.9.0 — Advanced assertions & validation diagnostics

### Safety

The LLM remains restricted to the fixed JSON test-plan schema. Runtime execution remains limited to explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.

Keep secrets in CI/environment variables and use `${env.NAME}` rather than committing them to repository files. Failure artifacts may contain request/response evidence, so review data handling before publishing reports.
