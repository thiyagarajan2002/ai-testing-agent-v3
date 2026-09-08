# Changelog

All notable project changes are documented here at release level. The complete implementation history and method-level details remain in `PROJECT_DETAILS.md`.

## 3.21.0

- Expanded `examples/` into a multi-scenario API, UI, suite and data-driven example catalog.
- Added API examples for POST, PUT, DELETE, query parameters, custom headers and negative status validation.
- Added UI examples for navigation, login, product/cart interaction and negative login validation.
- Added dedicated API regression, UI regression and full regression suites.
- Added a runnable JSON data-driven users example.
- Added automatic full-page screenshot capture after every successful UI step.
- Added automatic failure screenshots for failed UI steps.
- Added durable per-test API logs containing request, response, assertion and error details.
- Added live terminal-output capture that preserves stdout/stderr and writes it to timestamped run logs.
- Added API-oriented terminal logs under `reports/api/logs/` and UI interactive logs under `reports/ui/logs/`.
- Added redaction before API execution details are persisted.
- Kept the explicit `screenshot` UI action backward compatible while making per-step evidence automatic.
- Unified execution reporting so each run produces exactly `report.html`, `report.csv` and `report.pdf` from the same `ExecutionResult`.
- Enhanced HTML reporting with KPI cards, search, PASS/FAIL and slow-step filters, sortable columns, expandable details, artifact links, theme switching and print support.
- Enhanced CSV reporting with UTF-8 BOM, CSV-safe quoting and a summary footer for status, counts, pass rate, duration statistics and failure analysis.
- Enhanced PDF reporting with the same execution summary, duration metrics, detailed step results, input/output text, artifacts and failure analysis.
- Removed `execution.log` generation from the `ReportManager` report bundle; runtime logs remain separate execution evidence.
- Added `ReportManagerTest` regression coverage to enforce the three-report bundle and core content.
- Updated `README.md`, `PROJECT_DETAILS.md` and example/evidence documentation.

## 3.20.0

- Standardized repository organization for documentation, examples and engineering support files.
- Added architecture, configuration and testing documentation indexes.
- Established a documented target Java package architecture for the next controlled package migration.
- Preserved runtime behavior while separating repository organization from package migration.
- Fixed suite plan path validation so sibling directories such as `../plans` are valid within the suite workspace while traversal outside that workspace remains blocked.
- Fixed the suite security regression test to avoid writing to protected filesystem locations on CI runners.
- Added regression coverage for both valid sibling-directory plan references and invalid workspace escapes.
