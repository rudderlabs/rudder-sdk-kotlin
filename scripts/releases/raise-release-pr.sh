#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

TEMPLATE_PATH="$SCRIPT_DIR/pr-template.md"

# ---------------------------------------------------------------------------
# extract_ticket_id - extracts Linear ticket ID from branch name
#   e.g. release/1.5.0-SDK-4649 -> SDK-4649
# ---------------------------------------------------------------------------
extract_ticket_id() {
    local branch="$1"
    echo "$branch" | grep -oE '[A-Z]+-[0-9]+' | tail -1
}

# ---------------------------------------------------------------------------
# extract_release_version - extracts version from branch name
#   e.g. release/1.5.0-SDK-4649 -> 1.5.0
# ---------------------------------------------------------------------------
extract_release_version() {
    local branch="$1"
    echo "$branch" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1
}

# ===========================================================================
# Main
# ===========================================================================

DRY_RUN=false
if [[ "${1:-}" == "--dry-run" ]]; then
    DRY_RUN=true
    echo "[DRY RUN MODE] — will preview only, no commit/push/PR"
    echo ""
fi

echo "Step 1: Running changelog generation..."
echo ""
bash "$SCRIPT_DIR/update-changelog.sh"

echo ""
echo "Step 2: Preparing release PR..."

# Gather branch info
CURRENT_BRANCH=$(git branch --show-current)
TICKET_ID=$(extract_ticket_id "$CURRENT_BRANCH")
RELEASE_VERSION=$(extract_release_version "$CURRENT_BRANCH")

if [[ -z "$TICKET_ID" ]]; then
    echo "  Warning: could not extract ticket ID from branch '$CURRENT_BRANCH'" >&2
    TICKET_ID="UNKNOWN"
fi

if [[ -z "$RELEASE_VERSION" ]]; then
    RELEASE_VERSION=$(grep '^SDK_VERSION=' "$REPO_ROOT/$GRADLE_PROPERTIES_PATH" | cut -d'=' -f2)
fi

echo "  Branch: $CURRENT_BRANCH"
echo "  Ticket: $TICKET_ID"
echo "  Release version: $RELEASE_VERSION"

# Build version table from the summary arrays
# Re-read versions after update-changelog.sh has run
echo ""
echo "Step 3: Building version table..."

VERSION_TABLE=""
ALL_MODULES=$(discover_modules)

MONOREPO_VERSION=$(grep '^SDK_VERSION=' "$REPO_ROOT/$GRADLE_PROPERTIES_PATH" | cut -d'=' -f2)
VERSION_TABLE+="| rudder-sdk-kotlin (monorepo) | $MONOREPO_VERSION |"$'\n'

for module in $ALL_MODULES; do
    local_version=$(get_module_version "$module")
    VERSION_TABLE+="| $module | $local_version |"$'\n'
done

# Fill template using sed for simple values, then split-and-rejoin for the table
echo "Step 4: Generating PR body..."

# Read template and replace simple placeholders
PR_BODY=$(sed \
    -e "s|{{RELEASE_BRANCH}}|$CURRENT_BRANCH|g" \
    -e "s|{{MONOREPO_VERSION}}|$MONOREPO_VERSION|g" \
    -e "s|{{TICKET_ID}}|$TICKET_ID|g" \
    "$TEMPLATE_PATH")

# Replace {{VERSION_TABLE}} by splitting on the placeholder and inserting the table
PR_BEFORE="${PR_BODY%%\{\{VERSION_TABLE\}\}*}"
PR_AFTER="${PR_BODY#*\{\{VERSION_TABLE\}\}}"
PR_BODY="${PR_BEFORE}${VERSION_TABLE}${PR_AFTER}"

PR_TITLE="chore(release): merge $CURRENT_BRANCH into main [$TICKET_ID]"

echo ""
echo "============================================"
echo "  PR Preview"
echo "============================================"
echo ""
echo "Title: $PR_TITLE"
echo ""
echo "--- Body ---"
echo "$PR_BODY"
echo "--- End ---"
echo ""

if [[ "$DRY_RUN" == true ]]; then
    echo "[DRY RUN] Skipping: stage, commit, push, PR creation"
    echo "[DRY RUN] Run without --dry-run to execute"
    echo ""
    echo "Resetting file changes..."
    git checkout -- "$REPO_ROOT/$BUILD_CONFIG_PATH" \
        "$REPO_ROOT/$GRADLE_PROPERTIES_PATH"
    for module in $ALL_MODULES; do
        changelog_path="$REPO_ROOT/$module/CHANGELOG.md"
        if [[ -f "$changelog_path" ]]; then
            git checkout -- "$changelog_path"
        fi
    done
    echo "  Reset complete"
    exit 0
fi

# Stage changed files
echo "Step 5: Staging changes..."
git add \
    "$REPO_ROOT/$BUILD_CONFIG_PATH" \
    "$REPO_ROOT/$GRADLE_PROPERTIES_PATH"

for module in $ALL_MODULES; do
    changelog_path="$REPO_ROOT/$module/CHANGELOG.md"
    if [[ -f "$changelog_path" ]]; then
        git add "$changelog_path"
    fi
done

echo "  Files staged"

# Commit
echo ""
echo "Step 6: Creating commit..."
git commit -m "chore(release): v$MONOREPO_VERSION [$TICKET_ID]"
echo "  Commit created"

# Push
echo ""
echo "Step 7: Pushing to remote..."
git push origin "$CURRENT_BRANCH"
echo "  Pushed"

# Create PR
echo ""
echo "Step 8: Creating pull request..."
PR_URL=$(gh pr create \
    --base main \
    --head "$CURRENT_BRANCH" \
    --title "$PR_TITLE" \
    --body "$PR_BODY")

echo ""
echo "============================================"
echo "  Release PR created"
echo "============================================"
echo "  $PR_URL"
echo "============================================"
