# Contributing

## Development workflow

1. Create a focused branch for the change.
2. Keep behavior changes separate from structural refactors where possible.
3. Add or update tests for every behavior change.
4. Run `mvn clean verify` when dependencies are available.
5. Keep `README.md` and `PROJECT_DETAILS.md` synchronized with every release.
6. Do not commit credentials, generated reports or local IDE files.

## Commit style

Use concise imperative messages, for example:

```text
refactor: organize repository documentation
fix: validate data-driven filters
feat: add execution analytics
```

## Release rule

A version is not considered build-verified until Maven or GitHub Actions reports a successful verification run.
