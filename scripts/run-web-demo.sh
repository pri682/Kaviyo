#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
build_dir="$(mktemp -d "${TMPDIR:-/tmp}/kaviyo-web-demo.XXXXXX")"
trap 'rm -rf "$build_dir"' EXIT

if [[ -f "$project_root/.env" ]]; then
  set -a
  # Local service endpoints and credentials are supplied by the developer, never committed.
  source "$project_root/.env"
  set +a
fi

port="${1:-8080}"
cd "$project_root"
mkdir -p "$build_dir/classes"
find src/main/java/com/kaviyo/app src/main/java/com/kaviyo/audit src/main/java/com/kaviyo/catalog src/main/java/com/kaviyo/importer \
  -name '*.java' ! -name 'CatalogPageAuditService.java' -print > "$build_dir/sources.list"
javac --release 17 --add-modules jdk.httpserver -d "$build_dir/classes" @"$build_dir/sources.list"
java --add-modules jdk.httpserver -cp "$build_dir/classes" com.kaviyo.app.KaviyoWebDemo "$port"
