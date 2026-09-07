# AI Testing Agent — v3.20.0

AI-assisted API/UI testing framework using Java 21, Ollama, REST Assured, Playwright, environment profiles, assertions, retries, artifacts, reporting, suites, history/analytics, security redaction, preflight validation and data-driven execution.

## v3.20.0 — Repository Organization & Engineering Standards

This release standardizes the repository layout without changing runtime behavior. Documentation, examples and engineering support files are separated by purpose, and a target Java package architecture is documented for a later controlled package migration.

### Repository layout

```text
ai-testing-agent-v3/
├── .github/                 # CI/CD and repository automation
├── config/                  # Environment profiles and reusable test data
├── docs/                    # Architecture, configuration, testing and releases
├── examples/                # User-facing plans, suites, data and requirements
├── scripts/                 # Developer and CI helper scripts
├── src/main/java/           # Production Java source
├── src/main/resources/      # Runtime resources
├── src/test/java/           # Automated tests
├── src/test/resources/      # Test-only resources
├── reports/                 # Generated runtime output (not source)
├── pom.xml                  # Maven build definition
├── README.md                # Quick-start documentation
├── PROJECT_DETAILS.md       # Complete project documentation
├── CONTRIBUTING.md          # Development standards
├── SECURITY.md              # Security guidance
└── LICENSE.md
```

### Java architecture

The current Java packages remain behaviorally unchanged in v3.20.0. The target architecture is documented in `docs/architecture/package-structure.md` so the next package migration can update declarations, imports, tests and CI atomically.

### Commands

```bash
mvn clean verify
mvn exec:java -Dexec.args="plan <file>"
mvn exec:java -Dexec.args="suite <file>"
mvn exec:java -Dexec.args="data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]"
mvn exec:java -Dexec.args="validate plan <file>"
mvn exec:java -Dexec.args="validate suite <file>"
mvn exec:java -Dexec.args="interactive"
```

## v3.19.0 — Data-Driven Parallel Execution & Performance

v3.19 adds configurable parallel execution for data-driven rows while preserving dataset order in reports. It records measured iteration work, wall-clock duration and approximate speedup metrics. See `PROJECT_DETAILS.md` for complete details.

## Documentation map

- `PROJECT_DETAILS.md` — complete implementation and method reference.
- `docs/architecture/` — architecture and package boundaries.
- `docs/configuration/` — configuration guidance.
- `docs/testing/` — testing standards.
- `docs/releases/` — release history.
- `examples/` — runnable examples and sample inputs.
- `CONTRIBUTING.md` — development workflow.
- `SECURITY.md` — security guidance.

## Security

Do not store production credentials in plans or datasets. Sensitive execution diagnostics are redacted before reporting and AI failure analysis.

## Version history

- **v3.20.0** — repository organization and engineering standards
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
