#!/usr/bin/env bash
# Creates a GitHub PR for a release branch targeting main.
#
# Usage: create-release-pr.sh <release_branch> <version> <ticket_id> <affected_modules_file>

set -euo pipefail

release_branch="${1:?release branch name required}"
version="${2:?monorepo version required}"
ticket_id="${3:?ticket ID required}"
affected_modules_file="${4:?affected modules file path required}"

# --- Build version table ---

version_table="| Module | Old Version | New Version |
|--------|-------------|-------------|
"

while IFS='|' read -r module bump old_ver new_ver; do
    version_table+="| ${module} | ${old_ver} | ${new_ver} |
"
done < "$affected_modules_file"

# --- Create PR ---

body_file=$(mktemp)
cat > "$body_file" <<'TEMPLATE'
:crown: **Automated Release PR**

This pull request was created automatically by the GitHub Actions workflow. It merges the release branch into the `main` branch.

This ensures that the latest release branch changes are incorporated into the `main` branch for production.

### Details
TEMPLATE
{
    echo "- **Monorepo Release Version**: v${version}"
    echo "- **Release Branch**: \`${release_branch}\`"
    echo "- **Related Ticket**: [${ticket_id}](https://linear.app/rudderstack/issue/${ticket_id})"
    echo ""
    echo "### Version updates"
    echo ""
    printf "%s" "$version_table"
    echo ""
    echo "---"
    echo "Please review and merge when ready. :rocket:"
} >> "$body_file"

gh pr create \
    --base main \
    --head "$release_branch" \
    --title "chore(release): merge ${release_branch} into main" \
    --body-file "$body_file"

rm -f "$body_file"
