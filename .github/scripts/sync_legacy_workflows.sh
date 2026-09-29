#!/usr/bin/env bash
set -euo pipefail

branch="${1:?usage: sync_legacy_workflows.sh <legacy-branch>}"
current="$(git branch --show-current)"
if [ "$current" != "$branch" ]; then
  echo "Check out $branch first (currently on ${current:-a detached HEAD})" >&2
  exit 1
fi

git checkout main -- .github ':!.github/EDITION'
git status --short
