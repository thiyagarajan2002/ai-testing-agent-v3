# Package Architecture

## Current production package

The project currently uses feature-oriented top-level packages under `com.thiyagarajan.agent`: `ai`, `config`, `model`, `report`, and `runtime`.

## Target organization

The long-term production layout is:

```text
com.thiyagarajan.agent
├── cli
├── ai
├── config
├── model
├── execution
│   ├── api
│   ├── ui
│   ├── data
│   └── suite
├── validation
├── reporting
├── analytics
├── security
├── exception
├── io
└── util
```

### Package responsibilities

| Package | Responsibility |
|---|---|
| `cli` | Command-line entry points and argument handling |
| `ai` | Ollama clients and prompt construction |
| `config` | Application and environment configuration |
| `model` | Input/output domain models |
| `execution` | Test execution orchestration and lifecycle |
| `execution.api` | REST Assured execution and API assertions |
| `execution.ui` | Playwright execution |
| `execution.data` | Dataset loading and data-driven execution |
| `execution.suite` | Suite execution and aggregation |
| `validation` | Plan/suite preflight validation |
| `reporting` | HTML/JSON/CSV/PDF/log reporting |
| `analytics` | History, comparison and flaky-test analytics |
| `security` | Sensitive-data redaction and safety controls |
| `exception` | Application exception types |
| `io` | File and serialization helpers |
| `util` | Small stateless cross-cutting utilities |

## Refactoring rule

Package migration must be atomic: update the Java `package` declaration, all imports, tests, documentation references and CI in the same change. Do not leave duplicate production implementations during migration.

The v3.20 repository organization release therefore standardizes repository-level structure first; behavioral package migration is kept as a separate controlled refactor so the release can be validated without changing execution semantics.
