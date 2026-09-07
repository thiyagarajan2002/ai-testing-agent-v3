# Examples

This directory contains runnable API, UI, suite and data-driven examples. Examples use public demonstration services and must never contain production credentials or private data.

## 1. API examples

| Example | Purpose |
|---|---|
| `plans/api/get-user.json` | GET request, query parameter and response assertion |
| `plans/api/create-resource.json` | POST request with JSON body |
| `plans/api/02-post.json` | POST request with headers and body |
| `plans/api/03-put.json` | PUT request with JSON body |
| `plans/api/04-delete.json` | DELETE request |
| `plans/api/05-query-headers.json` | Query parameters and custom headers |
| `plans/api/06-negative-status.json` | Expected 404 status validation |
| `plans/api/save-variable.json` | Response extraction and variable reuse |
| `plans/api/v3-plan-file.json` | Versioned API smoke/retry example |

Run an API example:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="plan examples/plans/api/get-user.json"
```

Validate first:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="validate plan examples/plans/api/02-post.json"
```

### API evidence

Each API execution should produce request/response details in:

```text
reports/api/logs/
├── <test-name>-<timestamp>.log
└── terminal-<timestamp>-plan.log
```

The durable API log includes timestamp, step, attempt, HTTP method, URL, status, duration, bounded response data and errors. Sensitive values are redacted.

## 2. UI examples

| Example | Purpose |
|---|---|
| `plans/ui/01-navigation.json` | Navigate and verify login control |
| `plans/ui/02-login.json` | Complete SauceDemo login workflow |
| `plans/ui/03-search-and-cart.json` | Login, product interaction and cart validation |
| `plans/ui/04-negative-login.json` | Invalid-login error validation |
| `plans/ui/homepage-smoke.json` | Existing homepage smoke example |
| `plans/ui/login-flow.json` | Existing detailed login flow |
| `plans/ui/search-flow.json` | Existing multi-step UI flow |
| `plans/ui/negative-login.json` | Existing negative UI flow |

Run a UI example:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="plan examples/plans/ui/02-login.json"
```

Install Chromium if required:

```bash
mvn -B -DskipTests compile exec:java@playwright-cli -Dexec.args="install chromium"
```

### Automatic UI screenshots

UI execution captures evidence after **every successful UI step** and also captures a screenshot when a step fails.

```text
reports/screenshots/ui/
└── <test-name>/
    ├── 001-navigate.png
    ├── 002-fill.png
    ├── 003-fill.png
    ├── 004-click.png
    └── 005-assertvisible.png
```

Failure evidence is stored with the test and step information. The screenshot path is attached to the step result and can be consumed by reports.

The explicit `screenshot` action remains supported for backward compatibility.

## 3. Suite examples

### API regression

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="suite examples/suites/api-regression-suite.json"
```

Contains GET, POST, PUT, DELETE, query/header and negative API scenarios.

### UI regression

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="suite examples/suites/ui-regression-suite.json"
```

Contains navigation, login, product/cart and negative-login scenarios.

### Full regression

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="suite examples/suites/full-regression-suite.json"
```

This demonstrates combining API and UI plans in one suite.

Existing `api-smoke-suite.json` and `ui-smoke-suite.json` remain available as smaller smoke suites.

## 4. Data-driven examples

Plan:

```text
plans/data-driven/users-api.json
```

Dataset:

```text
data/json/users.json
```

Run:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="data-driven examples/plans/data-driven/users-api.json examples/data/json/users.json --parallelism 3"
```

Filter one row:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="data-driven examples/plans/data-driven/users-api.json examples/data/json/users.json --filter userId=2"
```

## 5. Terminal logs

The application keeps output visible in the terminal while teeing stdout/stderr to a timestamped file.

```text
reports/
├── api/
│   └── logs/
│       ├── terminal-<timestamp>-plan.log
│       └── <api-test>-<timestamp>.log
├── ui/
│   └── logs/
│       └── terminal-<timestamp>-interactive.log
└── terminal/
    └── logs/
```

Terminal logs contain the command, application output/errors and final exit code.

## 6. Recommended learning order

1. `plans/api/get-user.json`
2. `plans/api/02-post.json`
3. `plans/api/05-query-headers.json`
4. `plans/api/save-variable.json`
5. `plans/api/06-negative-status.json`
6. `plans/ui/01-navigation.json`
7. `plans/ui/02-login.json`
8. `plans/ui/03-search-and-cart.json`
9. `plans/ui/04-negative-login.json`
10. `plans/data-driven/users-api.json`
11. `suites/api-regression-suite.json`
12. `suites/ui-regression-suite.json`
13. `suites/full-regression-suite.json`

## 7. Validation

Before execution, validate plans and suites:

```bash
mvn -B --no-transfer-progress exec:java -Dexec.args="validate plan examples/plans/api/get-user.json"
mvn -B --no-transfer-progress exec:java -Dexec.args="validate plan examples/plans/ui/02-login.json"
mvn -B --no-transfer-progress exec:java -Dexec.args="validate suite examples/suites/api-regression-suite.json"
mvn -B --no-transfer-progress exec:java -Dexec.args="validate suite examples/suites/ui-regression-suite.json"
```
