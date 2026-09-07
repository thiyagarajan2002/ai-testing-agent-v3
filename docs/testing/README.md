# Testing Standards

The automated test suite mirrors the production package layout where practical.

## Principles

1. Unit tests must be deterministic and isolated.
2. Input validation is tested before execution behavior.
3. Data-driven tests cover valid, invalid and filtering scenarios.
4. Security redaction is tested explicitly.
5. CI is the authoritative build verification when local dependency access is unavailable.

Test resources belong under `src/test/resources/`; executable examples intended for users belong under `examples/`.
