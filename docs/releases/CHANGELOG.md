# Changelog

All notable project changes are documented here at release level. The complete implementation history and method-level details remain in `PROJECT_DETAILS.md`.

## 3.24.0

- Expanded API execution to GET, POST, PUT, PATCH, DELETE, HEAD and OPTIONS.
- Added explicit request `contentType` support.
- Added URL-encoded form parameter support through `TestStep.form`.
- Added step authentication model with bearer, preemptive basic, API-key-header and API-key-query modes.
- Kept authentication values out of the executor's explicit request log fields; existing persisted-log redaction remains active.
- Preserved variable substitution across paths, query parameters, headers, bodies, form fields and authentication values.
- Added XMLPath existence and equality assertions alongside the existing JSONPath, header, body, status and response-time assertions.
- Preserved legacy `assertSpec` compatibility.
- Preserved response JSONPath variable extraction and retry behavior.
- Added `ApiExecutorPhase3Test` coverage using a local HTTP server for bearer authentication, JSON request bodies, form requests and assertions.
- Updated README and complete project documentation with API request/auth/assertion examples.
- Bumped the application/Maven version to 3.24.0.

## 3.23.0

- Standardized the deterministic three-file report package: `report.html`, `report.csv`, `report.pdf`.
- Added `ReportIntegrityValidator` for exact file-set, non-empty artifact, HTML marker, CSV BOM/summary and PDF signature validation.
- Added regression coverage for complete, incomplete, unexpected-file and invalid-PDF packages.
- Standardized color-coded PDF reporting and unified report-source behavior.
- Updated README and project documentation for reporting integrity.

## 3.22.0

- Hardened Phase 1 execution stability before feature expansion.
- Added a hard `PARALLELISM` safety limit of 64 and validated CLI overrides against the same limit.
- Standardized runtime error categories through `AgentExecutionException` and rejected null categories.
- Fixed CLI error handling so execution exceptions are captured while durable terminal logging is still active.
- Deferred process termination until after the `RunLogManager` try-with-resources scope closes.
- Preserved full exception stack traces in durable terminal logs.
- Added secure redaction to persisted stdout/stderr and made terminal-log redaction UTF-8 safe.
- Added regression coverage for configuration bounds, log redaction, Unicode preservation and execution-result serialization.
- Added CI shell syntax validation and corrected JSON fixture validation for array-based datasets.

## 3.21.0

- Expanded `examples/` into a multi-scenario API, UI, suite and data-driven example catalog.
- Added API examples for POST, PUT, DELETE, query parameters, custom headers and negative status validation.
- Added UI examples and automatic screenshots for successful/failed UI steps.
- Added durable per-test API logs and live terminal-output capture.
- Unified execution reporting into HTML, CSV and PDF generated from the same `ExecutionResult`.
- Enhanced HTML, CSV and PDF reporting and kept runtime logs/screenshots as separate evidence.

## 3.20.0

- Standardized repository organization for documentation, examples and engineering support files.
- Added architecture, configuration and testing documentation indexes.
- Established documented target Java package architecture.
- Fixed suite plan path validation for valid sibling directories while blocking workspace escapes.
- Added regression coverage for valid sibling references and invalid workspace escapes.
