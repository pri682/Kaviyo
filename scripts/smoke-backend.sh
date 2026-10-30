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
if catalog.get("dataClassification") != "MERCHANT_SUPPLIED":
    raise SystemExit("Catalog response is missing its merchant-supplied data label.")
ids = {item.get("productId") for item in catalog.get("products", [])}
expected = {"laptop-northstar-14", "laptop-cedar-air-13", "laptop-harbor-pro-16"}
if not expected.issubset(ids):
    raise SystemExit("Confirmed catalog is missing sample products.")
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
if scan.get("status") != "QUEUED" or scan.get("observationType") != "KAVIYO_FETCH":
    raise SystemExit("Scan response does not confirm that the Kaviyo fetch was queued.")
print(scan["scanRunId"])
PY
)"

scan_status="QUEUED"
for attempt in $(seq 1 90); do
  curl --fail --silent --show-error \
    "$base_url/api/v1/catalog/scans/$scan_run_id" > "$temp_dir/scan-detail.json"
  scan_status="$(python3 - "$temp_dir/scan-detail.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    result = json.load(source)
print(result.get("scan", {}).get("status", "UNKNOWN"))
PY
  )"
  if [[ "$scan_status" == "COMPLETED" || "$scan_status" == "PARTIAL" ]]; then
    break
  fi
  if [[ "$scan_status" == "FAILED" ]]; then
    printf 'Queued scan %s failed.\n' "$scan_run_id" >&2
    exit 1
  fi
  sleep 1
done
if [[ "$scan_status" != "COMPLETED" && "$scan_status" != "PARTIAL" ]]; then
  printf 'Queued scan %s did not finish within 90 seconds (last status: %s).\n' \
    "$scan_run_id" "$scan_status" >&2
  exit 1
fi
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

resolution_url="$base_url/api/v1/catalog/scans/$scan_run_id/products/laptop-cedar-air-13/findings/PRICE_MISMATCH/resolution"
curl --fail --silent --show-error \
  -H 'Content-Type: application/json' \
  --data '{"resolution":"ACCEPT_CATALOG","note":"Smoke: page shows a stale sale price."}' \
  --request PUT \
  "$resolution_url" > "$temp_dir/resolution.json"
python3 - "$temp_dir/resolution.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    result = json.load(source)
if result.get("dataClassification") != "MERCHANT_SUPPLIED":
    raise SystemExit("Resolution response is missing its merchant-supplied data label.")
saved = result.get("resolution", {})
if saved.get("findingCode") != "PRICE_MISMATCH" or saved.get("resolution") != "ACCEPT_CATALOG":
    raise SystemExit("Saved resolution did not round-trip the guided decision.")
if not saved.get("decidedAt"):
    raise SystemExit("Saved resolution is missing its decision timestamp.")
PY
curl --fail --silent --show-error \
  -H 'Content-Type: application/json' \
  --data '{"resolution":"DISMISSED"}' \
  --request PUT \
  "$resolution_url" > "$temp_dir/resolution-updated.json"
curl --fail --silent --show-error \
  "$resolution_url" > "$temp_dir/resolution-get.json"
python3 - "$temp_dir/resolution-get.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    result = json.load(source)
if result.get("resolution", {}).get("resolution") != "DISMISSED":
    raise SystemExit("Re-recorded resolution did not update the saved decision.")
PY
resolution_404_status="$(curl --silent --show-error --output "$temp_dir/resolution-missing.json" \
  --write-out '%{http_code}' \
  "$base_url/api/v1/catalog/scans/$scan_run_id/products/laptop-cedar-air-13/findings/AVAILABILITY_MISMATCH/resolution")"
if [[ "$resolution_404_status" != "404" ]]; then
  printf 'Expected an undecided finding to report 404; got HTTP %s.\n' "$resolution_404_status" >&2
  exit 1
