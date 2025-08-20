#!/bin/bash

# Script to send Slack notifications for CI/CD workflows
# Usage: ./scripts/send-slack-notification.sh [workflow_type] [status] [additional_info]
# 
# This script generates appropriate Slack payloads for different workflow types:
# - branch: Branch validation notifications
# - pr: Pull request validation notifications

set -e

# Function to generate branch validation payload
generate_branch_payload() {
    local status="$1"
    local status_emoji
    
    if [ "$status" = "success" ] || [ "$status" = "passed" ]; then
        status_emoji="✅"
    else
        status_emoji="❌"
    fi
    
    cat << EOF
{
  "text": "Branch Validation $status $status_emoji",
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "*Branch Validation $status $status_emoji*\n*Repository:* $GITHUB_REPOSITORY\n*Branch:* $GITHUB_REF_NAME\n*Commit:* $GITHUB_EVENT_HEAD_COMMIT_MESSAGE\n*By:* $GITHUB_ACTOR\n<https://github.com/$GITHUB_REPOSITORY/commit/$GITHUB_SHA|View Commit>"
      }
    }
  ]
}
EOF
}

# Function to generate PR validation payload
generate_pr_payload() {
    local status="$1"
    local failed_checks="$2"
    local has_artifacts="$3"
    local status_emoji
    
    if [ "$status" = "success" ] || [ "$status" = "passed" ]; then
        status_emoji="✅"
    else
        status_emoji="❌"
    fi
    
    local base_payload
    base_payload=$(cat << EOF
{
  "text": "PR Validation $status $status_emoji",
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "*PR Validation $status $status_emoji*\n*Repository:* $GITHUB_REPOSITORY\n*PR:* #$GITHUB_EVENT_NUMBER - $GITHUB_EVENT_PR_TITLE\n*Branch:* $GITHUB_HEAD_REF → $GITHUB_BASE_REF\n*By:* $GITHUB_ACTOR\n<$GITHUB_EVENT_PR_HTML_URL|View PR>"
      }
    }
EOF
)
    
    # Add failure details section if there are failed checks
    if [ "$status" = "failed" ] && [ -n "$failed_checks" ]; then
        local failure_section
        failure_section=$(cat << EOF
,
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "🚨 *Failed Checks:*\n$failed_checks\n\n💡 *Quick Fixes:*\n• **PR Title**: Use lowercase after colon (e.g., \`chore: fix issue\`)\n• **SonarCloud**: Address code quality/security issues\n• **Detekt/Lint**: Fix code style issues\n• **Tests**: Ensure all unit tests pass\n• **Build**: Check compilation errors\n\n<https://github.com/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID|View Workflow Details>"
      }
    }
EOF
)
        base_payload="${base_payload}${failure_section}"
    fi

    # Add artifacts section if available (for successful builds)
    if [ "$status" = "passed" ] && [ "$has_artifacts" = "true" ] && [ -n "$failed_checks" ]; then
        local artifacts_section
        artifacts_section=$(cat << EOF
,
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "📦 *Generated AARs/JARs:*\nBuilt modules: $failed_checks\n<https://github.com/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID|Download Library AARs>"
      }
    }
EOF
)
        base_payload="${base_payload}${artifacts_section}"
    fi
    
    # Close the blocks array and JSON
    base_payload="${base_payload}
  ]
}"
    
    echo "$base_payload"
}

# Function to send notification via webhook
send_notification() {
    local payload="$1"
    
    if [ -z "$SLACK_WEBHOOK_URL" ]; then
        echo "⚠️  SLACK_WEBHOOK_URL not set, skipping notification"
        return 0
    fi
    
    echo "📤 Sending Slack notification..."
    echo "Payload:"
    echo "$payload"
    
    # Send the notification
    local response
    response=$(curl -s -X POST -H 'Content-type: application/json' \
        --data "$payload" \
        "$SLACK_WEBHOOK_URL")
    
    if [ "$response" = "ok" ]; then
        echo "✅ Slack notification sent successfully"
    else
        echo "❌ Failed to send Slack notification: $response"
        return 1
    fi
}

# Main function
main() {
    local workflow_type="$1"
    local status="$2"
    local failed_checks_or_modules="$3"
    local has_artifacts="$4"

    echo "📢 Preparing Slack notification..."
    echo "Workflow type: $workflow_type"
    echo "Status: $status"
    echo "Failed checks/Built modules: $failed_checks_or_modules"
    echo "Has artifacts: $has_artifacts"
    echo ""

    local payload
    case "$workflow_type" in
        "branch")
            payload=$(generate_branch_payload "$status")
            ;;
        "pr")
            payload=$(generate_pr_payload "$status" "$failed_checks_or_modules" "$has_artifacts")
            ;;
        *)
            echo "❌ Unknown workflow type: $workflow_type"
            echo "Available types: branch, pr"
            return 1
            ;;
    esac
    
    send_notification "$payload"
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
