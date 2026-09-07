# Examples

The examples directory contains runnable templates for API, UI, suite and data-driven testing.

## API examples

- `plans/api/v3-plan-file.json` — retry, headers, query parameters and assertions.
- `plans/api/get-user.json` — GET request with query validation.
- `plans/api/create-resource.json` — POST request with JSON body.
- `plans/api/save-variable.json` — response extraction and variable reuse.
- `suites/api-smoke-suite.json` — API examples executed as a suite.

Run an API plan:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="plan examples/plans/api/get-user.json"
```

API logs are stored under `reports/api/logs/`. Each API execution creates a durable request/response log containing timestamps, step number, attempt number, URL, status, duration and redacted response data.

## UI examples

- `plans/ui/homepage-smoke.json` — navigation and page assertions.
- `plans/ui/login-flow.json` — SauceDemo login flow.
- `plans/ui/search-flow.json` — login and add-to-cart flow.
- `plans/ui/negative-login.json` — negative authentication validation.
- `suites/ui-smoke-suite.json` — UI examples executed as a suite.

Run a UI plan:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="plan examples/plans/ui/login-flow.json"
```

### Automatic UI screenshots

UI execution now captures a screenshot **after every successful UI step**. A failed step also gets a failure screenshot. Screenshots are organized as:

```text
reports/
└── screenshots/
    └── ui/
        ├── <test-name>/
        │   ├── 001-navigate.png
        │   ├── 002-fill.png
        │   ├── 003-click.png
        │   └── 004-assertvisible.png
        └── failures/
            └── <test-name>-step-4-failure.png
```

The explicit `screenshot` action is still accepted for backward compatibility, but automatic evidence capture means it is no longer required after each step.

## Terminal run logs

The command-line terminal output is preserved live **and** written to disk. API-oriented commands are stored under:

```text
reports/api/logs/terminal-<timestamp>-<command>.log
```

Interactive runs are stored under `reports/ui/logs/`, while unsupported/help commands use `reports/terminal/logs/`.

This gives each run two levels of evidence:

1. **Terminal log** — complete stdout/stderr from the application.
2. **API execution log** — request, response, assertion and error details for API tests.

Sensitive values are redacted before API execution details are persisted.

## Data-driven examples

Data-driven examples remain under `plans/data-driven/` and datasets under `data/json/`.

## Validation

Validate an example without executing it:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="validate plan examples/plans/api/get-user.json"
mvn -B --no-transfer-progress exec:java -Dexec.args="validate suite examples/suites/ui-smoke-suite.json"
```

Do not put real credentials, access tokens, cookies, production URLs or private customer data in examples.