fi
resolution_400_status="$(curl --silent --show-error --output "$temp_dir/resolution-rejected.json" \
  --write-out '%{http_code}' \
  -H 'Content-Type: application/json' \
  --data '{"resolution":"ACCEPT_CATALOG"}' \
  --request PUT \
  "$base_url/api/v1/catalog/scans/$scan_run_id/products/laptop-cedar-air-13/findings/DESCRIPTION_THIN/resolution")"
if [[ "$resolution_400_status" != "400" ]]; then
  printf 'Expected a non-guided finding code to report 400; got HTTP %s.\n' "$resolution_400_status" >&2
  exit 1
fi

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

draft_http_status=""
for draft_attempt in 1 2; do
  draft_http_status="$(curl --silent --show-error --output "$temp_dir/draft.json" \
    --write-out '%{http_code}' \
    -H 'Content-Type: application/json' \
    --data '{"findingCode":"DESCRIPTION_THIN","shopperQuestion":"A laptop for photo editing with 32 GB of RAM and an OLED screen"}' \
    "$base_url/api/v1/catalog/scans/$scan_run_id/products/laptop-cedar-air-13/drafts")"
  if [[ "$draft_http_status" == "201" || "$draft_http_status" == "503" ]]; then
    break
  fi
  sleep 5
done
if [[ "$draft_http_status" == "503" ]]; then
  python3 - "$temp_dir/draft.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    result = json.load(source)
if result.get("code") != "PROVIDER_NOT_CONFIGURED":
    raise SystemExit("Missing Meta credentials were not reported as a provider-unavailable result.")
PY
  printf 'Backend smoke checks passed: health, synthetic audit, CSV import, saved scans/findings/notifications, and graceful Meta-provider unavailability.\n'
elif [[ "$draft_http_status" == "201" ]]; then
  python3 - "$temp_dir/draft.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    result = json.load(source)
if result.get("status") != "NEEDS_REVIEW":
    raise SystemExit("Live Meta draft was not returned for merchant review.")
if result.get("provider") != "META_MODEL_API" or not result.get("modelRunId"):
    raise SystemExit("Live Meta draft is missing its provider audit trace.")
draft = result.get("draft", {})
if not draft.get("id") or not draft.get("revision", {}).get("proposedValue"):
    raise SystemExit("Live Meta draft is missing its proposed copy.")
PY
  draft_id="$(python3 -c "import json; print(json.load(open('$temp_dir/draft.json'))['draft']['id'])")"
  decide_http_status="$(curl --silent --show-error --output "$temp_dir/decision.json" \
    --write-out '%{http_code}' \
    -H 'Content-Type: application/json' \
    --data '{"decision":"APPROVE","note":"Smoke review: proposal matches catalog facts."}' \
    "$base_url/api/v1/catalog/drafts/$draft_id/decision")"
  if [[ "$decide_http_status" != "200" ]]; then
    printf 'Expected approving the draft to report 200; got HTTP %s.\n' "$decide_http_status" >&2
    exit 1
  fi
  export_http_status="$(curl --silent --show-error --output "$temp_dir/export.json" \
    --write-out '%{http_code}' \
    -H 'Content-Type: application/json' \
    --request POST \
    "$base_url/api/v1/catalog/drafts/$draft_id/export")"
  if [[ "$export_http_status" != "200" ]]; then
    printf 'Expected exporting the approved draft to report 200; got HTTP %s.\n' "$export_http_status" >&2
    exit 1
  fi
  python3 - "$temp_dir/export.json" <<'PY'
import json, sys
with open(sys.argv[1], encoding="utf-8") as source:
    result = json.load(source)
if not result.get("packet", {}).get("exportId"):
    raise SystemExit("Approved draft export did not issue a change packet.")
PY
  printf 'Backend smoke checks passed: health, synthetic audit, CSV import, saved scans/findings/notifications, and a live Meta draft approved + exported.\n'
else
  printf 'Expected the Meta draft route to report 503 without credentials or 201 with credentials; got HTTP %s.\n' "$draft_http_status" >&2
  exit 1
fi
