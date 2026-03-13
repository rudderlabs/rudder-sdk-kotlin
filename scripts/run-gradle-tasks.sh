#!/usr/bin/env bash
# Builds a Gradle task list from module:task pairs and runs them in parallel.
#
# Usage: run-gradle-tasks.sh <modules_csv> <task_suffix> [<modules_csv> <task_suffix> ...]
#
# Examples:
#   run-gradle-tasks.sh ":core,:android" "detekt"
#   run-gradle-tasks.sh ":core,:android" "assembleRelease" ":jvm-core" "assemble"

set -euo pipefail

if [[ $# -lt 2 ]] || [[ $(($# % 2)) -ne 0 ]]; then
  echo "Usage: $0 <modules_csv> <task_suffix> [<modules_csv> <task_suffix> ...]"
  exit 1
fi

TASKS=""
while [[ $# -ge 2 ]]; do
  MODULES_CSV="$1"
  TASK_SUFFIX="$2"
  shift 2

  IFS=',' read -ra MODS <<< "$MODULES_CSV"
  for mod in "${MODS[@]}"; do
    [ -n "$mod" ] && TASKS="$TASKS ${mod}:${TASK_SUFFIX}"
  done
done

TASKS=$(echo "$TASKS" | sed 's/^ //')

if [ -z "$TASKS" ]; then
  echo "No tasks to run"
  exit 0
fi

echo "Running: ./gradlew $TASKS --parallel"
./gradlew $TASKS --parallel
