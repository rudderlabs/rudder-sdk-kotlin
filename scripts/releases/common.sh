#!/usr/bin/env bash
set -euo pipefail

REPO_URL="https://github.com/rudderlabs/rudder-sdk-kotlin"
BUILD_CONFIG_PATH="buildSrc/src/main/kotlin/RudderStackBuildConfig.kt"
GRADLE_PROPERTIES_PATH="gradle.properties"

# Portable sed -i wrapper
sed_inplace() {
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
}

# Scan for modules with build.gradle.kts under repo root.
# Include: core, android, integrations/*
# Exclude: app, kotlin-jvm-app, buildSrc
discover_modules() {
    local modules=()
    local repo_root
    repo_root="$(git rev-parse --show-toplevel)"

    for dir in "$repo_root"/*/; do
        local name
        name="$(basename "$dir")"
        # Skip excluded directories
        case "$name" in
            app|kotlin-jvm-app|buildSrc) continue ;;
        esac
        if [[ -f "$dir/build.gradle.kts" ]]; then
            if [[ "$name" == "core" || "$name" == "android" ]]; then
                modules+=("$name")
            fi
        fi
    done

    # Scan integrations subdirectories
    if [[ -d "$repo_root/integrations" ]]; then
        for dir in "$repo_root"/integrations/*/; do
            if [[ -f "$dir/build.gradle.kts" ]]; then
                local name
                name="$(basename "$dir")"
                modules+=("integrations/$name")
            fi
        done
    fi

    # Output in dependency order: core first, android second, then integrations sorted
    local result=()
    local has_core=false
    local has_android=false
    local integration_modules=()

    for m in "${modules[@]}"; do
        case "$m" in
            core)           has_core=true ;;
            android)        has_android=true ;;
            integrations/*) integration_modules+=("$m") ;;
        esac
    done

    if [[ "$has_core" == true ]]; then
        result+=("core")
    fi
    if [[ "$has_android" == true ]]; then
        result+=("android")
    fi
    if [[ ${#integration_modules[@]} -gt 0 ]]; then
        IFS=$'\n' read -r -d '' -a integration_modules < <(printf '%s\n' "${integration_modules[@]}" | sort && printf '\0') || true
        result+=("${integration_modules[@]}")
    fi

    if [[ ${#result[@]} -gt 0 ]]; then echo "${result[*]}"; fi
}

# Find the latest monorepo version tag (v*.*.* without pre-release suffix)
find_monorepo_tag() {
    local tag
    tag="$(git tag -l 'v*' --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -1)"
    if [[ -z "$tag" ]]; then
        echo "Error: no monorepo version tag found" >&2
        return 1
    fi
    echo "$tag"
}

# Find a merge-base for snapshot comparison
find_snapshot_base() {
    local base
    base="$(git merge-base HEAD origin/develop 2>/dev/null)" || \
    base="$(git merge-base HEAD origin/main)"
    echo "$base"
}

# Map a file path to its module, or return empty if not a module file
_file_to_module() {
    local file="$1"
    case "$file" in
        core/*)             echo "core" ;;
        android/*)          echo "android" ;;
        integrations/*/*)
            local integration_name
            integration_name="$(echo "$file" | cut -d'/' -f2)"
            echo "integrations/$integration_name"
            ;;
        *)                  echo "" ;;
    esac
}

# Check if a module is already in a newline-separated list
_list_contains() {
    local list="$1"
    local item="$2"
    echo "$list" | grep -qx "$item"
}

# Add a module to a newline-separated list (deduplicating)
_list_add() {
    local list="$1"
    local item="$2"
    if [[ -z "$list" ]]; then
        echo "$item"
    elif ! _list_contains "$list" "$item"; then
        printf '%s\n%s' "$list" "$item"
    else
        echo "$list"
    fi
}

