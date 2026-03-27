#!/usr/bin/env bash
# Publishes a single module to Maven via Gradle.
#
# Usage:
#   publish-module.sh <module> <mode> [snapshotModules]
#
#   module          — module name (e.g. "adjust", "core", "android")
#   mode            — "snapshot" or "release"
#   snapshotModules — comma-separated list of snapshot modules (snapshot mode only,
#                     e.g. "core,android,adjust")

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=./common.sh
source "${SCRIPT_DIR}/common.sh"

module="${1:-}"
mode="${2:-}"
snapshot_modules="${3:-}"

if [[ -z "$module" ]]; then
    echo "Error: module name is required as \$1" >&2
    exit 1
fi

if [[ -z "$mode" ]]; then
    echo "Error: mode is required as \$2 (\"snapshot\" or \"release\")" >&2
    exit 1
fi

if [[ "$mode" != "snapshot" && "$mode" != "release" ]]; then
    echo "Error: mode must be \"snapshot\" or \"release\", got \"${mode}\"" >&2
    exit 1
fi

gradle_path="$(module_to_gradle_path "$module")"

case "$mode" in
    snapshot)
        ./gradlew "${gradle_path}:publishToSonatype" -PsnapshotModules="${snapshot_modules}"
        ;;
    release)
        ./gradlew "${gradle_path}:publishToSonatype" -Prelease closeAndReleaseSonatypeStagingRepository
        ;;
    *)
        echo "Error: unexpected mode: ${mode}" >&2
        exit 1
        ;;
esac
