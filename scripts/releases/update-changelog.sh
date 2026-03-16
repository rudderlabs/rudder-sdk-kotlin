#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

# ---------------------------------------------------------------------------
# max_bump - returns the higher of two bump levels
# Order: major > minor > patch > none
# ---------------------------------------------------------------------------
max_bump() {
    local a="$1"
    local b="$2"

    if [[ "$a" == "major" || "$b" == "major" ]]; then
        echo "major"
    elif [[ "$a" == "minor" || "$b" == "minor" ]]; then
        echo "minor"
    elif [[ "$a" == "patch" || "$b" == "patch" ]]; then
        echo "patch"
    else
        echo "none"
    fi
}

# ---------------------------------------------------------------------------
# clean_commit_subject - strips conventional-commit prefix and capitalises
# ---------------------------------------------------------------------------
clean_commit_subject() {
    local subject="$1"

    # Strip conventional commit prefix (e.g. "feat(core): ", "fix: ")
    local cleaned
    cleaned=$(echo "$subject" | sed -E 's/^[a-z]+(\([^)]*\))?[!]?:[[:space:]]*//')

    # Capitalise first letter
    cleaned="$(echo "${cleaned:0:1}" | tr '[:lower:]' '[:upper:]')${cleaned:1}"

    echo "$cleaned"
}

# ---------------------------------------------------------------------------
# linkify_pr - converts (#NNN) at the end of a subject into a markdown link
# ---------------------------------------------------------------------------
linkify_pr() {
    local text="$1"

    echo "$text" | sed -E "s|\(#([0-9]+)\)$|([#\1](${REPO_URL}/pull/\1))|"
}

# ---------------------------------------------------------------------------
# section_heading - maps a commit type to a changelog section heading
# ---------------------------------------------------------------------------
section_heading() {
    local commit_type="$1"

    case "$commit_type" in
        breaking) echo "## ⚠ Breaking Changes" ;;
        feat)     echo "## Features" ;;
        fix)      echo "## Bug Fixes" ;;
        refactor) echo "## Refactors" ;;
        chore)    echo "## Chores" ;;
        deps)     echo "## Dependency Updates" ;;
        *)        echo "" ;;
    esac
}