# Find modules affected by changes since base_ref.
# Maps files to modules by path, then cascades along the dependency chain:
# core -> android -> integrations
find_affected_modules() {
    local base_ref="$1"
    local changed_files
    changed_files="$(git diff --name-only "$base_ref"...HEAD)"

    local affected=""

    while IFS= read -r file; do
        [[ -z "$file" ]] && continue
        local mod
        mod="$(_file_to_module "$file")"
        if [[ -n "$mod" ]]; then
            affected="$(_list_add "$affected" "$mod")"
        fi
    done <<< "$changed_files"

    # Cascade: core changes affect android and all integrations
    if _list_contains "$affected" "core"; then
        affected="$(_list_add "$affected" "android")"
        local all_modules
        all_modules="$(discover_modules)"
        for m in $all_modules; do
            if [[ "$m" == integrations/* ]]; then
                affected="$(_list_add "$affected" "$m")"
            fi
        done
    fi

    # Cascade: android changes affect all integrations
    if _list_contains "$affected" "android"; then
        local all_modules
        all_modules="$(discover_modules)"
        for m in $all_modules; do
            if [[ "$m" == integrations/* ]]; then
                affected="$(_list_add "$affected" "$m")"
            fi
        done
    fi

    # Output in dependency order: core first, android second, integrations sorted
    local result=()
    if _list_contains "$affected" "core"; then
        result+=("core")
    fi
    if _list_contains "$affected" "android"; then
        result+=("android")
    fi
    local integration_modules=()
    while IFS= read -r m; do
        [[ -z "$m" ]] && continue
        if [[ "$m" == integrations/* ]]; then
            integration_modules+=("$m")
        fi
    done <<< "$affected"
    if [[ ${#integration_modules[@]} -gt 0 ]]; then
        IFS=$'\n' read -r -d '' -a integration_modules < <(printf '%s\n' "${integration_modules[@]}" | sort && printf '\0') || true
        result+=("${integration_modules[@]}")
    fi

    if [[ ${#result[@]} -gt 0 ]]; then echo "${result[*]}"; fi
}

# Map each non-merge commit to its affected modules.
# Writes temp files (one per module) containing commit hashes.
# Prints the temp directory path.
map_commits_to_modules() {
    local base_ref="$1"
    local tmp_dir
    tmp_dir="$(mktemp -d)"

    local commits
    commits="$(git log --no-merges --format="%H" "$base_ref"...HEAD)"

    while IFS= read -r commit; do
        [[ -z "$commit" ]] && continue
        local files
        files="$(git diff-tree --no-commit-id --name-only -r "$commit")"

        local commit_mods=""

        while IFS= read -r file; do
            [[ -z "$file" ]] && continue
            local mod
            mod="$(_file_to_module "$file")"
            if [[ -n "$mod" ]]; then
                commit_mods="$(_list_add "$commit_mods" "$mod")"
            fi
        done <<< "$files"

        while IFS= read -r mod; do
            [[ -z "$mod" ]] && continue
            local safe_name
            safe_name="${mod//\//_}"
            echo "$commit" >> "$tmp_dir/$safe_name"
        done <<< "$commit_mods"
    done <<< "$commits"

    echo "$tmp_dir"
}

# Parse a conventional commit subject into type and breaking flag.
parse_commit_type() {
    local subject="$1"

    # Check for conventional commit pattern: type(scope)!: or type!: or type(scope): or type:
    local pattern='^([a-zA-Z]+)(\([^)]*\))?(!)?: '
    if [[ "$subject" =~ $pattern ]]; then
        local type="${BASH_REMATCH[1]}"
        local breaking="${BASH_REMATCH[3]}"

        case "$type" in
            feat)
                if [[ "$breaking" == "!" ]]; then
                    echo "feat true"
                else
                    echo "feat false"
                fi
                ;;
            fix|refactor|chore)
                if [[ "$breaking" == "!" ]]; then
                    echo "Breaking changes only allowed on feat commits" >&2
                    return 1
                fi
                echo "$type false"
                ;;
            docs)
                echo "SKIP false"
                ;;
            *)
                echo "SKIP false"
                echo "Warning: non-conventional commit subject: $subject" >&2
                ;;
        esac
    else
        echo "SKIP false"
        echo "Warning: non-conventional commit subject: $subject" >&2
    fi
}

# Read type/breaking pairs from stdin, determine highest bump level.
# major (breaking) > minor (feat) > patch (fix/refactor/chore) > none (all SKIP)
compute_version_bump() {
    local has_major=false
    local has_minor=false
    local has_patch=false

    while IFS=' ' read -r type breaking; do
        [[ -z "$type" ]] && continue
        if [[ "$breaking" == "true" ]]; then
            has_major=true
            continue
        fi
        case "$type" in
            feat)      has_minor=true ;;
            fix)       has_patch=true ;;
            refactor)  has_patch=true ;;
            chore)     has_patch=true ;;
            SKIP)      ;;
        esac
    done

    if [[ "$has_major" == true ]]; then
        echo "major"
    elif [[ "$has_minor" == true ]]; then
        echo "minor"
    elif [[ "$has_patch" == true ]]; then
        echo "patch"
    else
        echo "none"
    fi
}

# Determine cascade bump from an upstream module's bump level.
# major upstream → major cascade, anything else → patch
compute_cascade_bump() {
    local upstream_bump="$1"
    if [[ "$upstream_bump" == "major" ]]; then
        echo "major"
    else
        echo "patch"
    fi
}

# Read a module's VERSION_NAME / versionName from RudderStackBuildConfig.kt
get_module_version() {
    local module="$1"
    local repo_root
    repo_root="$(git rev-parse --show-toplevel)"
    local config_file="$repo_root/$BUILD_CONFIG_PATH"

    case "$module" in
        core)
            grep -A 5 'object Core {' "$config_file" | \
                grep 'VERSION_NAME' | \
                head -1 | \
                sed 's/.*"\(.*\)".*/\1/'
            ;;
        android)
            grep -A 5 'object Android {' "$config_file" | \
                grep 'VERSION_NAME' | \
                head -1 | \
                sed 's/.*"\(.*\)".*/\1/'
            ;;
        integrations/*)
            local name
            name="$(basename "$module")"
            grep -A 5 "moduleName.*=.*\"$name\"" "$config_file" | \
                grep 'versionName' | \
                head -1 | \
                sed 's/.*"\(.*\)".*/\1/'
            ;;
    esac
}

# Read a module's VERSION_CODE / versionCode from RudderStackBuildConfig.kt
# Core returns empty string (JVM module, no version code)
get_module_version_code() {
    local module="$1"
    local repo_root
    repo_root="$(git rev-parse --show-toplevel)"
    local config_file="$repo_root/$BUILD_CONFIG_PATH"

    case "$module" in
        core)
            echo ""
            ;;
        android)
            grep -A 5 'object Android {' "$config_file" | \
                grep 'VERSION_CODE' | \
                head -1 | \
                sed 's/.*"\(.*\)".*/\1/'
            ;;
        integrations/*)
            local name
            name="$(basename "$module")"
            grep -A 5 "moduleName.*=.*\"$name\"" "$config_file" | \
                grep 'versionCode' | \
                head -1 | \
                sed 's/.*"\(.*\)".*/\1/'
            ;;
    esac
}

# Get the tag prefix for a given module
get_module_tag_prefix() {
    local module="$1"
    case "$module" in
        core)
            echo "com.rudderstack.sdk.kotlin.core"
            ;;
        android)
            echo "com.rudderstack.sdk.kotlin.android"
            ;;
        integrations/*)
            local name
            name="$(basename "$module")"
            echo "com.rudderstack.integration.kotlin.$name"
            ;;
    esac
}

# Bump a semver version string according to the bump type.
# Usage: bump_version "1.2.3" "minor" => "1.3.0"
bump_version() {
    local current="$1"
    local type="$2"

    local major minor patch
    IFS='.' read -r major minor patch <<< "$current"

    case "$type" in
        major)
            echo "$(( major + 1 )).0.0"
            ;;
        minor)
            echo "${major}.$(( minor + 1 )).0"
            ;;
        patch)
            echo "${major}.${minor}.$(( patch + 1 ))"
            ;;
        none)
            echo "$current"
            ;;
    esac
}
