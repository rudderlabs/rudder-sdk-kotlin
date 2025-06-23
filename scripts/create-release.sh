#!/bin/bash

# Script to create GitHub release with changelog and artifacts
# Usage: ./scripts/create-release.sh "version" "affected_modules" "built_modules" [dry_run]
# 
# This script:
# 1. Generates changelog from git history
# 2. Creates GitHub release with artifacts
# 3. Creates and pushes git tag

set -e

# Function to generate changelog
generate_changelog() {
    local version="$1"
    local affected_modules="$2"
    local built_modules="$3"
    
    echo "Generating changelog for version $version..."
    
    # Try to find the previous tag
    local previous_tag
    previous_tag=$(git tag --sort=-version:refname | grep -E "^v[0-9]+\.[0-9]+\.[0-9]+$" | head -1 2>/dev/null || echo "")
    
    local changelog
    if [ -n "$previous_tag" ]; then
        echo "Generating changelog from $previous_tag to HEAD"
        changelog=$(git log --pretty=format:"- %s (%h)" $previous_tag..HEAD --no-merges)
    else
        echo "No previous tag found, generating changelog from first commit"
        changelog=$(git log --pretty=format:"- %s (%h)" --no-merges)
    fi
    
    # Create changelog content
    cat > changelog.md << EOF
## What's Changed

$changelog

## Affected Modules

$affected_modules

## Built Artifacts

$built_modules

**Full Changelog**: https://github.com/$GITHUB_REPOSITORY/compare/$previous_tag...v$version
EOF
    
    echo "Changelog generated: changelog.md"
}

# Function to create GitHub release
create_github_release() {
    local version="$1"
    local dry_run="$2"
    
    if [ "$dry_run" = "true" ]; then
        echo "Dry run mode: skipping GitHub release creation"
        return 0
    fi
    
    echo "Creating GitHub release for version $version..."
    
    # Create release with GitHub CLI
    local release_output
    release_output=$(gh release create "v$version" \
        --title "Android SDK v$version" \
        --notes-file changelog.md \
        --prerelease \
        artifacts/**/*.aar artifacts/**/*.jar 2>&1)
    
    echo "Release creation output:"
    echo "$release_output"
    
    # Get release URL and ID
    local release_url release_id
    release_url=$(gh release view "v$version" --json url --jq '.url')
    release_id=$(gh release view "v$version" --json id --jq '.id')
    
    # Output for GitHub Actions
    if [ -n "$GITHUB_OUTPUT" ]; then
        echo "id=$release_id" >> "$GITHUB_OUTPUT"
        echo "html_url=$release_url" >> "$GITHUB_OUTPUT"
    fi
    
    echo "Release created: $release_url"
    echo "Release ID: $release_id"
}

# Function to create and push git tag
create_git_tag() {
    local version="$1"
    local dry_run="$2"
    
    if [ "$dry_run" = "true" ]; then
        echo "Dry run mode: skipping git tag creation"
        return 0
    fi
    
    echo "Creating and pushing git tag v$version..."
    
    git config --local user.email "action@github.com"
    git config --local user.name "GitHub Action"
    git tag -a "v$version" -m "Release v$version"
    git push origin "v$version"
    
    echo "Git tag v$version created and pushed"
}

# Function to validate environment
validate_environment() {
    if [ -z "$GITHUB_TOKEN" ]; then
        echo "❌ GITHUB_TOKEN environment variable is required"
        return 1
    fi
    
    if [ -z "$GITHUB_REPOSITORY" ]; then
        echo "❌ GITHUB_REPOSITORY environment variable is required"
        return 1
    fi
    
    # Check if gh CLI is available
    if ! command -v gh &> /dev/null; then
        echo "❌ GitHub CLI (gh) is not installed"
        return 1
    fi
    
    echo "✅ Environment validation passed"
}

# Function to check if artifacts exist
check_artifacts() {
    if [ ! -d "artifacts" ]; then
        echo "⚠️  No artifacts directory found"
        return 1
    fi
    
    local artifact_count
    artifact_count=$(find artifacts -name "*.aar" -o -name "*.jar" | wc -l)
    
    if [ "$artifact_count" -eq 0 ]; then
        echo "⚠️  No AAR or JAR artifacts found in artifacts directory"
        return 1
    fi
    
    echo "✅ Found $artifact_count artifacts"
    find artifacts -name "*.aar" -o -name "*.jar" | head -10
}

# Main function
main() {
    local version="$1"
    local affected_modules="$2"
    local built_modules="$3"
    local dry_run="${4:-false}"
    
    if [ -z "$version" ]; then
        echo "❌ Version is required"
        echo "Usage: $0 <version> [affected_modules] [built_modules] [dry_run]"
        return 1
    fi
    
    echo "🚀 Creating release for version $version"
    echo "Affected modules: $affected_modules"
    echo "Built modules: $built_modules"
    echo "Dry run: $dry_run"
    echo ""
    
    # Validate environment
    validate_environment
    
    # Check artifacts (only if not dry run)
    if [ "$dry_run" != "true" ]; then
        check_artifacts
    fi
    
    # Generate changelog
    generate_changelog "$version" "$affected_modules" "$built_modules"
    
    # Create GitHub release
    create_github_release "$version" "$dry_run"
    
    # Create git tag
    create_git_tag "$version" "$dry_run"
    
    echo ""
    echo "✅ Release creation completed successfully"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