# ---------------------------------------------------------------------------
# generate_changelog_entry
#   Builds a changelog entry string for a module.
#   Arguments: module_path, old_version, new_version, commit_file
#   The commit_file contains one commit hash per line.
# ---------------------------------------------------------------------------
generate_changelog_entry() {
    local module_path="$1"
    local old_version="$2"
    local new_version="$3"
    local commit_file="$4"

    local tag_prefix
    tag_prefix=$(get_module_tag_prefix "$module_path")
    local old_tag="${tag_prefix}@${old_version}"
    local new_tag="${tag_prefix}@${new_version}"
    local today
    today=$(date +%Y-%m-%d)

    # Collect commits by section
    local -a breaking_items=()
    local -a feat_items=()
    local -a fix_items=()
    local -a refactor_items=()
    local -a chore_items=()

    while IFS= read -r hash; do
        [[ -z "$hash" ]] && continue
        local subject
        subject=$(git log --format="%s" -1 "$hash")
        local parsed
        parsed=$(parse_commit_type "$subject")
        local ctype="${parsed%% *}"
        local is_breaking="${parsed##* }"

        local cleaned
        cleaned=$(clean_commit_subject "$subject")
        local entry
        entry=$(linkify_pr "$cleaned")
        local line="- ${entry}"

        if [[ "$is_breaking" == "true" ]]; then
            breaking_items+=("$line")
        fi

        case "$ctype" in
            feat)     feat_items+=("$line") ;;
            fix)      fix_items+=("$line") ;;
            refactor) refactor_items+=("$line") ;;
            chore|ci|docs|style|perf|test|build) chore_items+=("$line") ;;
        esac
    done < "$commit_file"

    # Build the entry
    local entry_text=""
    entry_text+="# [${new_version}](${REPO_URL}/compare/${old_tag}...${new_tag}) (${today})"

    # Sections in order: breaking, features, bug fixes, refactors, chores
    if [[ ${#breaking_items[@]} -gt 0 ]]; then
        entry_text+=$'\n\n'"## ⚠ Breaking Changes"$'\n'
        for item in "${breaking_items[@]}"; do
            entry_text+=$'\n'"$item"
        done
    fi

    if [[ ${#feat_items[@]} -gt 0 ]]; then
        entry_text+=$'\n\n'"## Features"$'\n'
        for item in "${feat_items[@]}"; do
            entry_text+=$'\n'"$item"
        done
    fi

    if [[ ${#fix_items[@]} -gt 0 ]]; then
        entry_text+=$'\n\n'"## Bug Fixes"$'\n'
        for item in "${fix_items[@]}"; do
            entry_text+=$'\n'"$item"
        done
    fi

    if [[ ${#refactor_items[@]} -gt 0 ]]; then
        entry_text+=$'\n\n'"## Refactors"$'\n'
        for item in "${refactor_items[@]}"; do
            entry_text+=$'\n'"$item"
        done
    fi

    if [[ ${#chore_items[@]} -gt 0 ]]; then
        entry_text+=$'\n\n'"## Chores"$'\n'
        for item in "${chore_items[@]}"; do
            entry_text+=$'\n'"$item"
        done
    fi

    echo "$entry_text"
}

# ---------------------------------------------------------------------------
# add_dependency_update_section
#   Appends a "Dependency Updates" line to a changelog entry string.
# ---------------------------------------------------------------------------
add_dependency_update_section() {
    local entry_text="$1"
    local dep_name="$2"
    local dep_version="$3"

    entry_text+=$'\n\n'"## Dependency Updates"$'\n'
    entry_text+=$'\n'"- Update ${dep_name} dependency to ${dep_version}"

    echo "$entry_text"
}

# ---------------------------------------------------------------------------
# prepend_changelog_entry
#   Inserts a new entry into the module's CHANGELOG.md after the 3-line header.
# ---------------------------------------------------------------------------
prepend_changelog_entry() {
    local changelog_path="$1"
    local entry_text="$2"

    if [[ ! -f "$changelog_path" ]]; then
        # Create a new changelog if it doesn't exist
        {
            echo "# Changelog"
            echo ""
            echo "All notable changes to this project will be documented in this file."
            echo ""
            echo "$entry_text"
        } > "$changelog_path"
        return
    fi

    # Header is lines 1-3, rest is existing content after a blank line separator
    local header
    header=$(head -3 "$changelog_path")
    local rest
    rest=$(tail -n +4 "$changelog_path")

    {
        echo "$header"
        echo ""
        echo "$entry_text"
        echo "$rest"
    } > "$changelog_path"
}

# ---------------------------------------------------------------------------
# update_buildconfig_version
#   Updates VERSION_NAME (and optionally VERSION_CODE) in RudderStackBuildConfig.kt
#   using awk for block-scoped replacements.
# ---------------------------------------------------------------------------
update_buildconfig_version() {
    local module_path="$1"
    local new_version="$2"
    local new_version_code="${3:-}"

    local config_file="${REPO_ROOT}/${BUILD_CONFIG_PATH}"
    local tmp_file
    tmp_file=$(mktemp)

    if [[ "$module_path" == "core" ]]; then
        awk -v new_ver="$new_version" '
        /object Core \{/ { in_block=1 }
        in_block && /VERSION_NAME/ {
            sub(/VERSION_NAME = "[^"]*"/, "VERSION_NAME = \"" new_ver "\"")
            in_block=0
        }
        { print }
        ' "$config_file" > "$tmp_file"
        mv "$tmp_file" "$config_file"

    elif [[ "$module_path" == "android" ]]; then
        awk -v new_ver="$new_version" -v new_code="$new_version_code" '
        /object Android \{/ { in_block=1 }
        in_block && /VERSION_NAME/ {
            sub(/VERSION_NAME = "[^"]*"/, "VERSION_NAME = \"" new_ver "\"")
        }
        in_block && /VERSION_CODE/ {
            sub(/VERSION_CODE = "[^"]*"/, "VERSION_CODE = \"" new_code "\"")
            in_block=0
        }
        { print }
        ' "$config_file" > "$tmp_file"
        mv "$tmp_file" "$config_file"

    elif [[ "$module_path" == integrations/* ]]; then
        local integration_name="${module_path#integrations/}"
        awk -v mod_name="$integration_name" -v new_ver="$new_version" -v new_code="$new_version_code" '
        /moduleName.*=.*"/ && $0 ~ "\"" mod_name "\"" { in_block=1 }
        in_block && /versionName/ {
            sub(/versionName.*=.*"[^"]*"/, "versionName: String = \"" new_ver "\"")
        }
        in_block && /versionCode/ {
            sub(/versionCode.*=.*"[^"]*"/, "versionCode: String = \"" new_code "\"")
            in_block=0
        }
        { print }
        ' "$config_file" > "$tmp_file"
        mv "$tmp_file" "$config_file"
    fi
}

# ---------------------------------------------------------------------------
# process_module
#   Processes a single module: computes bump, generates changelog, updates files.
#   Arguments: module_path, commit_map_dir, upstream_bump, upstream_name, upstream_version
#   Outputs: <bump_level>:<new_version> on stdout (for cascade propagation)
# ---------------------------------------------------------------------------
process_module() {
    local module_path="$1"
    local commit_map_dir="$2"
    local upstream_bump="${3:-none}"
    local upstream_name="${4:-}"
    local upstream_version="${5:-}"

    local safe_name="${module_path//\//_}"
    local commit_file="${commit_map_dir}/${safe_name}"

    # Collect commit types for direct bump computation
    local type_lines=""
    local has_commits=false

    if [[ -f "$commit_file" ]]; then
        while IFS= read -r hash; do
            [[ -z "$hash" ]] && continue
            has_commits=true
            local subject
            subject=$(git log --format="%s" -1 "$hash")
            local parsed
            parsed=$(parse_commit_type "$subject")
            type_lines+="${parsed}"$'\n'
        done < "$commit_file"
    fi

    # Compute direct bump from commits
    local direct_bump="none"
    if [[ "$has_commits" == "true" ]]; then
        direct_bump=$(echo "$type_lines" | compute_version_bump)
    fi

    # Compute cascade bump from upstream
    local cascade_bump="none"
    if [[ "$upstream_bump" != "none" ]]; then
        cascade_bump=$(compute_cascade_bump "$upstream_bump")
    fi

    # Final bump is the maximum of direct and cascade
    local final_bump
    final_bump=$(max_bump "$direct_bump" "$cascade_bump")

    if [[ "$final_bump" == "none" ]]; then
        echo "none:"
        return
    fi

    # Get current version and compute new version
    local current_version
    current_version=$(get_module_version "$module_path")
    local new_version
    new_version=$(bump_version "$current_version" "$final_bump")

    # Generate changelog entry from commits
    local entry_text=""
    if [[ "$has_commits" == "true" ]]; then
        entry_text=$(generate_changelog_entry "$module_path" "$current_version" "$new_version" "$commit_file")
    else
        # No direct commits but cascade bump - create a minimal entry
        local tag_prefix
        tag_prefix=$(get_module_tag_prefix "$module_path")
        local old_tag="${tag_prefix}@${current_version}"
        local new_tag="${tag_prefix}@${new_version}"
        local today
        today=$(date +%Y-%m-%d)
        entry_text="# [${new_version}](${REPO_URL}/compare/${old_tag}...${new_tag}) (${today})"
    fi

    # Add dependency update section if cascaded from upstream
    if [[ -n "$upstream_name" && "$upstream_bump" != "none" ]]; then
        entry_text=$(add_dependency_update_section "$entry_text" "$upstream_name" "$upstream_version")
    fi

    echo "  [$module_path] Updating version: $current_version -> $new_version ($final_bump)" >&2

    # Update RudderStackBuildConfig.kt
    if [[ "$module_path" == "core" ]]; then
        update_buildconfig_version "$module_path" "$new_version"
    else
        local current_code
        current_code=$(get_module_version_code "$module_path")
        local new_code=$((current_code + 1))
        update_buildconfig_version "$module_path" "$new_version" "$new_code"
        echo "  [$module_path] Updating version code: $current_code -> $new_code" >&2
    fi

    # Prepend to CHANGELOG.md
    local changelog_path="${REPO_ROOT}/${module_path}/CHANGELOG.md"
    prepend_changelog_entry "$changelog_path" "$entry_text"
    echo "  [$module_path] Updated changelog: $changelog_path" >&2

    # Dry-run tag
    local dry_run_tag_prefix
    dry_run_tag_prefix=$(get_module_tag_prefix "$module_path")
    echo "  [$module_path] [DRY RUN] Would create tag: ${dry_run_tag_prefix}@${new_version}" >&2

    # Return bump and new version for downstream cascade
    echo "${final_bump}:${new_version}"
}

# ===========================================================================
# Main
# ===========================================================================

# Step 1: Find the monorepo base tag
echo "Step 1: Finding monorepo base tag..."
BASE_TAG=$(find_monorepo_tag)
echo "  Base tag: ${BASE_TAG}"

# Step 2: Find affected modules
echo ""
echo "Step 2: Finding affected modules..."
AFFECTED_MODULES=$(find_affected_modules "$BASE_TAG")

if [[ -z "$AFFECTED_MODULES" ]]; then
    echo "  No affected modules found. Nothing to do."
    exit 0
fi

echo "  Affected modules: ${AFFECTED_MODULES}"

# Step 3: Map commits to modules
echo ""
echo "Step 3: Mapping commits to modules..."
COMMIT_MAP_DIR=$(map_commits_to_modules "$BASE_TAG")
echo "  Commit map created"

# Step 4: Process modules in dependency order
echo ""
echo "Step 4: Processing modules..."

# Arrays to track results for the summary table
declare -a summary_modules=()
declare -a summary_current=()
declare -a summary_new=()
declare -a summary_bump=()

core_bump="none"
core_new_version=""
android_bump="none"
android_new_version=""

echo ""
echo "Processing core..."
if echo "$AFFECTED_MODULES" | grep -q "^core$\|^core \| core$\| core "; then
    core_current=$(get_module_version "core")
    result=$(process_module "core" "$COMMIT_MAP_DIR")
    core_bump="${result%%:*}"
    core_new_version="${result##*:}"

    if [[ "$core_bump" != "none" ]]; then
        summary_modules+=("core")
        summary_current+=("$core_current")
        summary_new+=("$core_new_version")
        summary_bump+=("$core_bump")
    fi
fi

echo ""
echo "Processing android..."
if echo "$AFFECTED_MODULES" | grep -q "android"; then
    android_current=$(get_module_version "android")
    result=$(process_module "android" "$COMMIT_MAP_DIR" "$core_bump" "core" "$core_new_version")
    android_bump="${result%%:*}"
    android_new_version="${result##*:}"

    if [[ "$android_bump" != "none" ]]; then
        summary_modules+=("android")
        summary_current+=("$android_current")
        summary_new+=("$android_new_version")
        summary_bump+=("$android_bump")
    fi
fi

echo ""
echo "Processing integrations..."
# Determine the effective upstream bump for integrations: max of core and android
integration_upstream_bump=$(max_bump "$core_bump" "$android_bump")

# Integrations depend on android (not core directly): core → android → integrations
integration_upstream_name=""
integration_upstream_version=""
if [[ "$android_bump" != "none" ]]; then
    integration_upstream_name="android"
    integration_upstream_version="$android_new_version"
fi

for module in $AFFECTED_MODULES; do
    if [[ "$module" == integrations/* ]]; then
        mod_current=$(get_module_version "$module")
        result=$(process_module "$module" "$COMMIT_MAP_DIR" "$integration_upstream_bump" "$integration_upstream_name" "$integration_upstream_version")
        mod_bump="${result%%:*}"
        mod_new_version="${result##*:}"

        if [[ "$mod_bump" != "none" ]]; then
            summary_modules+=("$module")
            summary_current+=("$mod_current")
            summary_new+=("$mod_new_version")
            summary_bump+=("$mod_bump")
        fi
    fi
done

echo ""
echo "Step 5: Computing monorepo version..."
monorepo_bump="none"
if [[ "$core_bump" == "major" || "$android_bump" == "major" ]]; then
    monorepo_bump="major"
elif [[ ${#summary_modules[@]} -gt 0 ]]; then
    monorepo_bump="minor"
fi

if [[ "$monorepo_bump" != "none" ]]; then
    # Get current monorepo version from gradle.properties
    current_monorepo_version=$(grep '^SDK_VERSION=' "${REPO_ROOT}/${GRADLE_PROPERTIES_PATH}" | cut -d'=' -f2)
    new_monorepo_version=$(bump_version "$current_monorepo_version" "$monorepo_bump")

    # Update gradle.properties
    echo "  Updating monorepo version: $current_monorepo_version -> $new_monorepo_version ($monorepo_bump)"
    sed_inplace "s/^SDK_VERSION=.*/SDK_VERSION=${new_monorepo_version}/" "${REPO_ROOT}/${GRADLE_PROPERTIES_PATH}"
    echo "  Updated gradle.properties"

    echo "  [DRY RUN] Would create tag: v${new_monorepo_version}"
fi

echo ""
echo "Step 6: Summary"
echo ""
echo "============================================"
echo "  Release Summary"
echo "============================================"
printf "%-24s%-18s%s\n" "Module" "Current → New" "Bump"
echo "--------------------------------------------"

for i in "${!summary_modules[@]}"; do
    printf "%-24s%-18s%s\n" "${summary_modules[$i]}" "${summary_current[$i]} → ${summary_new[$i]}" "${summary_bump[$i]}"
done

echo "--------------------------------------------"
if [[ "$monorepo_bump" != "none" ]]; then
    printf "%-24s%-18s%s\n" "Monorepo (SDK_VERSION)" "${current_monorepo_version} → ${new_monorepo_version}" "${monorepo_bump}"
fi
echo "============================================"

# Step 7: Cleanup
if [[ -d "$COMMIT_MAP_DIR" ]]; then
    rm -rf "$COMMIT_MAP_DIR"
fi
