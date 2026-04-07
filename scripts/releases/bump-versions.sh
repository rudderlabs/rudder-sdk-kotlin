#!/usr/bin/env bash
# Bumps version numbers in RudderStackBuildConfig.kt and gradle.properties
# based on the affected modules pipe format.
#
# Input: file path or stdin containing: module|bump|oldVersion|newVersion
# Output: modified RudderStackBuildConfig.kt and gradle.properties

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

# --- Bump module versions in RudderStackBuildConfig.kt ---

monorepo_bump="minor"

while IFS='|' read -r module bump old_version new_version; do
    [[ -z "$module" ]] && continue
    validate_module_name "$module"

    old_escaped=$(escape_sed "$old_version")
    new_escaped=$(escape_sed "$new_version")
    module_escaped=$(escape_sed "$module")

    if [[ "$module" == "android" && "$bump" == "major" ]]; then
        monorepo_bump="major"
    fi

    case "$module" in
        core)
            # Match inside `object Core {` block — update VERSION_NAME
            sed_inplace -E "/object Core \{/,/object PublishConfig/ s/(VERSION_NAME = \")${old_escaped}(\")/\1${new_version}\2/" "$BUILD_CONFIG_PATH"
            ;;
        android)
            # Match inside `object Android {` block — update VERSION_NAME
            sed_inplace -E "/object Android \{/,/object PublishConfig/ s/(VERSION_NAME = \")${old_escaped}(\")/\1${new_version}\2/" "$BUILD_CONFIG_PATH"
            # Increment VERSION_CODE
            current_code=$(grep -A5 'object Android {' "$BUILD_CONFIG_PATH" | grep 'VERSION_CODE' | sed -E 's/.*"([0-9]+)".*/\1/')
            if [[ -z "$current_code" || ! "$current_code" =~ ^[0-9]+$ ]]; then
                echo "Error: could not parse VERSION_CODE for module $module" >&2
                exit 1
            fi
            current_code_escaped=$(escape_sed "$current_code")
            new_code=$((current_code + 1))
            sed_inplace -E "/object Android \{/,/object PublishConfig/ s/(VERSION_CODE = \")${current_code_escaped}(\")/\1${new_code}\2/" "$BUILD_CONFIG_PATH"
            ;;
        *)
            # Integration module — match on moduleName = "module"
            sed_inplace -E "/moduleName.*=.*\"${module_escaped}\"/,/override val pomPackaging/ s/(versionName.*=.*\")${old_escaped}(\")/\1${new_version}\2/" "$BUILD_CONFIG_PATH"
            # Increment versionCode
            current_code=$(awk "/moduleName.*\"${module_escaped}\"/,/pomPackaging/" "$BUILD_CONFIG_PATH" | grep 'versionCode' | sed -E 's/.*"([0-9]+)".*/\1/')
            if [[ -z "$current_code" || ! "$current_code" =~ ^[0-9]+$ ]]; then
                echo "Error: could not parse versionCode for module $module" >&2
                exit 1
            fi
            current_code_escaped=$(escape_sed "$current_code")
            new_code=$((current_code + 1))
            sed_inplace -E "/moduleName.*=.*\"${module_escaped}\"/,/override val pomPackaging/ s/(versionCode.*=.*\")${current_code_escaped}(\")/\1${new_code}\2/" "$BUILD_CONFIG_PATH"
            ;;
    esac

done <<< "$affected"

# --- Bump monorepo SDK_VERSION in gradle.properties ---

current_sdk_version=$(grep '^SDK_VERSION=' "$GRADLE_PROPERTIES_PATH" | cut -d'=' -f2)
new_sdk_version=$(bump_version "$current_sdk_version" "$monorepo_bump")
sed_inplace "s/^SDK_VERSION=.*/SDK_VERSION=${new_sdk_version}/" "$GRADLE_PROPERTIES_PATH"
