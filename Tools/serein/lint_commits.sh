#!/usr/bin/env bash
# Lints the messages of SereinGram's own commits with commitlint, the same
# way CI does; run it before pushing. Upstream commits arrive through merges,
# so only the first-parent line is checked.
#
# Needs commitlint next to the repository, as CI installs it:
#   npm install --no-save --no-package-lock @commitlint/cli@19.8.1 @commitlint/config-conventional@19.8.1
#
# Usage: Tools/serein/lint_commits.sh [revision range]   (default: @{upstream}..HEAD)
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
default_range='@{upstream}..HEAD'
range="${1:-$default_range}"
# Resolved apart from the loop, so that a bad range fails instead of checking nothing.
commits="$(git rev-list --first-parent --reverse "$range")"
failed=0
for sha in $commits; do
  echo "Checking $(git log -1 --format='%h %s' "$sha")"
  git log -1 --format=%B "$sha" \
    | npx --no-install commitlint --config Tools/serein/commitlint.config.mjs --verbose \
    || failed=1
done
exit "$failed"
