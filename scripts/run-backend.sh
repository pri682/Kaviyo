#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"

if [[ -f .env ]]; then
  set -a
  # .env is a local developer-owned file and is ignored by Git.
  source .env
  set +a
fi

# Local-only convenience. The backend binds to loopback by default; scanning is not ready
# for a public deployment until authentication, workspace authorization, and rate limits exist.
export KAVIYO_CRAWLER_ENABLED="${KAVIYO_CRAWLER_ENABLED:-true}"

exec ./mvnw quarkus:dev
