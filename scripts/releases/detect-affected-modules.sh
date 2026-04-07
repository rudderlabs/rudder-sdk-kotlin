#!/usr/bin/env bash
# Detects which modules need a version bump based on conventional commits
# since the last monorepo tag. Outputs affected modules in dependency order.
#
# Output format: module|bump|oldVersion|newVersion

set -euo pipefail
set -f

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

# --- Configurable bump rules ---
# Add/remove commit types to control which changes trigger bumps.

MAJOR_TYPES="feat!"
MINOR_TYPES="feat"
PATCH_TYPES="fix refactor chore"

# --- Key-value store (bash 3.2 compatible) ---
# Uses a flat string: "key1=val1 key2=val2"

_KV_BUMPS=""

_kv_get() {
    local key="$1" store="$2"
    local val
    val=$(echo "$store" | tr ' ' '\n' | grep "^${key}=" | tail -1 | cut -d'=' -f2)
    echo "${val:-none}"
    return 0
}

_kv_set() {
    local key="$1" val="$2"
    # Remove existing entry, then append
    _KV_BUMPS=$(echo "$_KV_BUMPS" | tr ' ' '\n' | grep -v "^${key}=" | tr '\n' ' ')
    _KV_BUMPS="${_KV_BUMPS}${key}=${val} "
    return 0
}

# --- Helpers ---

get_bump_level() {
    local type="$1"
    local breaking="$2"
    # Build the lookup key: "feat!" if breaking, "feat" otherwise
    local lookup="$type"
    if [[ "$breaking" == "true" ]]; then
        lookup="${type}!"
    fi

    for t in $MAJOR_TYPES; do
        if [[ "$lookup" == "$t" ]]; then echo "major"; return; fi
    done
    for t in $MINOR_TYPES; do
        if [[ "$lookup" == "$t" ]]; then echo "minor"; return; fi
    done
    for t in $PATCH_TYPES; do
        if [[ "$lookup" == "$t" ]]; then echo "patch"; return; fi
    done

    # Breaking types not in any list fall through to their non-breaking level
    if [[ "$breaking" == "true" ]]; then
        for t in $MINOR_TYPES; do
            if [[ "$type" == "$t" ]]; then echo "minor"; return; fi
        done
        for t in $PATCH_TYPES; do
            if [[ "$type" == "$t" ]]; then echo "patch"; return; fi
        done
    fi

    echo "none"
}

get_cascade_bump() {
    local upstream_bump="$1"
    case "$upstream_bump" in
        major) echo "major" ;;
        minor|patch) echo "patch" ;;
        *) echo "none" ;;
    esac
    return 0
}

# --- Main ---

baseline=$(find_monorepo_tag)
if [[ -z "$baseline" ]]; then
    echo "Error: no monorepo tag found" >&2
    exit 1
fi

commit_count=$(git rev-list --count --no-merges "${baseline}..HEAD")
if [[ "$commit_count" -gt 1000 ]]; then
    echo "Error: too many commits since last tag ($commit_count). Check baseline tag." >&2
    exit 1
fi

# Step 1: Parse commits and determine direct bumps per module
while IFS= read -r line; do
    [[ -z "$line" ]] && continue

    commit_hash="${line%% *}"
    subject="${line#* }"

    # Parse conventional commit: type(scope)!: description
    breaking="false"
    if echo "$subject" | grep -qE '^[a-zA-Z]+(\(.+\))?!: '; then
        breaking="true"
    fi

    type=$(echo "$subject" | sed -E 's/^([a-zA-Z]+)(\(.+\))?(!)?: .+/\1/')
    # Verify it actually matched a conventional commit pattern
    if [[ "$type" == "$subject" ]]; then
        continue
    fi

    bump=$(get_bump_level "$type" "$breaking")
    [[ "$bump" == "none" ]] && continue

    # Get changed files for this commit
    while IFS= read -r filepath; do
        [[ -z "$filepath" ]] && continue
        module=$(file_to_module "$filepath")
        [[ -z "$module" ]] && continue

        current=$(_kv_get "$module" "$_KV_BUMPS")
        _kv_set "$module" "$(max_bump "$current" "$bump")"
    done < <(git diff-tree --no-commit-id --name-only -r "$commit_hash")

done < <(git log --no-merges --format="%H %s" "${baseline}..HEAD")

# Step 2: Build topological order, then cascade in that order
chain=$(get_dependency_chain)
ordered_modules=""
visited_modules=""

topo_visit() {
    local mod="$1"
    echo "$visited_modules" | tr ' ' '\n' | grep -qx "$mod" && return
    visited_modules="${visited_modules}${mod} "

    local deps
    deps=$(get_module_deps "$mod")
    for d in $deps; do
        topo_visit "$d"
    done
    ordered_modules="${ordered_modules}${mod} "
}

while IFS='|' read -r name _rest; do
    [[ "$name" == "name" ]] && continue
    topo_visit "$name"
done <<< "$chain"

# Cascade in topological order (upstream processed before downstream)
for module in $ordered_modules; do
    bump=$(_kv_get "$module" "$_KV_BUMPS")
    [[ "$bump" == "none" ]] && continue

    cascade=$(get_cascade_bump "$bump")
    [[ "$cascade" == "none" ]] && continue

    dependents=$(get_dependents "$module")
    for dep in $dependents; do
        current=$(_kv_get "$dep" "$_KV_BUMPS")
        _kv_set "$dep" "$(max_bump "$current" "$cascade")"
    done
done

# Step 3: Output in dependency order
for module in $ordered_modules; do
    bump=$(_kv_get "$module" "$_KV_BUMPS")
    [[ "$bump" == "none" ]] && continue

    old_version=$(get_module_version "$module")
    new_version=$(bump_version "$old_version" "$bump")
    echo "${module}|${bump}|${old_version}|${new_version}"
done
