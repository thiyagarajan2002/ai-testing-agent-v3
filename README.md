# AI Testing Agent — v3.6.0

AI-assisted API and UI test planning and execution using Java 21, Ollama, REST Assured, and Playwright.

## v3.6.0 — Test suite & parallel execution engine

Version 3.6 adds a production-style suite runner for executing multiple API/UI test-plan files with configurable parallelism, isolated runtime state, individual reports, and aggregated suite reporting.

### New capabilities
- Execute multiple test-plan JSON files from one `TestSuite` JSON file.
- Parallel execution with configurable `PARALLELISM` thread count.
- Fresh API/UI executor state per test, preventing API variable state from leaking between parallel tests.
- Failure isolation: one failed test does not cancel the other suite tasks.
- Individual HTML, CSV, PDF, JSON, and log reports under `reports/suite/tests/<number>/`.
- Aggregated suite HTML dashboard, CSV, JSON, and real PDF reports.
- Suite-level pass/fail statistics and total duration.
- CI-friendly exit code: suite command exits with code `1` when any test fails.
- Suite plan path validation prevents a plan from escaping the suite directory.
- Existing single-plan execution and v3.5 dashboard/PDF reporting remain supported.

### Suite input

Example `examples/v3-suite.json`:

```json
{
  "name": "Smoke Suite",
  "plans": [
    "../examples/v3-plan-file.json"
  ]
}
```

Each entry in `plans` points to a test-plan JSON file. Paths are resolved relative to the suite file directory.

### Run a single plan

```bash
mvn clean test
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=plan examples/v3-plan-file.json"
```

### Run a suite

```bash
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json"
```

PowerShell alternative:

```powershell
mvn exec:java '-Dexec.mainClass=com.thiyagarajan.agent.Main' '-Dexec.args=suite examples/v3-suite.json'
```

### Parallelism

Default:

```text
PARALLELISM=4
```

Linux/macOS:

```bash
export PARALLELISM=2
mvn exec:java "-Dexec.mainClass=com.thiyagarajan.agent.Main" "-Dexec.args=suite examples/v3-suite.json"
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
    ├── suite-report.html       # Aggregated browser dashboard
    ├── suite-report.csv        # Aggregated tabular result
    ├── suite-report.pdf        # Real aggregated PDF
    ├── suite-execution.json    # Full machine-readable suite result
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

### Suite execution flow

```text
TestSuite JSON
     |
     v
Validate plan paths
     |
     v
Create fixed thread pool
     |
     +---- Test 1 ----> Fresh API/UI executor ----> ExecutionResult
     |
     +---- Test 2 ----> Fresh API/UI executor ----> ExecutionResult
     |
     +---- Test N ----> Fresh API/UI executor ----> ExecutionResult
     |
     v
Aggregate results
     |
     +--> suite-execution.json
     +--> suite-report.csv
     +--> suite-report.html
     +--> suite-report.pdf
     +--> individual reports
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

### Safety

The LLM remains restricted to the fixed JSON test-plan schema. Runtime execution remains limited to explicitly supported API and UI actions; arbitrary shell, Java, JavaScript, or SQL execution is not introduced.

Do not persist API credentials, tokens, cookies, or other secrets in test plans or failure artifacts.
