# Changelog

All notable project changes are documented here at release level. The complete implementation history and method-level details remain in `PROJECT_DETAILS.md`.

## 3.20.0

- Standardized repository organization for documentation, examples and engineering support files.
- Added architecture, configuration and testing documentation indexes.
- Established a documented target Java package architecture for the next controlled package migration.
- Preserved runtime behavior while separating repository organization from package migration.
- Fixed suite plan path validation so sibling directories such as `../plans` are valid within the suite workspace while traversal outside that workspace remains blocked.
- Fixed the suite security regression test to avoid writing to protected filesystem locations on CI runners.
- Added regression coverage for both valid sibling-directory plan references and invalid workspace escapes.
