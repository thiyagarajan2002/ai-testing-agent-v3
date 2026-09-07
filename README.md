# AI Testing Agent — v3.7.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.7.0 — CI/CD & GitHub Actions

Version 3.7 adds a GitHub Actions CI pipeline that automatically builds and tests the project on pushes and pull requests to `main`, supports manual workflow dispatch, installs the Playwright Chromium browser, and publishes the generated `reports/` directory as a workflow artifact.

### New capabilities
- GitHub Actions workflow at `.github/workflows/ci.yml`.
- Runs automatically on pushes to `main`.
- Runs automatically on pull requests targeting `main`.
- Supports manual execution through `workflow_dispatch`.
- Uses Java 21 Temurin.
- Enables Maven dependency caching.
- Installs Playwright Chromium in the runner.
- Runs `mvn -B clean verify`.
- Configures CI for headless execution and `PARALLELISM=4`.
- Uploads `reports/` after success or failure for troubleshooting.
- Uses a 15-minute job timeout.
- Uses read-only repository contents permission.

### GitHub Actions flow

```text
Push / Pull Request / Manual Run
              |
              v
       Checkout repository
              |
              v
       Setup Java 21
              |
              v
        Maven dependency cache
              |
              v
     Install Playwright Chromium
              |
              v
       mvn clean verify
              |
          +---+---+
          |       |
        PASS     FAIL
          |       |
          +---+---+
              |
              v
       Upload reports artifact
```

### Workflow file

```text
.github/
└── workflows/
    └── ci.yml
```

### CI commands

The workflow executes the equivalent of:

```bash
mvn -B clean verify
```

with:

```text
HEADLESS=true
PARALLELISM=4
```

Playwright Chromium is installed before the Maven verification step.

### Reports in GitHub Actions

The workflow attempts to upload:

```text
reports/
```

as the artifact:

```text
ai-testing-agent-reports
```

The artifact is retained for 14 days. Uploading is configured with `if: always()`, so reports can still be collected when tests fail, provided files were generated.

### Local execution

Run tests locally:

```bash
mvn clean verify
```

Run a single plan:

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json"
```

Run a suite:

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json"
```

PowerShell:

```powershell
mvn clean verify
mvn exec:java '-Dexec.mainClass=com.thiyagarajan.agent.Main' '-Dexec.args=suite examples/v3-suite.json'
```

### Parallelism

Default:

```text
PARALLELISM=4
```

PowerShell:

```powershell
$env:PARALLELISM="2"
mvn exec:java '-Dexec.mainClass=com.thiyagarajan.agent.Main' '-Dexec.args=suite examples/v3-suite.json'
```

The actual worker count is `min(PARALLELISM, number of tests)`. A value below `1` is normalized to `1`.

### Suite report structure

```text
reports/
└── suite/
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

### Safety

The LLM remains restricted to the fixed JSON test-plan schema. Runtime execution remains limited to explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.

Do not persist API credentials, tokens, cookies, or other secrets in test plans or failure artifacts.
