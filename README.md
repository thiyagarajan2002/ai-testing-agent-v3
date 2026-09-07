# AI Testing Agent — v3.10.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.10.0 — Secure Secrets & Sensitive Data Redaction

Version 3.10 adds centralized protection for credentials and sensitive execution evidence. Runtime requests still receive the real values, but logs, reports, failure artifacts, and AI failure-analysis prompts are redacted before persistence or transmission.

### Security capabilities
- Centralized `SecurityRedactor` utility.
- Masks passwords, secrets, tokens, API keys, client secrets, authorization values, cookies, and common auth headers.
- Detects JSON secret fields such as `password`, `token`, `access_token`, `api_key`, and `client_secret`.
- Detects text forms such as `Authorization: Bearer ...`, `api_key=...`, and `password=...`.
- Masks values of sensitive operating-system environment variables when those values appear in output.
- Execution step details are redacted before they enter `ExecutionResult`.
- Failure-analysis output is redacted before it is stored.
- API failure artifacts redact URL, request body, response body, and diagnostic text.
- AI failure analysis receives a redacted serialized plan and redacted execution result.
- Existing execution behavior is preserved; redaction is applied to evidence/output rather than the live request.

### Redaction example

Input:

```text
Authorization: Bearer eyJhbGciOi.example-token
password=super-secret
{"api_key":"abc123-secret"}
```

Stored/report output:

```text
Authorization: ***REDACTED***
password=***REDACTED***
{"api_key":"***REDACTED***"}
```

### Important security boundary

Redaction is an evidence-protection layer, not a replacement for proper secret management. Keep credentials in CI secrets or operating-system environment variables and reference them using v3.8 environment placeholders such as `${env.API_TOKEN}`. Never commit real credentials to test plans, environment profiles, or test-data files.

## v3.9.0 — Advanced Assertions & Validation Diagnostics

Version 3.9 extends API validation from a small fixed set of checks to a typed assertion engine. Existing `assertSpec` plans remain compatible, while the `assertions` array supports multiple checks on the same response and reports exactly which validations failed.

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

Example:

```json
{
  "action": "GET",
  "path": "/users/1",
  "assertions": [
    {"type":"status","expected":"200"},
    {"type":"headerEquals","path":"Content-Type","expected":"application/json"},
    {"type":"jsonPathExists","path":"$.id","expected":"true"},
    {"type":"jsonPathEquals","path":"$.id","expected":"1"},
    {"type":"bodyNotContains","expected":"error"},
    {"type":"responseTimeMs","expected":"1000"}
  ]
}
```

The legacy format remains supported:

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

## v3.8.0 — Test Data Management & Environment Profiles

Profiles are stored under `config/environments/` and can provide base URLs, headers, variables, timeouts, and JSON test data.

```text
config/
├── environments/
│   └── qa.json
└── test-data/
    └── qa.json
```

Example profile:

```json
{
  "name": "qa",
  "baseUrl": "https://qa.example.com/api",
  "timeoutMs": 15000,
  "dataFile": "config/test-data/qa.json",
  "variables": {"tenant": "demo"},
  "headers": {
    "Accept": "application/json",
    "X-Tenant": "${profile.tenant}"
  }
}
```

Supported placeholders:

| Placeholder | Source |
|---|---|
| `${profile.tenant}` | Profile variables |
| `${data.resourceId}` | Profile test-data JSON |
| `${env.API_TOKEN}` | Operating-system environment variable |

Run:

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json --env qa"
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json --env qa"
```

Unresolved placeholders fail fast.

## v3.7.0 — CI/CD & GitHub Actions

`.github/workflows/ci.yml` runs on pushes and pull requests to `main`, uses Java 21 Temurin, installs Playwright Chromium, executes `mvn -B clean verify`, and uploads the `reports/` directory as a workflow artifact.

## v3.6.0 — Test Suite & Parallel Execution

Multiple JSON test plans can be executed through a `TestSuite`. Tests execute independently with configurable parallelism. Each test gets fresh API/UI executor state, preventing API variables from leaking between parallel tests.

Suite reports are written under:

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

A failed suite returns a non-zero process exit code for CI usage.

## v3.5.0 — PDF Reporting & Dashboard

Every execution produces HTML, CSV, PDF, JSON execution data, and execution logs. Failure artifacts are linked from the HTML report.

## v3.4.0 — Failure Artifacts

UI failures can create screenshots and metadata. API failures can create evidence containing URL, request body, response body, and diagnostic information. v3.10 now redacts sensitive content in these evidence files.

## v3.3.0 — Advanced Reporting

Execution results include per-step status, duration, details, failure analysis, and artifact references.

## v3.2.0 — Retry & Resilience

API steps support a step-level `retryCount`. A value of `0` means one attempt; `2` means up to three attempts.

## Architecture

```text
Requirement
    ↓
PromptManager → OllamaClient
    ↓
TestPlan JSON
    ↓
EnvironmentManager (optional profile/data resolution)
    ↓
AgentRunner
    ├── ApiExecutor → ApiAssertionEngine → REST Assured
    └── UiExecutor → Playwright
    ↓
ExecutionResult
    ↓
SecurityRedactor
    ↓
ReportManager / SuiteReportManager / Failure Artifacts
```

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
    ├── ExecutionResult.java
    ├── FailureArtifactManager.java
    ├── SecurityRedactor.java
    ├── SuiteExecutionEngine.java
    └── UiExecutor.java
```

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless execution |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default API/UI timeout |
| `PARALLELISM` | `4` | Maximum suite workers |
| `REPORTS_DIR` | `reports` | Report root |
| `SCREENSHOTS_DIR` | `screenshots` | Failure-artifact subdirectory |

## Supported actions

API: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.

UI: `navigate`, `click`, `fill`, `press`, `selectOption`, `assertVisible`, `assertText`, `assertValue`, `waitFor`, `screenshot`.

## Build and test

```bash
mvn -B clean verify
```

For Playwright browser installation:

```bash
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## Security checklist

1. Store secrets in CI/environment variables, not Git files.
2. Prefer `${env.NAME}` for secret injection.
3. Do not disable redaction when publishing reports.
4. Treat failure artifacts as potentially sensitive evidence even after masking.
5. Review custom data and assertion values before sharing reports externally.
6. The LLM remains restricted to the fixed test-plan schema; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.

## Version history

- v3.2.0 — Retry & resilience engine
- v3.3.0 — Advanced execution reporting and logs
- v3.4.0 — Failure artifacts & screenshot management
- v3.5.0 — Real PDF reporting & execution dashboard
- v3.6.0 — Test suite & parallel execution engine
- v3.7.0 — CI/CD & GitHub Actions integration
- v3.8.0 — Test data management & environment profiles
- v3.9.0 — Advanced assertions & validation diagnostics
- v3.10.0 — Secure secrets & sensitive-data redaction
