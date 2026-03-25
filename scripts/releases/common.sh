#!/usr/bin/env bash
# Shared utilities for release automation scripts.
# Sourced by all other scripts in this directory.

set -euo pipefail

# --- Constants ---

REPO_URL="https://github.com/rudderlabs/rudder-sdk-kotlin"
BUILD_CONFIG_PATH="buildSrc/src/main/kotlin/RudderStackBuildConfig.kt"
GRADLE_PROPERTIES_PATH="gradle.properties"

# --- Security helpers ---

escape_sed() {
    local input="$1"
    printf '%s' "$input" | sed 's/[.[\/*^$&]/\\&/g'
    return 0
}

escape_grep() {
    local input="$1"
    printf '%s' "$input" | sed 's/[.[\*^$+?{}()|]/\\&/g'
    return 0
}

validate_module_name() {
    local name="$1"
    if [[ ! "$name" =~ ^[a-zA-Z0-9_-]+$ ]]; then
        echo "Error: invalid module name: $name" >&2
        exit 1
    fi
    return 0
}

# --- Portable sed -i ---

sed_inplace() {
    if [[ "$(uname)" == "Darwin" ]]; then
        sed -i '' "$@"
    else
        sed -i "$@"
    fi
    return 0
}

# --- Monorepo tag ---

find_monorepo_tag() {
    git tag -l 'v*.*.*' --sort=-v:refname | head -1
    return 0
}

# --- Dependency chain (session-cached) ---

_DEPENDENCY_CHAIN_CACHE=""

get_dependency_chain() {
    if [[ -z "$_DEPENDENCY_CHAIN_CACHE" ]]; then
        local output
        output=$(./gradlew -q printDependencyChain 2>&1) || {
            echo "Error: Gradle printDependencyChain failed:" >&2
            echo "$output" >&2
            exit 1
        }
        _DEPENDENCY_CHAIN_CACHE=$(echo "$output" | grep '|')
        if [[ -z "$_DEPENDENCY_CHAIN_CACHE" ]]; then
            echo "Error: no dependency chain data found" >&2
            exit 1
        fi
    fi
    echo "$_DEPENDENCY_CHAIN_CACHE"
    return 0
}

# --- Module discovery ---

get_all_modules() {
    get_dependency_chain | tail -n +2 | cut -d'|' -f1 | tr '\n' ' ' | sed 's/ $//'
    return 0
}

get_module_maven_info() {
    local module="$1"
    local escaped
    escaped=$(escape_grep "$module")
    get_dependency_chain | grep "^${escaped}|" | cut -d'|' -f2-5
    return 0
}

get_module_version() {
    local module="$1"
    local escaped
    escaped=$(escape_grep "$module")
    get_dependency_chain | grep "^${escaped}|" | cut -d'|' -f4 | sed 's/-SNAPSHOT//'
    return 0
}

get_module_tag_prefix() {
    local module="$1"
    local escaped info
    escaped=$(escape_grep "$module")
    info=$(get_dependency_chain | grep "^${escaped}|")
    local group_id artifact_id
    group_id=$(echo "$info" | cut -d'|' -f2)
    artifact_id=$(echo "$info" | cut -d'|' -f3)
    echo "${group_id}.${artifact_id}"
    return 0
}

# --- Dependency graph ---

get_module_deps() {
    local module="$1"
    local escaped deps_field
    escaped=$(escape_grep "$module")
    deps_field=$(get_dependency_chain | grep "^${escaped}|" | cut -d'|' -f6)
    if [[ -z "$deps_field" ]]; then
        return
    fi
    echo "$deps_field" | tr ',' '\n' | cut -d':' -f1 | tr '\n' ' ' | sed 's/ $//'
}

get_dependents() {
    local module="$1"
    local escaped
    escaped=$(escape_grep "$module")
    get_dependency_chain | tail -n +2 | while IFS='|' read -r name _ _ _ _ deps; do
        if [[ -n "$deps" ]] && echo "$deps" | tr ',' '\n' | cut -d':' -f1 | grep -qx "$escaped"; then
            echo "$name"
        fi
    done | tr '\n' ' ' | sed 's/ $//'
    return 0
}

# --- Version utilities ---

bump_version() {
    local version="${1%%-*}"  # Strip any suffix like -SNAPSHOT
    local bump_type="$2"
    if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Error: invalid version format: $version" >&2
        exit 1
    fi
    local major minor patch
    IFS='.' read -r major minor patch <<< "$version"

    case "$bump_type" in
        major) echo "$((major + 1)).0.0" ;;
        minor) echo "${major}.$((minor + 1)).0" ;;
        patch) echo "${major}.${minor}.$((patch + 1))" ;;
        *) echo "Error: invalid bump type: $bump_type" >&2; exit 1 ;;
    esac
    return 0
}

max_bump() {
    local a="$1"
    local b="$2"
    _bump_rank() {
        local val="$1"
        case "$val" in
            major) echo 3 ;; minor) echo 2 ;; patch) echo 1 ;; *) echo 0 ;;
        esac
        return 0
    }
    local rank_a rank_b
    rank_a=$(_bump_rank "$a")
    rank_b=$(_bump_rank "$b")
    if [[ "$rank_a" -ge "$rank_b" ]]; then
        echo "$a"
    else
        echo "$b"
    fi
    return 0
}

# --- Path mapping ---

file_to_module() {
    local filepath="$1"
    local module=""
    case "$filepath" in
        core/*) module="core" ;;
        android/*) module="android" ;;
        integrations/*/*)
            module=$(echo "$filepath" | cut -d'/' -f2)
            ;;
        *) echo ""; return ;;
    esac
    validate_module_name "$module"
    echo "$module"
}

module_to_gradle_path() {
    local module="$1"
    validate_module_name "$module"
    case "$module" in
        core|android) echo ":${module}" ;;
        *) echo ":integrations:${module}" ;;
    esac
    return 0
}
