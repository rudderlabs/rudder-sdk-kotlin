#!/usr/bin/env bash
# Creates GitHub releases for each bumped module.
#
# Input: file path or stdin containing: module|bump|oldVersion|newVersion

set -euo pipefail

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

# --- Helpers ---

changelog_path_for_module() {
    local module="$1"
    case "$module" in
        core|android) echo "${module}/CHANGELOG.md" ;;
        *) echo "integrations/${module}/CHANGELOG.md" ;;
    esac
}

extract_latest_changelog_entry() {
    local changelog="$1"
    [[ ! -f "$changelog" ]] && return

    # Extract content between first '# [' heading and the second '# [' heading
    # (or end of file if only one entry exists)
    awk '
        /^# \[/ {
            if (found) { exit }
            found = 1
            print
            next
        }
        found { print }
    ' "$changelog"
}

# --- Create releases ---

while IFS='|' read -r module bump old_version new_version; do
    [[ -z "$module" ]] && continue

    tag_prefix=$(get_module_tag_prefix "$module")
    tag="${tag_prefix}@v${new_version}"
    changelog=$(changelog_path_for_module "$module")
    changelog_body=$(extract_latest_changelog_entry "$changelog")

    # Only mark Android as "Latest" — integrations and core don't take the badge
    latest_flag="--latest=false"
    if [[ "$module" == "android" ]]; then
        latest_flag="--latest"
    fi

    if gh release view "$tag" >/dev/null 2>&1; then
        echo "Release $tag already exists, skipping"
        continue
    fi

    echo "Creating GitHub release: $tag"
    gh release create "$tag" \
        --title "$tag" \
        --notes "${changelog_body:-No changelog available.}" \
        "$latest_flag"

done <<< "$affected"
