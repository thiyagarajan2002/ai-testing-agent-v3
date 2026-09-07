#!/usr/bin/env bash
set -euo pipefail

# CI smoke runner for every repository example.
# Requirement text files are validated as input fixtures; JSON examples are executed.

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
for file in examples/api-requirement.txt examples/ui-requirement.txt; do
  test -s "$file"
  echo "OK: $file"
done

echo "Validating all JSON examples..."
python3 - <<'PY'
import json
from pathlib import Path
for path in sorted(Path("examples").glob("*.json")):
    with path.open(encoding="utf-8") as fh:
        data = json.load(fh)
    if not isinstance(data, dict):
        raise SystemExit(f"JSON example must contain an object: {path}")
    print(f"OK: {path}")
PY

# v3 direct plan execution.
run_plan "v3 plan" "examples/v3-plan-file.json"

# v3 suite execution. The suite references the v3 plan and therefore exercises
# suite orchestration, reporting, history, comparison, and analytics as well.
echo "============================================================"
echo "EXAMPLE: v3 suite"
echo "FILE: examples/v3-suite.json"
echo "============================================================"
mvn -B --no-transfer-progress -DskipTests exec:java \
  -Dexec.mainClass=com.thiyagarajan.agent.Main \
  -Dexec.args="suite examples/v3-suite.json"

# v2 keeps API and UI examples in one envelope. Extract both plans and execute
# them through the current v3 runner so the legacy example remains continuously verified.
mkdir -p target/ci-examples
python3 - <<'PY'
import json
from pathlib import Path
source = Path("examples/v2-execution.json")
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
