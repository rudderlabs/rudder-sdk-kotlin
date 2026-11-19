#!/usr/bin/env bash
# Offline test: create-tags.sh creates no tag when Gradle fails, and the right tags when it works.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CREATE_TAGS="${SCRIPT_DIR}/../create-tags.sh"

workdir=$(mktemp -d)
trap 'rm -rf "$workdir"' EXIT
cd "$workdir"

# Isolate from user/system git config (hooks, tag signing, etc.)
export GIT_CONFIG_GLOBAL=/dev/null
export GIT_CONFIG_SYSTEM=/dev/null

git init -q
git -c user.email=test@test -c user.name=test commit -q --allow-empty -m "init"
echo "SDK_VERSION=2.0.0" > gradle.properties

# Stub gh: no tag exists yet, and each tag creation is recorded
mkdir bin
cat > bin/gh <<'EOF'
#!/usr/bin/env bash
for arg in "$@"; do
    [[ "$arg" == ref=refs/tags/* ]] && echo "${arg#ref=refs/tags/}" >> "$CREATED_TAGS"
done
[[ "$2" == */git/ref/tags/* ]] && exit 1
exit 0
EOF
chmod +x bin/gh
export PATH="$workdir/bin:$PATH"
export GITHUB_REPOSITORY="test/repo"
export CREATED_TAGS="$workdir/created-tags.txt"

affected="android|bumped|1.7.0|1.7.1"
failed=0

# --- Gradle fails, as it does on an unsupported Java version ---

cat > gradlew <<'EOF'
#!/usr/bin/env bash
echo "Unsupported class file major version 69" >&2
exit 1
EOF
chmod +x gradlew
: > "$CREATED_TAGS"

if echo "$affected" | bash "$CREATE_TAGS" - >/dev/null 2>&1; then
    echo "FAIL: create-tags.sh succeeded although Gradle failed"
    failed=1
elif [[ -s "$CREATED_TAGS" ]]; then
    echo "FAIL: create-tags.sh created tags although Gradle failed:"
    cat "$CREATED_TAGS"
    failed=1
else
    echo "PASS: a failed Gradle call stops create-tags.sh before it creates a tag"
fi

# --- Gradle works ---

cat > gradlew <<'EOF'
#!/usr/bin/env bash
echo "name|group|artifact|version|bump|deps"
echo "android|com.test.sdk|android|1.7.1|patch|"
EOF
: > "$CREATED_TAGS"

echo "$affected" | bash "$CREATE_TAGS" - >/dev/null
actual=$(cat "$CREATED_TAGS")
expected="com.test.sdk.android@v1.7.1
v2.0.0"

if [[ "$actual" == "$expected" ]]; then
    echo "PASS: create-tags.sh creates the module tag and the monorepo tag"
else
    echo "FAIL: create-tags.sh tag mismatch"
    echo "--- expected ---"; echo "$expected"
    echo "--- actual ---";   echo "$actual"
    failed=1
fi

exit "$failed"
