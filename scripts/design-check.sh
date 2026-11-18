#!/usr/bin/env bash
#
# design-check.sh : check the interface code of the folders already migrated to
# the design system (tokens of frontend/src/index.css, shared kit, lucide icons)
# against the rules of @arc/design: no hard-coded visual value, one icon set
# (the brand's, .arc-core/design/brand.json).
#
# Every folder of frontend/src is scanned except the two excluded modules, not migrated, which
# still use their own tokens (sitesecurity block of index.css) and libraries.
#
# Usage: scripts/design-check.sh [dir...]   (default: every folder of frontend/src but EXCLUDED_DIRS)

set -euo pipefail

REPO_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$REPO_ROOT"

EXCLUDED_DIRS=(
  frontend/src/features/sitesecurity
  frontend/src/features/cropstorage
)

MIGRATED_DIRS=()
for dir in frontend/src/*/ frontend/src/features/*/; do
  dir=${dir%/}
  [ "$dir" = frontend/src/features ] && continue
  case " ${EXCLUDED_DIRS[*]} " in *" $dir "*) continue ;; esac
  MIGRATED_DIRS+=("$dir")
done

if [ "$#" -gt 0 ]; then
  MIGRATED_DIRS=("$@")
fi

status=0
for dir in "${MIGRATED_DIRS[@]}"; do
  if [ ! -d "$dir" ]; then
    echo "design-check: $dir does not exist yet, skipped"
    continue
  fi
  echo "design-check: $dir"
  arc-design check code "$dir" || status=1
  arc-design check icons "$dir" --brand-dir .arc-core/design || status=1
done

exit "$status"
