#!/usr/bin/env bash
# Creates lightweight tags for each bumped module and the monorepo.
# Tags point to the merge commit SHA (already verified/signed by GitHub),
# so all tags inherit the "Verified" badge on the tags page.
#
# Input: file path ($1) or stdin ("-") containing: module|bump|oldVersion|newVersion
#
# Requires: GH_TOKEN env var, GITHUB_REPOSITORY env var.

set -euo pipefail
set -f

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

# --- Input ---

input="${1:--}"
if [[ "$input" == "-" ]]; then
    affected=$(cat)
else
    affected=$(cat "$input")
fi

[[ -z "$affected" ]] && exit 0

REPO="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY must be set}"
# Use the merge commit SHA — already signed/verified by GitHub
MERGE_SHA=$(git rev-parse HEAD)

create_lightweight_tag() {
    local tag="$1"

    if gh api "repos/${REPO}/git/ref/tags/${tag}" >/dev/null 2>&1; then
        echo "Warning: tag ${tag} already exists on remote, skipping"
        return
    fi

    gh api "repos/${REPO}/git/refs" \
        --method POST \
        -f "ref=refs/tags/${tag}" \
        -f "sha=${MERGE_SHA}" > /dev/null

    echo "Tagged: ${tag}"
}

# --- Create per-module tags ---

while IFS='|' read -r module bump old_version new_version; do
    [[ -z "$module" ]] && continue

    tag_prefix=$(get_module_tag_prefix "$module")
    create_lightweight_tag "${tag_prefix}@v${new_version}"
done <<< "$affected"

# --- Create monorepo tag ---

sdk_version=$(grep '^SDK_VERSION=' "$GRADLE_PROPERTIES_PATH" | cut -d'=' -f2)
create_lightweight_tag "v${sdk_version}"
