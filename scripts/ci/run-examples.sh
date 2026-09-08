#!/usr/bin/env bash
set -euo pipefail

# CI smoke runner for the organized repository layout.
# Requirements are validated as fixtures; runnable JSON plans/suites are exercised explicitly.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

run_plan() {
  local label="$1"
  local file="$2"
  echo "============================================================"
  echo "EXAMPLE: ${label}"
  echo "FILE: ${file}"
  echo "============================================================"
  mvn -B --no-transfer-progress -DskipTests exec:java \
    -Dexec.mainClass=com.thiyagarajan.agent.Main \
    -Dexec.args="plan ${file}"
}

echo "Validating requirement fixtures..."
for file in examples/requirements/api-requirement.txt examples/requirements/ui-requirement.txt; do
  test -s "$file"
  echo "OK: $file"
done

echo "Validating all JSON examples recursively..."
python3 - <<'PY'
import json
from pathlib import Path

for path in sorted(Path("examples").rglob("*.json")):
    with path.open(encoding="utf-8") as fh:
        data = json.load(fh)

    # Data-driven fixtures may legitimately be either an object or an array.
    # Plans, suites, environments, and legacy envelopes must remain objects.
    if "data" in path.parts:
        if not isinstance(data, (dict, list)):
            raise SystemExit(
                f"Dataset JSON must contain an object or array: {path}"
            )
    elif not isinstance(data, dict):
        raise SystemExit(f"JSON example must contain an object: {path}")

    print(f"OK: {path}")

print("All JSON examples validated successfully.")
PY

# v3 direct plan.
run_plan "v3 plan" "examples/plans/api/v3-plan-file.json"

# v3 suite.
echo "============================================================"
echo "EXAMPLE: v3 suite"
echo "FILE: examples/suites/v3-suite.json"
echo "============================================================"
mvn -B --no-transfer-progress -DskipTests exec:java \
  -Dexec.mainClass=com.thiyagarajan.agent.Main \
  -Dexec.args="suite examples/suites/v3-suite.json"

# v2 legacy envelope: extract API/UI plans and execute through the current v3 runner.
mkdir -p target/ci-examples
python3 - <<'PY'
import json
from pathlib import Path
source = Path("examples/legacy/v2-execution.json")
data = json.loads(source.read_text(encoding="utf-8"))
for key in ("api", "ui"):
    value = data.get(key)
    if not isinstance(value, dict):
        raise SystemExit(f"Missing object '{key}' in {source}")
    out = Path("target/ci-examples") / f"v2-{key}.json"
    out.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    print(f"Prepared: {out}")
PY

run_plan "v2 API example" "target/ci-examples/v2-api.json"
run_plan "v2 UI example" "target/ci-examples/v2-ui.json"

echo "============================================================"
echo "ALL EXAMPLES PASSED"
echo "============================================================"
