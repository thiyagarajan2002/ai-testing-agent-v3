# AI Testing Agent — v3.5.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.5.0 — Real PDF reporting & execution dashboard

Version 3.5 upgrades the reporting layer with a real PDF document and a richer browser dashboard while retaining the v3.4 failure-artifact model.

### New capabilities
- `PdfReportWriter` generates a real PDF using iText.
- `report.pdf` is now a valid PDF artifact rather than a text file with a `.pdf` extension.
- HTML reporting is upgraded to an execution dashboard with step, pass, fail, and total-duration summary cards.
- Dashboard continues to expose failure analysis and direct links to JSON, log, CSV, and PDF artifacts.
- Step-level failure screenshots and API evidence remain linked from the dashboard.
- CSV reporting retains artifact paths for CI/spreadsheet processing.

### Report artifacts

```text
reports/
├── report.html       # Execution dashboard
├── report.csv        # Tabular report
├── report.pdf        # Real PDF report
├── execution.json    # Machine-readable execution result
├── execution.log     # Human-readable execution log
└── screenshots/      # Failure screenshots/evidence
```

### PDF contents

The generated PDF contains the test name, overall status, step totals, failure analysis, and a step-result table containing action, status, duration, and details.

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

Custom report location:

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

### Version history

- v3.2.0 — Retry & resilience engine
- v3.3.0 — Advanced execution reporting and logs
- v3.4.0 — Failure artifacts & screenshot management
- v3.5.0 — Real PDF reporting & execution dashboard

### Safety

The LLM remains restricted to the fixed JSON test-plan schema. Runtime execution remains limited to explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.
