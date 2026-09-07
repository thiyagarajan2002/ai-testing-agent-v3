# AI Testing Agent — v3.3.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.3.0 — Advanced execution reporting and logs

Version 3.3 adds persistent execution artifacts so every completed execution can be inspected after the run.

### New capabilities
- `execution.json` machine-readable execution result.
- `execution.log` human-readable run log.
- Report output directory now follows `REPORTS_DIR` from configuration.
- ReportManager supports an explicit output directory for tests and integrations.
- HTML report includes step count, pass/fail totals, duration, and execution details.
- Retry attempt information produced by v3.2 is preserved in JSON, text, HTML, and CSV artifacts.
- Null result and invalid report-directory inputs are rejected clearly.

### Report artifacts

After an execution, the configured report directory contains:

| File | Purpose |
|---|---|
| `report.html` | Browser-friendly human report |
| `report.csv` | Spreadsheet/CI-friendly tabular report |
| `report.pdf` | Text report artifact |
| `execution.json` | Structured execution data |
| `execution.log` | Complete readable execution log |

### Execution log content

The log records the test name, overall status, step number, action, step status, duration, and execution details. API details include HTTP status, retry attempt information, response content (abbreviated), and saved variables where applicable.

### Configuration

`REPORTS_DIR` controls where all report and execution-log artifacts are written. Default: `reports`.

```bash
# Linux/macOS
export REPORTS_DIR=build/test-reports

# PowerShell
$env:REPORTS_DIR="build/test-reports"
```

### Build and test

```bash
mvn clean test
mvn clean compile
```

## Versioning

This commit represents milestone `v3.3.0`.

## Safety

The LLM remains restricted to the fixed JSON test-plan schema. Runtime execution remains limited to explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.
