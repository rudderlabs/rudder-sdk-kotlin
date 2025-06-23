#!/bin/bash

# Script to publish affected modules to Maven staging
# Usage: ./scripts/publish-to-maven.sh "affected_modules" [dry_run]
# 
# This script:
# 1. Publishes each affected library module to Maven staging
# 2. Excludes app module from publishing
# 3. Uses release mode for publishing

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

# Function to publish a single module
publish_module() {
    local module="$1"
    
    echo "Publishing $module to Maven staging..."
    
    # Publish module to staging with release flag
    ./gradlew "${module}:publish" publishToSonatype -Prelease
}

# Function to validate environment variables
validate_environment() {
    local missing_vars=()
    
    # Check required environment variables
    if [ -z "$SIGNING_KEY_ID" ]; then
        missing_vars+=("SIGNING_KEY_ID")
    fi
    
    if [ -z "$SIGNING_KEY_PASSWORD" ]; then
        missing_vars+=("SIGNING_KEY_PASSWORD")
    fi
    
    if [ -z "$NEXUS_USERNAME" ]; then
        missing_vars+=("NEXUS_USERNAME")
    fi
    
    if [ -z "$NEXUS_PASSWORD" ]; then
        missing_vars+=("NEXUS_PASSWORD")
    fi
    
    if [ -z "$SONATYPE_STAGING_PROFILE_ID" ]; then
        missing_vars+=("SONATYPE_STAGING_PROFILE_ID")
    fi
    
    if [ -z "$SIGNING_PRIVATE_KEY_BASE64" ]; then
        missing_vars+=("SIGNING_PRIVATE_KEY_BASE64")
    fi
    
    if [ ${#missing_vars[@]} -gt 0 ]; then
        echo "❌ Missing required environment variables:"
        printf '  - %s\n' "${missing_vars[@]}"
        return 1
    fi
    
    echo "✅ All required environment variables are set"
}

# Main function
main() {
    local affected_modules="$1"
    local dry_run="${2:-false}"
    
    echo "📤 Publishing affected modules to Maven staging..."
    echo "Affected modules: $affected_modules"
    echo "Dry run: $dry_run"
    echo ""
    
    if [ -z "$affected_modules" ]; then
        echo "No modules affected, skipping Maven publishing"
        return 0
    fi
    
    if [ "$dry_run" = "true" ]; then
        echo "Dry run mode: skipping Maven publishing"
        return 0
    fi
    
    # Validate environment variables
    validate_environment
    
    # Parse modules into array
    local modules_array
    if ! modules_array=($(parse_modules "$affected_modules")); then
        echo "Failed to parse modules"
        return 1
    fi
    
    echo "Will publish: ${modules_array[*]}"
    echo ""
    
    # Publish each affected module to staging
    local published_modules=""
    for module in "${modules_array[@]}"; do
        # Skip app module
        if [[ "$module" != ":app" ]]; then
            publish_module "$module"
            
            # Track published modules
            if [ -z "$published_modules" ]; then
                published_modules="$module"
            else
                published_modules="$published_modules,$module"
            fi
        else
            echo "Skipping app module: $module"
        fi
    done
    
    echo ""
    if [ -n "$published_modules" ]; then
        echo "Published modules: $published_modules"
        echo "✅ Maven staging deployment completed successfully"
    else
        echo "No library modules were published"
    fi
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
