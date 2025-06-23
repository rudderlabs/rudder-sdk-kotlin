#!/bin/bash

# Script to build AAR/JAR files for affected library modules
# Usage: ./scripts/build-aars.sh "affected_modules_list"
# 
# This script:
# 1. Filters out app module (only builds library modules)
# 2. Handles core module dependency (if core is affected, also builds android)
# 3. Uses appropriate build tasks for different module types
# 4. Outputs results for GitHub Actions

set -e

# Function to parse affected modules
parse_modules() {
    local modules_string="$1"

    if [ -z "$modules_string" ]; then
        echo "No modules provided" >&2
        return 1
    fi

    # Convert comma-separated string to array
    IFS=',' read -ra MODULES <<< "$modules_string"

    # Remove empty elements and trim whitespace
    local cleaned_modules=()
    for module in "${MODULES[@]}"; do
        module=$(echo "$module" | xargs) # trim whitespace
        if [ -n "$module" ]; then
            cleaned_modules+=("$module")
        fi
    done

    printf '%s\n' "${cleaned_modules[@]}"
}

# Function to determine modules to build
determine_modules_to_build() {
    local modules=("$@")
    local modules_to_build=()
    
    # First, collect all library modules (exclude app module)
    for module in "${modules[@]}"; do
        if [[ "$module" != ":app" ]]; then
            modules_to_build+=("$module")
        fi
    done
    
    # If core module is affected, also build android module (dependency)
    local core_affected=false
    for module in "${modules_to_build[@]}"; do
        if [[ "$module" == ":core" ]]; then
            core_affected=true
            break
        fi
    done
    
    if [ "$core_affected" = true ]; then
        # Check if android module is already in the list
        local android_in_list=false
        for module in "${modules_to_build[@]}"; do
            if [[ "$module" == ":android" ]]; then
                android_in_list=true
                break
            fi
        done
        
        # Add android module if not already present
        if [ "$android_in_list" = false ]; then
            echo "Core module affected - also building android module due to dependency" >&2
            modules_to_build+=(":android")
        fi
    fi
    
    printf '%s\n' "${modules_to_build[@]}"
}

# Function to build a single module
build_module() {
    local module="$1"
    
    echo "Building AAR/JAR for $module"
    
    # Use correct build task based on module type
    if [[ "$module" == ":core" ]]; then
        # Core is a Kotlin/JVM module - use assemble task
        ./gradlew "${module}:assemble"
    else
        # Android modules (android, integrations) - use assembleRelease task
        ./gradlew "${module}:assembleRelease"
    fi
}

# Main function
main() {
    local affected_modules="$1"
    
    echo "📦 Building AAR files for affected library modules..."
    echo "Affected modules: $affected_modules"
    echo ""
    
    if [ -z "$affected_modules" ]; then
        echo "No modules affected, skipping AAR build"
        if [ -n "$GITHUB_OUTPUT" ]; then
            echo "has_artifacts=false" >> "$GITHUB_OUTPUT"
        fi
        return 0
    fi
    
    # Parse modules into array
    local modules_array
    if ! modules_array=($(parse_modules "$affected_modules")); then
        echo "Failed to parse modules"
        if [ -n "$GITHUB_OUTPUT" ]; then
            echo "has_artifacts=false" >> "$GITHUB_OUTPUT"
        fi
        return 1
    fi
    
    # Determine which modules to build
    local modules_to_build
    modules_to_build=($(determine_modules_to_build "${modules_array[@]}"))
    
    if [ ${#modules_to_build[@]} -eq 0 ]; then
        echo "No library modules were affected"
        if [ -n "$GITHUB_OUTPUT" ]; then
            echo "has_artifacts=false" >> "$GITHUB_OUTPUT"
        fi
        return 0
    fi
    
    echo "Will build: ${modules_to_build[*]}"
    echo ""
    
    # Build all required modules
    local built_modules=""
    for module in "${modules_to_build[@]}"; do
        build_module "$module"
        
        # Track which modules were built for artifact upload
        if [ -z "$built_modules" ]; then
            built_modules="$module"
        else
            built_modules="$built_modules,$module"
        fi
    done
    
    # Output for GitHub Actions
    if [ -n "$GITHUB_OUTPUT" ]; then
        echo "built_modules=$built_modules" >> "$GITHUB_OUTPUT"
        echo "has_artifacts=true" >> "$GITHUB_OUTPUT"
    fi
    
    echo ""
    echo "Built modules: $built_modules"
    echo "Listing generated artifacts..."
    find . -name "*.aar" -o -name "*.jar" | grep -E "(core|android|integrations)" | head -20
    
    echo ""
    echo "✅ AAR build completed successfully"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
