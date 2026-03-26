#!/usr/bin/env bash
# Reads affected modules by comparing current versions in code against
# the last tagged versions. No commit parsing needed — just a version diff.
#
# Output format: module|bump|oldVersion|newVersion (compatible with create-tags.sh etc.)

set -euo pipefail
set -f

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

chain=$(get_dependency_chain)

while IFS='|' read -r name _ _ _ _ _; do
    [[ "$name" == "name" ]] && continue

    tag_prefix=$(get_module_tag_prefix "$name")
    current_version=$(get_module_version "$name")

    # Find the latest tag for this module
    latest_tag=$(git tag -l "${tag_prefix}@v*" --sort=-v:refname | head -1)

    if [[ -z "$latest_tag" ]]; then
        # No tag exists — this module is new, treat as affected
        echo "${name}|new||${current_version}"
        continue
    fi

    # Extract version from tag: com.rudderstack.sdk.kotlin.core@v6.0.0 → 6.0.0
    tagged_version="${latest_tag##*@v}"

    if [[ "$tagged_version" != "$current_version" ]]; then
        echo "${name}|bumped|${tagged_version}|${current_version}"
    fi
done <<< "$chain"
