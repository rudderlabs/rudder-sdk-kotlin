#!/bin/bash

# Script to determine release version based on trigger event
# Usage: ./scripts/determine-version.sh [provided_version] [event_name] [release_tag]
# 
# This script:
# 1. Determines version based on workflow trigger (workflow_dispatch, release, push)
# 2. Auto-increments version if none provided
# 3. Validates version format
# 4. Checks for existing tags

set -e

# Function to get current version from config
get_current_version() {
    local current_version
    current_version=$(grep "const val VERSION_NAME" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | sed 's/.*"\([^"]*\)".*/\1/')
    
    if [ -z "$current_version" ]; then
        echo "❌ Could not find VERSION_NAME in RudderStackBuildConfig.kt" >&2
        return 1
    fi
    
    echo "$current_version"
}

# Function to increment version
increment_version() {
    local current_version="$1"
    
    echo "Current version in config: $current_version"
    
    # Parse version components (X.Y.Z)
    local major minor patch
    major=$(echo $current_version | cut -d. -f1)
    minor=$(echo $current_version | cut -d. -f2)
    patch=$(echo $current_version | cut -d. -f3)
    
    # Increment patch/build version
    local new_patch=$((patch + 1))
    local new_version="$major.$minor.$new_patch"
    
    echo "Auto-determined next build version: $new_version"
    echo "$new_version"
}

# Function to validate version format
validate_version_format() {
    local version="$1"
    local version_pattern="^[0-9]+\.[0-9]+\.[0-9]+$"
    
    if ! [[ $version =~ $version_pattern ]]; then
        echo "❌ Invalid version format: $version" >&2
        echo "Version must be in format X.Y.Z (e.g., 1.2.3)" >&2
        return 1
    fi
    
    echo "✅ Version format validated: $version"
}

# Function to check for existing tag
check_existing_tag() {
    local version="$1"
    
    if git tag -l "v$version" | grep -q "v$version"; then
        echo "❌ Tag v$version already exists!" >&2
        return 1
    fi
    
    echo "✅ Tag v$version does not exist, proceeding with release"
}

# Function to determine version based on event
determine_version_by_event() {
    local provided_version="$1"
    local event_name="$2"
    local release_tag="$3"
    
    local version=""
    
    case "$event_name" in
        "workflow_dispatch")
            if [ -n "$provided_version" ]; then
                # Use provided version
                version="$provided_version"
                echo "Using provided version: $version"
            else
                # Auto-determine next build version (increment patch)
                echo "No version provided, determining next build version..."
                local current_version
                current_version=$(get_current_version)
                version=$(increment_version "$current_version")
            fi
            ;;
        "release")
            version="$release_tag"
            # Remove 'v' prefix if present
            version=${version#v}
            echo "Using release tag version: $version"
            ;;
        "push")
            # For release/hotfix branch pushes, auto-increment
            echo "Release branch push detected, auto-incrementing version..."
            local current_version
            current_version=$(get_current_version)
            version=$(increment_version "$current_version")
            ;;
        *)
            echo "❌ Unsupported trigger event: $event_name" >&2
            echo "Supported events: workflow_dispatch, release, push" >&2
            return 1
            ;;
    esac
    
    echo "$version"
}

# Function to check if current branch is a release branch
is_release_branch() {
    local branch_name="${GITHUB_REF_NAME:-$(git rev-parse --abbrev-ref HEAD)}"
    
    if [[ "$branch_name" =~ ^release/.* ]] || [[ "$branch_name" =~ ^hotfix/.* ]]; then
        echo "true"
    else
        echo "false"
    fi
}

# Main function
main() {
    local provided_version="$1"
    local event_name="$2"
    local release_tag="$3"
    
    echo "🔍 Determining release version..."
    echo "Provided version: ${provided_version:-'(none)'}"
    echo "Event name: ${event_name:-'(unknown)'}"
    echo "Release tag: ${release_tag:-'(none)'}"
    echo ""
    
    # Determine version
    local version
    version=$(determine_version_by_event "$provided_version" "$event_name" "$release_tag")
    
    if [ -z "$version" ]; then
        echo "❌ Failed to determine version" >&2
        return 1
    fi
    
    echo "Determined version: $version"
    
    # Validate version format
    validate_version_format "$version"
    
    # Check for existing tag
    check_existing_tag "$version"
    
    # Check if running on release branch
    local is_release_branch_result
    is_release_branch_result=$(is_release_branch)
    
    # Output for GitHub Actions
    if [ -n "$GITHUB_OUTPUT" ]; then
        echo "version=$version" >> "$GITHUB_OUTPUT"
        echo "is_release_branch=$is_release_branch_result" >> "$GITHUB_OUTPUT"
    fi
    
    echo ""
    echo "✅ Version determination completed successfully"
    echo "Final version: $version"
    echo "Is release branch: $is_release_branch_result"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
