#!/bin/bash

# Script to run quality checks on affected modules
# Usage: ./scripts/run-quality-checks.sh "affected_modules_list" [check_type]
# 
# This script runs various quality checks on the specified modules:
# - detekt: Kotlin static analysis
# - lint: Android lint (skips core module)
# - test: Unit tests
# - build: Build modules
# - all: Run all checks (default)

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

# Function to run Detekt on modules
run_detekt() {
    local modules=("$@")
    
    echo "🔍 Running Detekt on affected modules..."
    
    for module in "${modules[@]}"; do
        echo "Running detekt for $module"
        ./gradlew "${module}:detekt"
    done
    
    echo "✅ Detekt completed"
}

# Function to run Android Lint on modules
run_lint() {
    local modules=("$@")
    
    echo "🔍 Running Android Lint on affected modules..."
    
    for module in "${modules[@]}"; do
        # Skip core module for lint (it's not an Android module)
        if [[ "$module" != ":core" ]]; then
            echo "Running lint for $module"
            ./gradlew "${module}:lint"
        else
            echo "Skipping lint for $module (not an Android module)"
        fi
    done
    
    echo "✅ Android Lint completed"
}

# Function to run tests on modules
run_tests() {
    local modules=("$@")
    
    echo "🧪 Running tests on affected modules..."
    
    for module in "${modules[@]}"; do
        echo "Running tests for $module"
        ./gradlew "${module}:test"
    done
    
    echo "✅ Tests completed"
}

# Function to build modules
run_build() {
    local modules=("$@")
    
    echo "🔨 Building affected modules..."
    
    for module in "${modules[@]}"; do
        echo "Building $module"
        ./gradlew "${module}:build"
    done
    
    echo "✅ Build completed"
}

# Main function
main() {
    local affected_modules="$1"
    local check_type="${2:-all}"
    
    echo "🚀 Running quality checks..."
    echo "Affected modules: $affected_modules"
    echo "Check type: $check_type"
    echo ""
    
    if [ -z "$affected_modules" ]; then
        echo "No modules affected, skipping quality checks"
        return 0
    fi
    
    # Parse modules into array
    local modules_array
    if ! modules_array=($(parse_modules "$affected_modules")); then
        echo "Failed to parse modules"
        return 1
    fi
    
    echo "Will run checks on: ${modules_array[*]}"
    echo ""
    
    # Run specified checks
    case "$check_type" in
        "detekt")
            run_detekt "${modules_array[@]}"
            ;;
        "lint")
            run_lint "${modules_array[@]}"
            ;;
        "test")
            run_tests "${modules_array[@]}"
            ;;
        "build")
            run_build "${modules_array[@]}"
            ;;
        "all")
            run_detekt "${modules_array[@]}"
            run_lint "${modules_array[@]}"
            run_tests "${modules_array[@]}"
            run_build "${modules_array[@]}"
            ;;
        *)
            echo "❌ Unknown check type: $check_type"
            echo "Available types: detekt, lint, test, build, all"
            return 1
            ;;
    esac
    
    echo ""
    echo "✅ Quality checks completed successfully"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
