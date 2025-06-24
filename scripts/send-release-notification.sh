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
        echo "Kotlin SDK v$version released successfully"
    else
        echo "Kotlin SDK v$version release failed"
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

        module_info="📦 *Kotlin SDK*: v$core_version (build $core_version_code)"
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
                local int_version int_version_code integration_name_cap
                integration_name_cap=$(echo "$integration_name" | sed 's/^./\U&/')
                int_version=$(grep -i -A 10 "object $integration_name_cap : IntegrationModuleInfo" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | grep "override val versionName: String" | sed 's/.*"\([^"]*\)".*/\1/' 2>/dev/null || echo "unknown")
                int_version_code=$(grep -i -A 10 "object $integration_name_cap : IntegrationModuleInfo" buildSrc/src/main/kotlin/RudderStackBuildConfig.kt | grep "override val versionCode: String" | sed 's/.*"\([0-9]*\)".*/\1/' 2>/dev/null || echo "unknown")

                if [ "$int_version" != "unknown" ]; then
                    if [ -z "$integration_info" ]; then
                        integration_info="📱 *$integration_name_cap Integration*: v$int_version (build $int_version_code)"
                    else
                        integration_info="$integration_info\n📱 *$integration_name_cap Integration*: v$int_version (build $int_version_code)"
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

    if ! command -v curl &> /dev/null; then
        echo "❌ curl command not found"
        return 1
    fi

    echo "✅ Environment validation passed"
}

# Function to send custom release Slack notification
send_release_slack_notification() {
    local version="$1"
    local status="$2"
    local module_versions="$3"
    local release_url="$4"

    local emoji color title
    if [ "$status" = "passed" ]; then
        emoji="✅"
        color="good"
        title="Kotlin SDK v$version released successfully"
    else
        emoji="❌"
        color="danger"
        title="Kotlin SDK v$version release failed"
    fi

    # Create the Slack payload
    local payload
    payload=$(cat <<EOF
{
  "text": "$emoji $title",
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "*$title* $emoji\n*Repository:* ${GITHUB_REPOSITORY:-unknown}\n*Branch:* ${GITHUB_REF_NAME:-unknown}\n*Triggered by:* ${GITHUB_ACTOR:-unknown}"
      }
    }
EOF
)

    # Add module versions if available
    if [ -n "$module_versions" ]; then
        payload="$payload,
    {
      \"type\": \"section\",
      \"text\": {
        \"type\": \"mrkdwn\",
        \"text\": \"*Released SDKs:*\n$module_versions\"
      }
    }"
    fi

    # Add release URL if available
    if [ -n "$release_url" ]; then
        payload="$payload,
    {
      \"type\": \"section\",
      \"text\": {
        \"type\": \"mrkdwn\",
        \"text\": \"<$release_url|📦 View GitHub Release> | <https://github.com/${GITHUB_REPOSITORY:-unknown}/actions/runs/${GITHUB_RUN_ID:-unknown}|🔗 View Workflow Run>\"
      }
    }"
    else
        payload="$payload,
    {
      \"type\": \"section\",
      \"text\": {
        \"type\": \"mrkdwn\",
        \"text\": \"<https://github.com/${GITHUB_REPOSITORY:-unknown}/actions/runs/${GITHUB_RUN_ID:-unknown}|🔗 View Workflow Run>\"
      }
    }"
    fi

    # Close the payload
    payload="$payload
  ]
}"

    echo "Sending Slack notification..."
    echo "Payload preview: $title"

    # Send to Slack
    local response
    response=$(curl -s -X POST -H 'Content-type: application/json' \
        --data "$payload" \
        "$SLACK_WEBHOOK_URL")

    if [ "$response" = "ok" ]; then
        echo "✅ Slack notification sent successfully"
    else
        echo "⚠️  Slack notification response: $response"
    fi
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

    # Send custom release notification to Slack
    send_release_slack_notification "$version" "$status" "$module_versions" "$release_url"

    echo "✅ Release notification sent successfully"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
