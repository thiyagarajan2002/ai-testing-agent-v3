# Configuration Guide

Runtime configuration is kept outside Java source under `config/`.

- `config/environments/` — named environment profiles.
- `config/test-data/` — reusable non-secret test data.

Never commit production credentials, access tokens, private keys or client secrets. Use environment variables or an approved secret store for sensitive values.
