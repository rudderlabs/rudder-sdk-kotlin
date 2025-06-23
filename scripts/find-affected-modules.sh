#!/bin/bash

# Script to find affected modules based on git changes
# Usage: ./scripts/find-affected-modules.sh [base_branch]
#
# This script:
# 1. Determines the base branch to compare against
# 2. Finds changed files
# 3. Maps changed files to affected modules
# 4. Handles module dependencies (e.g., if :core is affected, :android is also affected)
# 5. Outputs results in GitHub Actions format

set -e

# Function to determine base branch
determine_base_branch() {
    local provided_base="$1"

    if [ -n "$provided_base" ]; then
        # If provided base doesn't start with "origin/", add it
        if [[ "$provided_base" != origin/* ]]; then
            # Check if the remote branch exists
            if git rev-parse --verify "origin/$provided_base" >/dev/null 2>&1; then
                echo "origin/$provided_base"
            else
                echo "⚠️  Warning: origin/$provided_base not found, falling back to HEAD~1" >&2
                echo "HEAD~1"
            fi
        else
            echo "$provided_base"
        fi
        return
    fi

    # For PR context, use github.base_ref if available
    if [ -n "$GITHUB_BASE_REF" ]; then
        echo "origin/$GITHUB_BASE_REF"
        return
    fi

    # For branch validation, use complex logic
    if git merge-base --is-ancestor origin/develop HEAD 2>/dev/null; then
        # If current branch is derived from develop
        echo "origin/develop"
    else
        # Otherwise use the most recent ancestor
        local base_commit
        base_commit=$(git merge-base HEAD origin/main 2>/dev/null || git merge-base HEAD origin/develop 2>/dev/null || echo "HEAD~1")
        echo "$base_commit"
    fi
}

# Function to find affected modules
find_affected_modules() {
    local base_branch="$1"

    echo "Comparing against base: $base_branch"

    # Get changed files
    local changed_files
    if ! changed_files=$(git diff --name-only "$base_branch...HEAD" 2>/dev/null); then
        echo "⚠️  Failed to get diff against $base_branch, falling back to all modules"
        # If git diff fails, consider all modules as affected
        changed_files=$(find core android integrations/* -name "*.kt" -o -name "*.java" -o -name "*.gradle*" 2>/dev/null | head -10 || echo "core/dummy android/dummy")
    fi

    echo "Changed files:"
    echo "$changed_files"
    echo ""

    # Create a list of affected modules
    local affected_modules=""
    local core_affected=false

    # Check for changes in each module directory
    for dir in core android integrations/*; do
        if [ -d "$dir" ] && echo "$changed_files" | grep -q "^$dir/"; then
            local module_path
            module_path=$(echo "$dir" | sed 's/\//:/')

            # Track if core module is affected
            if [ "$dir" = "core" ]; then
                core_affected=true
            fi

            if [ -z "$affected_modules" ]; then
                affected_modules=":$module_path"
            else
                affected_modules="$affected_modules,:$module_path"
            fi

            # Output for GitHub Actions
            if [ -n "$GITHUB_OUTPUT" ]; then
                echo "$dir=true" >> "$GITHUB_OUTPUT"
            fi
            echo "✓ Module affected: $dir"
        else
            if [ -d "$dir" ]; then
                # Output for GitHub Actions
                if [ -n "$GITHUB_OUTPUT" ]; then
                    echo "$dir=false" >> "$GITHUB_OUTPUT"
                fi
                echo "○ Module not affected: $dir"
            fi
        fi
    done

    # If core is affected, ensure android is also affected (dependency relationship)
    if [ "$core_affected" = true ] && ! echo "$affected_modules" | grep -q ":android"; then
        if [ -z "$affected_modules" ]; then
            affected_modules=":android"
        else
            affected_modules="$affected_modules,:android"
        fi

        # Update GitHub Actions output
        if [ -n "$GITHUB_OUTPUT" ]; then
            echo "android=true" >> "$GITHUB_OUTPUT"
        fi
        echo "✓ Module affected (dependency): android (depends on core)"
    fi

    echo ""
    echo "Affected modules: $affected_modules"

    # Output for GitHub Actions
    if [ -n "$GITHUB_OUTPUT" ]; then
        echo "affected_modules=$affected_modules" >> "$GITHUB_OUTPUT"
    fi

    # Also output to stdout for local testing
    echo "AFFECTED_MODULES=$affected_modules"
}

# Main execution
main() {
    local base_branch_input="$1"
    
    echo "🔍 Finding affected modules..."
    echo "Working directory: $(pwd)"
    echo ""
    
    local base_branch
    base_branch=$(determine_base_branch "$base_branch_input")
    
    find_affected_modules "$base_branch"
    
    echo "✅ Module detection complete"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
