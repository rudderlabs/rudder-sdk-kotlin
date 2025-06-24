#!/bin/bash

# Script to determine base branch for release comparisons
# Usage: ./scripts/determine-release-base-branch.sh [is_release_branch]
# 
# This script:
# 1. Determines the appropriate base branch for comparing changes
# 2. Uses different logic for release branches vs manual releases
# 3. Falls back to sensible defaults

set -e

# Function to check if a remote branch exists
remote_branch_exists() {
    local branch="$1"
    git rev-parse --verify "origin/$branch" >/dev/null 2>&1
}

# Function to check if current HEAD is ancestor of a branch
is_ancestor_of() {
    local branch="$1"
    git merge-base --is-ancestor "origin/$branch" HEAD 2>/dev/null
}

# Function to determine base branch
determine_base_branch() {
    local is_release_branch="$1"

    echo "Determining base branch for release comparison..." >&2
    echo "Is release branch: $is_release_branch" >&2

    local base_branch=""

    if [ "$is_release_branch" = "true" ]; then
        # For release branches, compare against develop branch
        if remote_branch_exists "develop"; then
            base_branch="develop"
            echo "Release branch detected - using develop as base" >&2
        elif remote_branch_exists "main"; then
            base_branch="main"
            echo "⚠️  develop branch not found, falling back to main" >&2
        else
            echo "❌ Neither develop nor main branch found" >&2
            return 1
        fi
    else
        # For manual releases, use more complex logic
        echo "Manual release detected - determining best base branch..." >&2

        if remote_branch_exists "develop" && is_ancestor_of "develop"; then
            # If current branch is derived from develop
            base_branch="develop"
            echo "Current branch is derived from develop - using develop as base" >&2
        elif remote_branch_exists "main" && is_ancestor_of "main"; then
            # If current branch is derived from main
            base_branch="main"
            echo "Current branch is derived from main - using main as base" >&2
        elif remote_branch_exists "develop"; then
            # Default to develop if it exists
            base_branch="develop"
            echo "Defaulting to develop branch" >&2
        elif remote_branch_exists "main"; then
            # Fall back to main
            base_branch="main"
            echo "Defaulting to main branch" >&2
        else
            echo "❌ No suitable base branch found (develop or main)" >&2
            return 1
        fi
    fi

    echo "Selected base branch: $base_branch" >&2
    echo "$base_branch"
}

# Function to validate base branch
validate_base_branch() {
    local base_branch="$1"

    if [ -z "$base_branch" ]; then
        echo "❌ Base branch is empty" >&2
        return 1
    fi

    if ! remote_branch_exists "$base_branch"; then
        echo "❌ Base branch origin/$base_branch does not exist" >&2
        return 1
    fi

    echo "✅ Base branch origin/$base_branch exists and is valid" >&2
}

# Main function
main() {
    local is_release_branch="${1:-false}"
    
    echo "🔍 Determining base branch for release comparison..." >&2
    echo "" >&2

    # Determine base branch
    local base_branch
    base_branch=$(determine_base_branch "$is_release_branch")

    if [ -z "$base_branch" ]; then
        echo "❌ Failed to determine base branch" >&2
        return 1
    fi

    # Validate base branch
    validate_base_branch "$base_branch"

    # Output for GitHub Actions
    if [ -n "$GITHUB_OUTPUT" ]; then
        echo "base_branch=$base_branch" >> "$GITHUB_OUTPUT"
    fi

    echo "" >&2
    echo "✅ Base branch determination completed successfully" >&2
    echo "Base branch: $base_branch" >&2
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
