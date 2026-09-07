# Changelog

All notable project changes are documented here at release level. The complete implementation history and method-level details remain in `PROJECT_DETAILS.md`.

## 3.21.0

- Added multiple API example plans for GET, POST and response-variable chaining.
- Added multiple UI example plans for navigation, login, product flow and negative login validation.
- Added dedicated API and UI example suites.
- Added automatic full-page screenshot capture after every successful UI step.
- Added automatic failure screenshots for failed UI steps.
- Added durable per-test API logs containing request, response, assertion and error details.
- Added live terminal-output capture that preserves stdout/stderr and writes it to timestamped run logs.
- Added API-oriented terminal logs under `reports/api/logs/` and UI interactive logs under `reports/ui/logs/`.
- Added redaction before API execution details are persisted.
- Kept the explicit `screenshot` UI action backward compatible while making per-step evidence automatic.

## 3.20.0

- Standardized repository organization for documentation, examples and engineering support files.
- Added architecture, configuration and testing documentation indexes.
- Established a documented target Java package architecture for the next controlled package migration.
- Preserved runtime behavior while separating repository organization from package migration.
- Fixed suite plan path validation so sibling directories such as `../plans` are valid within the suite workspace while traversal outside that workspace remains blocked.
- Fixed the suite security regression test to avoid writing to protected filesystem locations on CI runners.
- Added regression coverage for both valid sibling-directory plan references and invalid workspace escapes.
