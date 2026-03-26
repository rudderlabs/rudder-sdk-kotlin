#!/usr/bin/env bash
# Creates a back-merge PR from main into develop after a release.
#
# Usage: create-backmerge-pr.sh <version>

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

version="${1:?monorepo version required}"

# --- Check for existing PR ---

existing_pr=$(gh pr list --base develop --head main --state open --json number -q '.[0].number')

if [[ -n "${existing_pr}" ]]; then
  echo "Back-merge PR already exists: #${existing_pr}" >&2
  echo "https://github.com/${GITHUB_REPOSITORY}/pull/${existing_pr}"
  exit 0
fi

# --- Create back-merge PR ---

body_file=$(mktemp)
cat > "$body_file" <<TEMPLATE
:crown: **Automated Post-Release PR**

This pull request was created automatically by the GitHub Actions workflow. It merges changes from the \`main\` branch into the \`develop\` branch after a release has been completed.

This ensures that the \`develop\` branch stays up to date with all release-related changes from the \`main\` branch.

### Details
- **Monorepo Release Version**: v${version}

---
Please review and merge it before closing the release ticket. :rocket:
TEMPLATE

gh pr create \
  --base develop \
  --head main \
  --title "chore(release): merge main into develop" \
  --body-file "$body_file"

rm -f "$body_file"
