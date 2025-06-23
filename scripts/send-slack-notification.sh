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
        "text": "*Branch Validation $status $status_emoji*\nRepository: $GITHUB_REPOSITORY\nBranch: $GITHUB_REF_NAME\nCommit: $GITHUB_EVENT_HEAD_COMMIT_MESSAGE\nBy: $GITHUB_ACTOR\n<https://github.com/$GITHUB_REPOSITORY/commit/$GITHUB_SHA|View Commit>"
      }
    }
  ]
}
EOF
}

# Function to generate PR validation payload
generate_pr_payload() {
    local status="$1"
    local built_modules="$2"
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
    
    # Add artifacts section if available
    if [ "$has_artifacts" = "true" ] && [ -n "$built_modules" ]; then
        local artifacts_section
        artifacts_section=$(cat << EOF
,
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "📦 *Generated AARs/JARs:*\nBuilt modules: $built_modules\n<https://github.com/$GITHUB_REPOSITORY/actions/runs/$GITHUB_RUN_ID|Download Library AARs>"
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
    local built_modules="$3"
    local has_artifacts="$4"
    
    echo "📢 Preparing Slack notification..."
    echo "Workflow type: $workflow_type"
    echo "Status: $status"
    echo "Built modules: $built_modules"
    echo "Has artifacts: $has_artifacts"
    echo ""
    
    local payload
    case "$workflow_type" in
        "branch")
            payload=$(generate_branch_payload "$status")
            ;;
        "pr")
            payload=$(generate_pr_payload "$status" "$built_modules" "$has_artifacts")
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
