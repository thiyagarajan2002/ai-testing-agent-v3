# AI Testing Agent — v3.21.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, retries, artifacts, reporting, suites, history/analytics, security redaction, preflight validation and data-driven execution.

## v3.21.0 — Examples, UI Evidence & Durable Logs

This release adds a broader example library and stronger execution evidence.

### API examples

```text
examples/plans/api/
├── v3-plan-file.json
├── get-user.json
├── create-resource.json
└── save-variable.json
```

API suite: `examples/suites/api-smoke-suite.json`

Run:

```bash
mvn exec:java -Dexec.args="plan examples/plans/api/get-user.json"
```

Every API test creates a durable request/response log under:

```text
reports/api/logs/<test-name>-<timestamp>.log
```

The log contains timestamp, step, attempt, action, URL, request body, response status, duration, bounded response body and errors. Sensitive values are redacted before persistence.

### UI examples

```text
examples/plans/ui/
├── homepage-smoke.json
├── login-flow.json
├── search-flow.json
└── negative-login.json
```

UI suite: `examples/suites/ui-smoke-suite.json`

Run:

```bash
mvn exec:java -Dexec.args="plan examples/plans/ui/login-flow.json"
```

Install Chromium first when needed:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

### Automatic screenshot after every UI step

Every successful UI step automatically captures a full-page screenshot. Failed UI steps capture a failure screenshot too.

```text
reports/screenshots/ui/
├── <test-name>/
│   ├── 001-navigate.png
│   ├── 002-fill.png
│   ├── 003-click.png
│   └── 004-assertvisible.png
└── failures/
    └── <test-name>-step-4-failure.png
```

The screenshot path is attached to the step result. The existing explicit `screenshot` action remains supported for compatibility.

### Terminal run logs

The application now tees stdout/stderr: output remains visible in the terminal and is simultaneously persisted to a timestamped log.

API-oriented CLI commands:

```text
reports/api/logs/terminal-<timestamp>-<command>.log
```

Interactive runs:

```text
reports/ui/logs/terminal-<timestamp>-interactive.log
```

Other commands:

```text
reports/terminal/logs/
```

The terminal log includes the command, application output/errors and final exit code.

## Core commands

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]"
mvn exec:java -Dexec.args="validate plan <file>"
mvn exec:java -Dexec.args="validate suite <file>"
mvn exec:java -Dexec.args="interactive"
```

## Repository layout

```text
ai-testing-agent-v3/
├── .github/                 # CI/CD
├── config/                  # Environment profiles and reusable data
├── docs/                    # Architecture, configuration, testing and releases
├── examples/                # API/UI plans, suites, datasets and requirements
├── scripts/                 # Developer/CI helpers
├── src/main/java/           # Production Java source
├── src/main/resources/      # Runtime resources
├── src/test/java/           # Automated tests
├── src/test/resources/      # Test fixtures
├── reports/                 # Generated runtime evidence
├── pom.xml
├── README.md
├── PROJECT_DETAILS.md
├── CONTRIBUTING.md
└── SECURITY.md
```

## Documentation

- `PROJECT_DETAILS.md` — complete implementation, methods, execution behavior and release history.
- `docs/architecture/` — architecture and package boundaries.
- `docs/configuration/` — configuration guidance.
- `docs/testing/` — testing standards.
- `docs/releases/` — release history.
- `examples/README.md` — API/UI examples and evidence/logging guide.

## Security

Do not store production credentials in plans or datasets. Sensitive execution diagnostics, API logs and AI failure-analysis input are redacted before persistence.

## Version history

- **v3.21.0** — comprehensive API/UI examples, automatic UI screenshots, API execution logs and terminal log persistence
- v3.20.0 — repository organization and engineering standards
- v3.19.0 — parallel data-driven execution, ordered results and performance metrics
- v3.18.0 — CSV datasets, dataset validation and exact row filtering
- v3.17.0 — data-driven reporting and HTML dashboard
- v3.16.0 — data-driven / parameterized test execution
- v3.15.0 — preflight validation
- v3.14.0 — history analytics and flaky-test analysis
- v3.13.0 — execution history and reliability improvements
- v3.12.0 — execution history foundations
- v3.11.0 — execution history and run comparison
- v3.10.0 — sensitive-data redaction
- v3.9.0 — advanced assertions
- v3.8.0 — environment profiles and test data
- v3.7.0 — GitHub Actions CI/CD
- v3.6.0 — suite and parallel execution
- v3.5.0 — advanced PDF/reporting
- v3.4.0 — failure artifacts
- v3.3.0 — advanced reporting
- v3.2.0 — retry support

## Repository

urlAI Testing Agent v3 on GitHubhttps://github.com/thiyagarajan2002/ai-testing-agent-v3
