# Changelog

All notable project changes are documented here at release level. The complete implementation history and method-level details remain in `PROJECT_DETAILS.md`.

## 3.25.0

- Hardened Playwright browser/context lifecycle with guaranteed cleanup.
- Added `DOMContentLoaded` navigation waiting instead of fixed post-navigation sleeps.
- Added UI actions for hover, check and uncheck while preserving existing actions.
- Added page title and URL assertions.
- Added locator state waits: `waitforvisible` and `waitforhidden`.
- Kept legacy timed `waitfor` support for backward compatibility.
- Added explicit locator validation for locator-dependent actions.
- Preserved per-step timeout overrides and default timeout configuration.
- Continued automatic full-page screenshots after successful UI steps and failure screenshots/metadata for failed steps.
- Applied security redaction to UI exception details stored in execution results.
- Updated README and complete project documentation.
- Bumped the application/Maven version to 3.25.0.

## 3.24.0

- Expanded API execution to GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS.
- Added explicit request `contentType` support and URL-encoded form parameters.
- Added bearer, preemptive basic, API-key-header and API-key-query authentication.
- Preserved variable substitution across request components and authentication values.
- Added XMLPath existence/equality assertions alongside JSONPath, header, body, status and response-time assertions.
- Preserved legacy assertions, response-variable extraction and retry behavior.
- Added local HTTP-server regression coverage for API authentication, JSON requests, form requests and assertions.

## 3.23.0

- Standardized the deterministic three-file report package: `report.html`, `report.csv`, `report.pdf`.
- Added `ReportIntegrityValidator` for exact file-set and artifact validation.
- Added regression coverage for complete, incomplete, unexpected-file and invalid-PDF packages.
- Standardized color-coded PDF reporting and unified report-source behavior.

## 3.22.0

- Hardened execution stability, configuration bounds, standardized errors and durable CLI logging.
- Added secure UTF-8 redaction to persisted stdout/stderr.
- Added regression coverage for configuration bounds, log redaction, Unicode preservation and execution-result serialization.
- Added CI shell syntax validation and corrected JSON fixture validation for array-based datasets.

## 3.21.0

- Expanded API/UI/suite/data-driven examples.
- Added automatic UI screenshots, durable API logs and terminal run logs.
- Unified execution reporting into HTML, CSV and PDF generated from the same `ExecutionResult`.

## 3.20.0

- Standardized repository organization and engineering support documentation.
- Established documented target package architecture.
- Fixed suite plan path validation and added security regression coverage.
