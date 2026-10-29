#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"

base_url="${KAVIYO_BASE_URL:-http://127.0.0.1:${KAVIYO_HTTP_PORT:-8082}}"
temp_dir="$(mktemp -d)"
trap 'rm -rf "$temp_dir"' EXIT

curl --fail --silent --show-error "$base_url/q/health" > "$temp_dir/health.json"
python3 - "$temp_dir/health.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    health = json.load(source)
if health.get("status") != "UP":
    raise SystemExit("Backend health is not UP.")
PY

curl --fail --silent --show-error "$base_url/api/v1/demo/audit" > "$temp_dir/audit.json"
python3 - "$temp_dir/audit.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    audit = json.load(source)
if audit.get("dataClassification") != "SYNTHETIC":
    raise SystemExit("Demo audit is missing its synthetic-data label.")
report = audit.get("report", {})
if report.get("productsChecked") != 3 or len(report.get("issues", [])) != 8:
    raise SystemExit("Demo audit returned an unexpected product or finding count.")
PY

curl --fail --silent --show-error \
  -H 'Content-Type: text/csv' \
  --data-binary @examples/laptop-catalog.csv \
  "$base_url/api/v1/catalog/imports/preview" > "$temp_dir/preview.json"
preview_hash="$(python3 - "$temp_dir/preview.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    preview = json.load(source)
if preview.get("readyRows") != 3 or preview.get("rowsNeedingReview") != 0:
    raise SystemExit("Sample CSV preview did not validate all three rows.")
print(preview["previewHash"])
PY
)"

curl --fail --silent --show-error \
  -H 'Content-Type: text/csv' \
  -H "X-Kaviyo-Confirm-Hash: $preview_hash" \
  --data-binary @examples/laptop-catalog.csv \
  "$base_url/api/v1/catalog/imports/confirm" > "$temp_dir/confirmation.json"

curl --fail --silent --show-error "$base_url/api/v1/catalog/products" > "$temp_dir/products.json"
python3 - "$temp_dir/products.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    catalog = json.load(source)
if catalog.get("dataClassification") != "SYNTHETIC_DEMO":
    raise SystemExit("Catalog response is missing its synthetic-data label.")
if len(catalog.get("products", [])) != 3:
    raise SystemExit("Confirmed catalog did not return all three products.")
PY

python3 - "$temp_dir/scan-request.json" <<'PY'
import json, sys
with open(sys.argv[1], "w", encoding="utf-8") as output:
    json.dump({"productIds": [
        "laptop-northstar-14",
        "laptop-cedar-air-13",
        "laptop-harbor-pro-16"
    ]}, output)
PY
curl --fail --silent --show-error \
  -H 'Content-Type: application/json' \
  --data-binary "@$temp_dir/scan-request.json" \
  "$base_url/api/v1/catalog/scans" > "$temp_dir/scan-create.json"
scan_run_id="$(python3 - "$temp_dir/scan-create.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    scan = json.load(source)
if scan.get("dataClassification") != "SYNTHETIC_DEMO":
    raise SystemExit("Scan response is missing its synthetic-data label.")
saved = scan.get("scan", {})
if saved.get("status") != "COMPLETED" or saved.get("observationType") != "KAVIYO_FETCH":
    raise SystemExit("Scan response does not contain a completed Kaviyo fetch run.")
print(saved["scanRunId"])
PY
)"

curl --fail --silent --show-error \
  "$base_url/api/v1/catalog/scans/$scan_run_id" > "$temp_dir/scan-detail.json"
python3 - "$temp_dir/scan-detail.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    scan = json.load(source)
saved = scan.get("scan", {})
if saved.get("status") != "COMPLETED":
    raise SystemExit("Saved scan run did not reload as completed.")
products = saved.get("products", [])
if len(products) != 3:
    raise SystemExit("Saved scan did not return all requested products.")
if any(item.get("page", {}).get("status") != "DESTINATION_BLOCKED" for item in products):
    raise SystemExit("Synthetic .invalid URLs were not safely blocked.")
if not saved.get("report", {}).get("issues"):
    raise SystemExit("Saved scan report did not retain its findings.")
PY

curl --fail --silent --show-error \
  "$base_url/api/v1/notifications?unreadOnly=true" > "$temp_dir/notifications.json"
python3 - "$temp_dir/notifications.json" "$scan_run_id" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    result = json.load(source)
notifications = result.get("notifications", [])
scan_id = sys.argv[2]
if not any(item.get("scanRunId") == scan_id and item.get("type") == "AUDIT_COMPLETED"
           for item in notifications):
    raise SystemExit("Completed scan notification was not persisted.")
PY

draft_http_status="$(curl --silent --show-error --output "$temp_dir/draft-unavailable.json" \
  --write-out '%{http_code}' \
  -H 'Content-Type: application/json' \
  --data '{"findingCode":"DESCRIPTION_THIN","shopperQuestion":"A laptop for photo editing with 32 GB of RAM and an OLED screen"}' \
  "$base_url/api/v1/catalog/scans/$scan_run_id/products/laptop-cedar-air-13/drafts")"
if [[ "$draft_http_status" != "503" ]]; then
  printf 'Expected the Meta draft route to report an unavailable provider without credentials; got HTTP %s.\n' "$draft_http_status" >&2
  exit 1
fi
python3 - "$temp_dir/draft-unavailable.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    result = json.load(source)
if result.get("code") != "PROVIDER_NOT_CONFIGURED":
    raise SystemExit("Missing Meta credentials were not reported as a provider-unavailable result.")
PY

printf 'Backend smoke checks passed: health, synthetic audit, CSV import, saved scans/findings/notifications, and graceful Meta-provider unavailability.\n'
