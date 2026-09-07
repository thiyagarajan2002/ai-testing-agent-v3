# AI Testing Agent — v3.8.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.8.0 — Test Data Management & Environment Profiles

Version 3.8 adds environment-aware execution without changing the base test-plan files. Profiles provide environment-specific base URLs, headers, variables, timeouts, and optional JSON test data.

### New capabilities
- JSON environment profiles under `config/environments/`.
- Optional JSON test data files referenced by a profile.
- CLI environment selection with `--env <name>`.
- `${profile.X}` placeholders for profile variables.
- `${data.X}` placeholders for values from the profile's test-data file.
- `${env.X}` placeholders for operating-system environment variables.
- Profile headers are merged with test-step headers; step headers take precedence.
- Profile timeout applies to steps that still use the default 30000 ms timeout.
- Suite execution applies the same profile independently to every test.
- No new YAML dependency is required; JSON keeps configuration consistent with test plans.

### Directory structure

```text
config/
├── environments/
│   └── qa.json
└── test-data/
    └── qa.json
```

### Example environment profile

`config/environments/qa.json`:

```json
{
  "name": "qa",
  "baseUrl": "https://qa.example.com/api",
  "timeoutMs": 15000,
  "dataFile": "config/test-data/qa.json",
  "variables": {
    "tenant": "demo"
  },
  "headers": {
    "Accept": "application/json",
    "X-Tenant": "${profile.tenant}"
  }
}
```

### Example test data

`config/test-data/qa.json`:

```json
{
  "username": "demo-user",
  "resourceId": "1",
  "sampleText": "qa-test"
}
```

Use values in a plan like:

```json
{
  "path": "/users/${data.resourceId}",
  "body": "{\"name\":\"${data.username}\"}"
}
```

Environment variables can be used for secrets without storing them in Git:

```text
${env.API_TOKEN}
```

> Never commit real credentials, tokens, cookies, passwords, or production secrets to profile or data files.

### Environment selection

Single plan:

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json --env qa"
```

Suite:

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json --env qa"
```

PowerShell:

```powershell
mvn exec:java '-Dexec.mainClass=com.thiyagarajan.agent.Main' '-Dexec.args=plan examples/v3-plan-file.json --env qa'
mvn exec:java '-Dexec.mainClass=com.thiyagarajan.agent.Main' '-Dexec.args=suite examples/v3-suite.json --env qa'
```

If `--env` is omitted, the existing default behavior is preserved and no profile is loaded.

### Placeholder resolution

| Placeholder | Source |
|---|---|
| `${profile.tenant}` | `variables` in the selected environment profile |
| `${data.resourceId}` | JSON object in the profile's `dataFile` |
| `${env.API_TOKEN}` | Operating-system environment variable |

Unresolved placeholders fail fast instead of silently sending an incorrect request.

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

### Supported actions

API: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.

UI: `navigate`, `click`, `fill`, `press`, `selectOption`, `assertVisible`, `assertText`, `assertValue`, `waitFor`, `screenshot`.

### Version history

- v3.2.0 — Retry & resilience engine
- v3.3.0 — Advanced execution reporting and logs
- v3.4.0 — Failure artifacts & screenshot management
- v3.5.0 — Real PDF reporting & execution dashboard
- v3.6.0 — Test suite & parallel execution engine
- v3.7.0 — CI/CD & GitHub Actions integration
- v3.8.0 — Test data management & environment profiles

### Safety

The LLM remains restricted to the fixed JSON test-plan schema. Runtime execution remains limited to explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.

Keep secrets in CI/environment variables and use `${env.NAME}` rather than committing them to repository files. Failure artifacts may contain request/response evidence, so review data handling before publishing reports.
