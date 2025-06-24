#!/bin/bash

# Test script for release workflow scripts
# Usage: ./test-release-scripts.sh

set -e

echo "🧪 Testing Release Workflow Scripts"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to run test and report result
run_test() {
    local test_name="$1"
    local command="$2"
    
    echo -e "\n${YELLOW}Testing: $test_name${NC}"
    echo "Command: $command"
    
    if eval "$command"; then
        echo -e "${GREEN}✅ PASS: $test_name${NC}"
        return 0
    else
        echo -e "${RED}❌ FAIL: $test_name${NC}"
        return 1
    fi
}

# Function to check script syntax
check_syntax() {
    local script="$1"
    echo -e "\n${YELLOW}Checking syntax: $script${NC}"
    
    if bash -n "$script"; then
        echo -e "${GREEN}✅ Syntax OK: $script${NC}"
        return 0
    else
        echo -e "${RED}❌ Syntax Error: $script${NC}"
        return 1
    fi
}

echo -e "\n📋 Phase 1: Script Syntax Validation"
echo "====================================="

SCRIPTS=(
    "scripts/determine-version.sh"
    "scripts/determine-release-base-branch.sh"
    "scripts/find-affected-modules.sh"
    "scripts/bump-version.sh"
    "scripts/build-aars.sh"
    "scripts/run-release-tests.sh"
    "scripts/publish-to-maven.sh"
    "scripts/create-release.sh"
    "scripts/create-release-prs.sh"
    "scripts/send-release-notification.sh"
    "scripts/run-quality-checks.sh"
    "scripts/send-slack-notification.sh"
)

SYNTAX_ERRORS=0
for script in "${SCRIPTS[@]}"; do
    if [ -f "$script" ]; then
        if ! check_syntax "$script"; then
            ((SYNTAX_ERRORS++))
        fi
    else
        echo -e "${RED}❌ Script not found: $script${NC}"
        ((SYNTAX_ERRORS++))
    fi
done

if [ $SYNTAX_ERRORS -gt 0 ]; then
    echo -e "\n${RED}❌ $SYNTAX_ERRORS syntax errors found. Fix before proceeding.${NC}"
    exit 1
fi

echo -e "\n📋 Phase 2: Individual Script Testing"
echo "====================================="

# Test 1: Version determination
run_test "Version determination (provided version)" \
    "./scripts/determine-version.sh '1.2.3' 'workflow_dispatch' ''"

run_test "Version determination (auto-increment)" \
    "./scripts/determine-version.sh '' 'workflow_dispatch' ''"

run_test "Version determination (release event)" \
    "./scripts/determine-version.sh '' 'release' 'v1.2.3'"

# Test 2: Base branch determination
run_test "Base branch determination (release branch)" \
    "./scripts/determine-release-base-branch.sh 'true'"

run_test "Base branch determination (manual release)" \
    "./scripts/determine-release-base-branch.sh 'false'"

# Test 3: Find affected modules
run_test "Find affected modules" \
    "./scripts/find-affected-modules.sh 'develop'"

# Test 4: Version bumping (dry run)
run_test "Version bump (dry run)" \
    "./scripts/bump-version.sh '1.2.3-test' ':core,:android' 'true'"

# Test 5: Release tests
run_test "Release tests" \
    "./scripts/run-release-tests.sh ':core'"

# Test 6: Release notification (without Slack)
run_test "Release notification (dry run)" \
    "SLACK_WEBHOOK_URL='' ./scripts/send-release-notification.sh '1.0.0-test' 'success' 'success' ':core' ':core' 'https://github.com/test'"

echo -e "\n📋 Phase 3: Integration Testing"
echo "==============================="

# Test script permissions
echo -e "\n${YELLOW}Checking script permissions...${NC}"
for script in "${SCRIPTS[@]}"; do
    if [ -f "$script" ]; then
        if [ -x "$script" ]; then
            echo -e "${GREEN}✅ Executable: $script${NC}"
        else
            echo -e "${YELLOW}⚠️  Not executable: $script${NC}"
            chmod +x "$script"
            echo -e "${GREEN}✅ Fixed permissions: $script${NC}"
        fi
    fi
done

# Test GitHub Actions output format
echo -e "\n${YELLOW}Testing GitHub Actions output format...${NC}"
export GITHUB_OUTPUT="/tmp/test-github-output"
touch "$GITHUB_OUTPUT"

./scripts/determine-version.sh "1.2.3" "workflow_dispatch" ""

if [ -s "$GITHUB_OUTPUT" ]; then
    echo -e "${GREEN}✅ GitHub Actions output generated:${NC}"
    cat "$GITHUB_OUTPUT"
else
    echo -e "${RED}❌ No GitHub Actions output generated${NC}"
fi

rm -f "$GITHUB_OUTPUT"

echo -e "\n📋 Test Summary"
echo "==============="
echo -e "${GREEN}✅ All basic script tests completed!${NC}"
echo ""
echo "Next steps:"
echo "1. Create a test release branch: git checkout -b release/test-1.0.0"
echo "2. Push the branch: git push origin release/test-1.0.0"
echo "3. Run the workflow manually with dry_run=true"
echo "4. Check the workflow logs for any issues"
echo ""
echo "For full testing, you can also:"
echo "- Test with actual module changes"
echo "- Test Slack notifications (set SLACK_WEBHOOK_URL)"
echo "- Test Maven publishing (set required secrets)"
echo "- Test GitHub release creation (set GITHUB_TOKEN)"
