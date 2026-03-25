#!/usr/bin/env bash
# Generates CHANGELOG.md entries for affected modules.
# Input: file path or stdin containing: module|bump|oldVersion|newVersion
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

# --- Configurable section mapping (order defines display order) ---
SECTION_ORDER="breaking feat fix refactor chore deps"

section_heading() {
    case "$1" in
        breaking) echo "## ⚠ Breaking Changes" ;;
        feat)     echo "## Features" ;;
        fix)      echo "## Bug Fixes" ;;
        refactor) echo "## Refactors" ;;
        chore)    echo "## Chores" ;;
        deps)     echo "## Dependency Updates" ;;
        *)        echo "## Other" ;;
    esac
}

sanitize_changelog_entry() {
    local text="$1"
    # Strip HTML tags
    text=$(echo "$text" | sed 's/<[^>]*>//g')
    # Strip markdown images (could be used for tracking pixels)
    text=$(echo "$text" | sed 's/!\[[^]]*\]([^)]*)//g')
    # Strip markdown link reference definitions
    text=$(echo "$text" | sed '/^\[.*\]:.*$/d')
    # Strip markdown links that don't point to the repo URL (replace [text](url) with just text)
    local escaped_url
    escaped_url=$(echo "$REPO_URL" | sed 's/[.\/]/\\&/g')
    text=$(echo "$text" | perl -pe "s/\[([^\]]+)\]\((?!${escaped_url})([^)]*)\)/\$1/g")
    echo "$text"
}

input="${1:--}"
if [[ "$input" == "-" ]]; then
    affected=$(cat)
else
    affected=$(cat "$input")
fi

[[ -z "$affected" ]] && exit 0

baseline=$(find_monorepo_tag)

commit_data=""
while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    commit_hash="${line%% *}"
    subject="${line#* }"

    files=$(git diff-tree --no-commit-id --name-only -r "$commit_hash")
    modules_for_commit=""
    while IFS= read -r filepath; do
        [[ -z "$filepath" ]] && continue
        mod=$(file_to_module "$filepath")
        [[ -z "$mod" ]] && continue
        # Avoid duplicate module entries per commit
        if ! echo "$modules_for_commit" | tr ',' '\n' | grep -qx "$mod"; then
            modules_for_commit="${modules_for_commit:+${modules_for_commit},}${mod}"
        fi
    done <<< "$files"

    [[ -z "$modules_for_commit" ]] && continue
    # Store: hash|subject|modules
    commit_data="${commit_data}${commit_hash}|${subject}|${modules_for_commit}
"
done < <(git log --no-merges --format="%H %s" "${baseline}..HEAD")

while IFS='|' read -r module bump old_version new_version; do
    [[ -z "$module" ]] && continue
    validate_module_name "$module"

    tag_prefix=$(get_module_tag_prefix "$module")
    today=$(date +%Y-%m-%d)

    # Determine module directory for CHANGELOG.md
    case "$module" in
        core|android) changelog_dir="$module" ;;
        *) changelog_dir="integrations/$module" ;;
    esac
    changelog_path="${changelog_dir}/CHANGELOG.md"

    # Collect commits for this module
    module_commits=""
    while IFS= read -r cline; do
        [[ -z "$cline" ]] && continue
        c_hash=$(echo "$cline" | cut -d'|' -f1)
        c_subject=$(echo "$cline" | cut -d'|' -f2)
        c_modules=$(echo "$cline" | cut -d'|' -f3)

        if echo "$c_modules" | tr ',' '\n' | grep -qx "$module"; then
            module_commits="${module_commits}${c_hash}|${c_subject}
"
        fi
    done <<< "$commit_data"

    # Build sections
    sections=""
    has_content=false

    if [[ -n "$module_commits" ]]; then
        for section_type in $SECTION_ORDER; do
            [[ "$section_type" == "deps" ]] && continue
            section_entries=""

            while IFS= read -r cline; do
                [[ -z "$cline" ]] && continue
                c_hash=$(echo "$cline" | cut -d'|' -f1)
                c_subject=$(echo "$cline" | cut -d'|' -f2)
                short_hash="${c_hash:0:7}"

                # Parse conventional commit
                breaking="false"
                if echo "$c_subject" | grep -qE '^[a-zA-Z]+(\(.+\))?!: '; then
                    breaking="true"
                fi

                type=$(echo "$c_subject" | sed -E 's/^([a-zA-Z]+)(\(.+\))?(!)?: .+/\1/')
                [[ "$type" == "$c_subject" ]] && continue

                scope=$(echo "$c_subject" | sed -E 's/^[a-zA-Z]+\(([^)]+)\)(!)?: .+/\1/')
                [[ "$scope" == "$c_subject" ]] && scope=""

                desc=$(echo "$c_subject" | sed -E 's/^[a-zA-Z]+(\([^)]+\))?(!)?: (.+)/\3/')
                # Sanitize desc before further processing
                desc=$(sanitize_changelog_entry "$desc")
                # Capitalise first letter
                desc="$(echo "${desc:0:1}" | tr '[:lower:]' '[:upper:]')${desc:1}"
                # Linkify PR references (#123)
                desc=$(echo "$desc" | sed -E "s|#([0-9]+)|[#\1](${REPO_URL}/pull/\1)|g")

                # Determine if this commit belongs in this section
                match=false
                if [[ "$section_type" == "breaking" && "$breaking" == "true" ]]; then
                    match=true
                elif [[ "$section_type" == "$type" ]]; then
                    match=true
                fi

                if [[ "$match" == "true" ]]; then
                    if [[ -n "$scope" ]]; then
                        section_entries="${section_entries}- **${scope}:** ${desc} ([${short_hash}](${REPO_URL}/commit/${c_hash}))
"
                    else
                        section_entries="${section_entries}- ${desc} ([${short_hash}](${REPO_URL}/commit/${c_hash}))
"
                    fi
                fi
            done <<< "$module_commits"

            if [[ -n "$section_entries" ]]; then
                heading=$(section_heading "$section_type")
                sections="${sections}
${heading}

${section_entries}"
                has_content=true
            fi
        done
    fi

    # Dependency updates — always check if upstream deps were bumped
    deps=$(get_module_deps "$module")
    dep_entries=""
    for dep in $deps; do
        dep_old=$(echo "$affected" | grep "^${dep}|" | cut -d'|' -f3 || true)
        dep_new=$(echo "$affected" | grep "^${dep}|" | cut -d'|' -f4 || true)
        if [[ -n "$dep_old" && -n "$dep_new" ]]; then
            dep_entries="${dep_entries}  - dependencies
    - @rudderstack/${dep} bumped from ${dep_old} to ${dep_new}
"
        fi
    done

    if [[ -n "$dep_entries" ]]; then
        heading=$(section_heading "deps")
        sections="${sections}
${heading}

- The following workspace dependencies were updated
${dep_entries}"
    fi

    # Build the full entry
    entry="# [${new_version}](${REPO_URL}/compare/${tag_prefix}@v${old_version}...${tag_prefix}@v${new_version}) (${today})
${sections}"

    # Ensure changelog exists with header
    if [[ ! -f "$changelog_path" ]]; then
        mkdir -p "$changelog_dir"
        printf "# Changelog\n\nAll notable changes to this project will be documented in this file.\n" > "$changelog_path"
    fi

    # Prepend entry after the 3-line header
    header=$(head -3 "$changelog_path")
    rest=$(tail -n +4 "$changelog_path")
    printf "%s\n\n%s\n%s\n" "$header" "$entry" "$rest" > "$changelog_path"

done <<< "$affected"
