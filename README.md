# AI Testing Agent — v3.4.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.4.0 — Failure artifacts & screenshot management

Version 3.4 extends the v3.3 execution reporting layer with persistent failure artifacts. Failed API and UI steps now leave evidence on disk, and report pages expose those artifacts.

### New capabilities
- `FailureArtifactManager` centralizes failure-artifact creation.
- UI failures automatically capture a screenshot when a browser page is available.
- UI failures also create JSON failure metadata.
- API failures create a readable `*-api.txt` artifact containing action, URL, request body, validation details, and abbreviated response information.
- Failure artifact names are filesystem-safe and include test/step context.
- `ExecutionResult.StepResult` now exposes an `artifacts` list while retaining the original constructor for compatibility.
- HTML reports link directly to step artifacts.
- CSV reports include an `artifacts` column.
- Existing `execution.json` and `execution.log` automatically preserve the new artifact paths through Jackson serialization.
- API response-variable extraction is now atomic: variables are committed only after all requested JSON paths are successfully extracted.

### Artifact layout

With the default configuration:

```text
reports/
├── report.html
├── report.csv
├── report.pdf
├── execution.json
├── execution.log
└── screenshots/
    ├── My_Test-step-1-failure.png
    ├── My_Test-step-1-20260907-093000-000+0530.json
    └── My_Test-step-2-20260907-093001-000+0530-api.txt
```

The timestamped metadata/API artifacts prevent collisions between repeated executions. The UI failure screenshot uses the stable test/step name so the latest failure evidence is easy to find; a later execution can replace that same screenshot.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_URL` | `http://localhost:11434` | Ollama server |
| `OLLAMA_MODEL` | `llama3.2` | Ollama model |
| `HEADLESS` | `true` | Playwright headless execution |
| `DEFAULT_TIMEOUT_MS` | `30000` | Default API/UI timeout |
| `REPORTS_DIR` | `reports` | Report root directory |
| `SCREENSHOTS_DIR` | `screenshots` | Failure-artifact subdirectory |

### Run

```bash
mvn clean test
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json"
```

For a custom report directory:

```bash
# Linux/macOS
export REPORTS_DIR=build/test-reports
export SCREENSHOTS_DIR=artifacts

# PowerShell
$env:REPORTS_DIR="build/test-reports"
$env:SCREENSHOTS_DIR="artifacts"
```

### Supported actions

API: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`.

UI: `navigate`, `click`, `fill`, `press`, `selectOption`, `assertVisible`, `assertText`, `assertValue`, `waitFor`, `screenshot`.

### Failure handling

A failed UI step attempts two artifacts: a screenshot and JSON metadata. If screenshot capture is unavailable, metadata is still retained. A failed API step creates a text evidence artifact. Artifact-generation failures do not hide the original test failure.

Request/response evidence should be treated as test data. Do not place secrets directly in request bodies or plans when artifacts are persisted to shared CI storage.

### Compatibility

The existing four-argument `StepResult` constructor remains available. `retryCount` behavior from v3.2 is unchanged: an omitted value means zero retries, while `retryCount: 2` permits three total attempts.

### Versioning

This commit represents milestone `v3.4.0` — Failure artifacts & screenshot management.

## Safety

The LLM remains restricted to the fixed JSON test-plan schema. Runtime execution remains limited to explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.
