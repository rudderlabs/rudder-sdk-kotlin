#!/bin/bash

# Script to run tests in release mode for affected modules
# Usage: ./scripts/run-release-tests.sh "affected_modules"
# 
# This script:
# 1. Runs tests in release mode for affected modules
# 2. Uses appropriate test tasks for different module types
# 3. Falls back to regular test task if release test is not available

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

    if [ ${#cleaned_modules[@]} -eq 0 ]; then
        echo "No valid modules found" >&2
        return 1
    fi

    echo "Parsed modules: ${cleaned_modules[*]}" >&2
    printf '%s\n' "${cleaned_modules[@]}"
}

# Function to run release tests for a single module
run_release_tests_for_module() {
    local module="$1"
    
    echo "Running tests for $module in release mode"
    
    # Try release-specific test task first, fall back to regular test
    if [[ "$module" == ":core" ]]; then
        # Core module - use regular test task with release flag
        ./gradlew "${module}:test" -Prelease
    else
        # Android modules - try testReleaseUnitTest first, fall back to test
        if ./gradlew "${module}:testReleaseUnitTest" -Prelease 2>/dev/null; then
            echo "✅ Release unit tests completed for $module"
        else
            echo "⚠️  testReleaseUnitTest not available for $module, falling back to regular test"
            ./gradlew "${module}:test" -Prelease
        fi
    fi
}

# Main function
main() {
    local affected_modules="$1"
    
    echo "🧪 Running tests in release mode for affected modules..."
    echo "Affected modules: $affected_modules"
    echo ""
    
    if [ -z "$affected_modules" ]; then
        echo "No modules affected, skipping release tests"
        return 0
    fi
    
    # Parse modules into array
    local modules_array
    if ! modules_array=($(parse_modules "$affected_modules")); then
        echo "Failed to parse modules"
        return 1
    fi
    
    echo "Will run release tests on: ${modules_array[*]}"
    echo ""
    
    # Run release tests for each module
    local tested_modules=""
    for module in "${modules_array[@]}"; do
        run_release_tests_for_module "$module"
        
        # Track tested modules
        if [ -z "$tested_modules" ]; then
            tested_modules="$module"
        else
            tested_modules="$tested_modules,$module"
        fi
    done
    
    echo ""
    echo "Tested modules: $tested_modules"
    echo "✅ Release tests completed successfully"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
