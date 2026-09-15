#!/usr/bin/env bash
# Regression test for read-affected-modules.sh.
# Verifies a leftover pre-release tag on an unchanged module does not
# re-add the module to the publish matrix, while bumped and brand-new
# modules are still detected.
#
# Runs fully offline: throwaway git repo + stub gradlew.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
READ_AFFECTED="${SCRIPT_DIR}/../read-affected-modules.sh"

workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT
cd "$workdir"

# Isolate from user/system git config (hooks, tag signing, etc.)
export GIT_CONFIG_GLOBAL=/dev/null
export GIT_CONFIG_SYSTEM=/dev/null

git init -q
git -c user.email=test@test -c user.name=test commit -q --allow-empty -m "init"

# Stub gradlew: prints the dependency chain the scripts normally get from Gradle
cat > gradlew <<'EOF'
#!/usr/bin/env bash
echo "name|group|artifact|version|bump|deps"
echo "android|com.test.sdk|android|1.7.0|minor|"
echo "braze|com.test.sdk|braze|1.5.0|minor|"
echo "sprig|com.test.sdk|sprig|1.0.0|minor|"
EOF
chmod +x gradlew

# android: unchanged at 1.7.0, with a leftover pre-release tag
git tag "com.test.sdk.android@v1.7.0"
git tag "com.test.sdk.android@v1.7.0-beta.1"
# braze: genuinely bumped 1.4.1 -> 1.5.0
git tag "com.test.sdk.braze@v1.4.1"
# sprig: no tags at all -> new module

actual=$(bash "$READ_AFFECTED")
expected="braze|bumped|1.4.1|1.5.0
sprig|new||1.0.0"

if [[ "$actual" == "$expected" ]]; then
    echo "PASS: unchanged module with leftover pre-release tag is not re-published"
else
    echo "FAIL: read-affected-modules.sh output mismatch"
    echo "--- expected ---"; echo "$expected"
    echo "--- actual ---";   echo "$actual"
    exit 1
fi
