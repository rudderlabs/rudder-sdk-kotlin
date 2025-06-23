#!/bin/bash

# Script to send release-specific Slack notifications
# Usage: ./scripts/send-release-notification.sh "version" "build_result" "release_result" [affected_modules] [built_modules] [release_url]
# 
# This script:
# 1. Determines release status based on key job results
# 2. Creates release-specific notification message
# 3. Uses existing send-slack-notification.sh script

set -e

# Function to determine release status
determine_release_status() {
    local build_result="$1"
    local release_result="$2"
    
    if [ "$build_result" = "success" ] && [ "$release_result" = "success" ]; then
        echo "passed"
    else
        echo "failed"
    fi
}

# Function to create release message
create_release_message() {
    local version="$1"
    local status="$2"
    
    if [ "$status" = "passed" ]; then
        echo "✅ Android SDK v$version released successfully!"
    else
        echo "❌ Android SDK v$version release failed!"
    fi
}

# Function to get module version information
get_module_versions() {
    local affected_modules="$1"
    local built_modules="$2"
    
    if [ -z "$built_modules" ]; then
        echo ""
        return
    fi
    
    local module_info=""
    
    # Get Core SDK version if core or android modules were built
    if [[ "$built_modules" =~ ":core" ]] || [[ "$built_modules" =~ ":android" ]]; then
        local core_version core_version_code
        core_version=$(grep "const val VERSION_NAME" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | sed 's/.*"\([^"]*\)".*/\1/' 2>/dev/null || echo "unknown")
        core_version_code=$(grep "const val VERSION_CODE" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | sed 's/.*"\([0-9]*\)".*/\1/' 2>/dev/null || echo "unknown")
        
        module_info="📦 Core SDK: v$core_version (build $core_version_code)"
    fi
    
    # Get integration module versions
    if [[ "$built_modules" =~ ":integrations:" ]]; then
        local integration_info=""
        
        # Parse built modules to find integrations
        IFS=',' read -ra MODULES <<< "$built_modules"
        for module in "${MODULES[@]}"; do
            if [[ "$module" =~ ^:integrations: ]]; then
                local integration_name
                integration_name=$(echo "$module" | sed 's/:integrations://')
                
                # Get integration version (case-insensitive search)
                local int_version int_version_code
                int_version=$(grep -i -A 10 "object ${integration_name^} : IntegrationModuleInfo" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | grep "override val versionName: String" | sed 's/.*"\([^"]*\)".*/\1/' 2>/dev/null || echo "unknown")
                int_version_code=$(grep -i -A 10 "object ${integration_name^} : IntegrationModuleInfo" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | grep "override val versionCode: String" | sed 's/.*"\([0-9]*\)".*/\1/' 2>/dev/null || echo "unknown")
                
                if [ "$int_version" != "unknown" ]; then
                    if [ -z "$integration_info" ]; then
                        integration_info="📱 ${integration_name^}: v$int_version (build $int_version_code)"
                    else
                        integration_info="$integration_info\n📱 ${integration_name^}: v$int_version (build $int_version_code)"
                    fi
                fi
            fi
        done
        
        # Combine module info
        if [ -n "$integration_info" ]; then
            if [ -n "$module_info" ]; then
                module_info="$module_info\n$integration_info"
            else
                module_info="$integration_info"
            fi
        fi
    fi
    
    echo -e "$module_info"
}

# Function to validate environment
validate_environment() {
    if [ -z "$SLACK_WEBHOOK_URL" ]; then
        echo "⚠️  SLACK_WEBHOOK_URL not set, skipping Slack notification"
        return 1
    fi
    
    if ! command -v ./scripts/send-slack-notification.sh &> /dev/null; then
        echo "❌ send-slack-notification.sh script not found"
        return 1
    fi
    
    echo "✅ Environment validation passed"
}

# Main function
main() {
    local version="$1"
    local build_result="$2"
    local release_result="$3"
    local affected_modules="${4:-}"
    local built_modules="${5:-}"
    local release_url="${6:-}"
    
    if [ -z "$version" ] || [ -z "$build_result" ] || [ -z "$release_result" ]; then
        echo "❌ Missing required parameters"
        echo "Usage: $0 <version> <build_result> <release_result> [affected_modules] [built_modules] [release_url]"
        return 1
    fi
    
    echo "📢 Sending release notification..."
    echo "Version: $version"
    echo "Build result: $build_result"
    echo "Release result: $release_result"
    echo "Affected modules: $affected_modules"
    echo "Built modules: $built_modules"
    echo "Release URL: $release_url"
    echo ""
    
    # Validate environment
    if ! validate_environment; then
        echo "Skipping notification due to environment issues"
        return 0
    fi
    
    # Determine release status
    local status
    status=$(determine_release_status "$build_result" "$release_result")
    echo "Determined status: $status"
    
    # Create release message
    local release_message
    release_message=$(create_release_message "$version" "$status")
    echo "Release message: $release_message"
    
    # Get module version information
    local module_versions
    module_versions=$(get_module_versions "$affected_modules" "$built_modules")
    if [ -n "$module_versions" ]; then
        echo "Module versions:"
        echo -e "$module_versions"
    fi
    
    # Set environment variables for the notification script
    export GITHUB_REPOSITORY="${GITHUB_REPOSITORY:-unknown/unknown}"
    export GITHUB_REF_NAME="${GITHUB_REF_NAME:-unknown}"
    export GITHUB_EVENT_HEAD_COMMIT_MESSAGE="$release_message"
    export GITHUB_ACTOR="${GITHUB_ACTOR:-unknown}"
    export GITHUB_SHA="${GITHUB_SHA:-unknown}"
    export GITHUB_RUN_ID="${GITHUB_RUN_ID:-unknown}"
    
    # Add release-specific information to the message if available
    if [ -n "$module_versions" ]; then
        export GITHUB_EVENT_HEAD_COMMIT_MESSAGE="$release_message

Released Modules:
$module_versions"
    fi
    
    if [ -n "$release_url" ]; then
        export GITHUB_EVENT_HEAD_COMMIT_MESSAGE="$GITHUB_EVENT_HEAD_COMMIT_MESSAGE

Release: $release_url"
    fi
    
    echo ""
    echo "Calling send-slack-notification.sh..."
    
    # Use existing notification script with branch type
    chmod +x scripts/send-slack-notification.sh
    ./scripts/send-slack-notification.sh "branch" "$status"
    
    echo "✅ Release notification sent successfully"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
