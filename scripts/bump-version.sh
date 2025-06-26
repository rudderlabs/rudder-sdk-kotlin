#!/bin/bash

# Script to bump version in RudderStackBuildConfig.kt
# Usage: ./scripts/bump-version.sh "version" "affected_modules"
# 
# This script:
# 1. Updates VERSION_NAME and VERSION_CODE for core SDK
# 2. Updates integration module versions for affected integrations
# 3. Commits the changes to git

set -e

# Function to update core SDK version
update_core_version() {
    local version="$1"

    echo "Updating core SDK version to: $version"

    # Update the VERSION_NAME in RudderStackBuildConfig.kt
    # Use different sed syntax for macOS vs Linux
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/const val VERSION_NAME = \".*\"/const val VERSION_NAME = \"$version\"/" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
    else
        sed -i "s/const val VERSION_NAME = \".*\"/const val VERSION_NAME = \"$version\"/" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
    fi

    # Get current VERSION_CODE and increment by 1
    local current_version_code
    current_version_code=$(grep "const val VERSION_CODE" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | sed 's/.*"\([0-9]*\)".*/\1/')
    local new_version_code=$((current_version_code + 1))
    echo "Incrementing VERSION_CODE from $current_version_code to $new_version_code"

    # Update the VERSION_CODE in RudderStackBuildConfig.kt
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/const val VERSION_CODE = \".*\"/const val VERSION_CODE = \"$new_version_code\"/" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
    else
        sed -i "s/const val VERSION_CODE = \".*\"/const val VERSION_CODE = \"$new_version_code\"/" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
    fi
}

# Function to update integration module version
update_integration_version() {
    local integration_name="$1"
    local provided_version="$2"

    echo "Updating integration module: $integration_name"

    # Capitalize first letter of integration name (portable way)
    local capitalized_name
    capitalized_name=$(echo "$integration_name" | awk '{print toupper(substr($0,1,1)) tolower(substr($0,2))}')

    # Get current integration versionName
    local current_integration_version
    current_integration_version=$(grep -A 10 "object $capitalized_name : IntegrationModuleInfo" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | grep "override val versionName: String" | sed 's/.*"\([^"]*\)".*/\1/')

    local new_integration_version
    if [ -n "$provided_version" ]; then
        # Use provided version for integration (same as core/android)
        new_integration_version="$provided_version"
        echo "Setting $integration_name versionName to provided version: $new_integration_version"
    elif [ -n "$current_integration_version" ]; then
        # Auto-increment patch/build version for integration
        local int_major int_minor int_patch
        int_major=$(echo $current_integration_version | cut -d. -f1)
        int_minor=$(echo $current_integration_version | cut -d. -f2)
        int_patch=$(echo $current_integration_version | cut -d. -f3)

        local new_int_patch=$((int_patch + 1))
        new_integration_version="$int_major.$int_minor.$new_int_patch"
        echo "Auto-incrementing $integration_name versionName from $current_integration_version to $new_integration_version"
    else
        echo "⚠️  Could not determine current version for $integration_name, skipping version update"
        return
    fi

    # Update versionName
    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "/object $capitalized_name : IntegrationModuleInfo/,/override val versionName: String/ s/override val versionName: String = \".*\"/override val versionName: String = \"$new_integration_version\"/" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
    else
        sed -i "/object $capitalized_name : IntegrationModuleInfo/,/override val versionName: String/ s/override val versionName: String = \".*\"/override val versionName: String = \"$new_integration_version\"/" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
    fi

    # Get current integration versionCode and increment by 1
    local current_integration_version_code
    current_integration_version_code=$(grep -A 10 "object $capitalized_name : IntegrationModuleInfo" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | grep "override val versionCode: String" | sed 's/.*"\([0-9]*\)".*/\1/')

    if [ -n "$current_integration_version_code" ]; then
        local new_integration_version_code=$((current_integration_version_code + 1))
        echo "Incrementing $integration_name versionCode from $current_integration_version_code to $new_integration_version_code"
        if [[ "$OSTYPE" == "darwin"* ]]; then
            sed -i '' "/object $capitalized_name : IntegrationModuleInfo/,/override val versionCode: String/ s/override val versionCode: String = \".*\"/override val versionCode: String = \"$new_integration_version_code\"/" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
        else
            sed -i "/object $capitalized_name : IntegrationModuleInfo/,/override val versionCode: String/ s/override val versionCode: String = \".*\"/override val versionCode: String = \"$new_integration_version_code\"/" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
        fi
    fi
}

# Function to check if core or android modules are affected
is_core_or_android_affected() {
    local affected_modules="$1"

    if echo "$affected_modules" | grep -q ":core" || echo "$affected_modules" | grep -q ":android"; then
        echo "true"
    else
        echo "false"
    fi
}

# Function to parse affected modules and update versions
update_affected_modules() {
    local affected_modules="$1"
    local version="$2"

    # Check if core or android modules are affected
    local core_android_affected
    core_android_affected=$(is_core_or_android_affected "$affected_modules")

    if [ "$core_android_affected" = "true" ]; then
        echo "Core or Android module affected - updating core SDK version"
        update_core_version "$version"
    fi

    if [ -n "$affected_modules" ]; then
        IFS=',' read -ra MODULES <<< "$affected_modules"
        for module in "${MODULES[@]}"; do
            # Check if this is an integration module
            if [[ "$module" =~ ^:integrations: ]]; then
                # Extract integration name (e.g., ":integrations:firebase" -> "firebase")
                local integration_name
                integration_name=$(echo "$module" | sed 's/:integrations://')
                update_integration_version "$integration_name" "$version"
            fi
        done
    fi
}

# Function to verify changes
verify_changes() {
    echo ""
    echo "Updated RudderStackBuildConfig.kt:"
    echo "=== Core SDK versions ==="
    grep -E "(VERSION_NAME|VERSION_CODE)" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
    echo "=== Integration versions ==="
    grep -E "(versionName|versionCode)" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
}

# Function to commit changes
commit_changes() {
    local version="$1"
    local dry_run="$2"
    
    if [ "$dry_run" != "true" ]; then
        echo "Committing version bump..."
        git config --local user.email "action@github.com"
        git config --local user.name "GitHub Action"
        git add buildSrc/src/main/kotlin/RudderStackBuildConfig.kt
        git commit -m "chore: bump version to $version" || echo "No changes to commit"
        git push
    else
        echo "Dry run mode: skipping commit"
    fi
}

# Main function
main() {
    local version="$1"
    local affected_modules="$2"
    local dry_run="${3:-false}"
    
    if [ -z "$version" ]; then
        echo "❌ Version is required"
        echo "Usage: $0 <version> [affected_modules] [dry_run]"
        return 1
    fi
    
    echo "🔄 Bumping version to: $version"
    echo "Affected modules: $affected_modules"
    echo "Dry run: $dry_run"
    echo ""

    # Update affected module versions (includes core SDK if core/android affected)
    update_affected_modules "$affected_modules" "$version"
    
    # Verify the changes
    verify_changes
    
    # Commit changes
    commit_changes "$version" "$dry_run"
    
    echo ""
    echo "✅ Version bump completed successfully"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
