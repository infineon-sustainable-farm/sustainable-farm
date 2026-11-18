#!/usr/bin/env bash
#
# design-check.sh : check the interface code of the folders already migrated to
# the design system (tokens of frontend/src/index.css, shared kit, lucide icons)
# against the rules of @arc/design: no hard-coded visual value, one icon set
# (the brand's, .arc-core/design/brand.json).
#
# The modules not migrated yet are not scanned: they still use the legacy tokens
# of index.css. When a module is migrated, add its folder to MIGRATED_DIRS
# (machinery since T12). T45 replaces the list by frontend/src.
#
# Usage: scripts/design-check.sh [dir...]   (default: MIGRATED_DIRS)

set -euo pipefail

REPO_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$REPO_ROOT"

MIGRATED_DIRS=(
  frontend/src/shared/ui
  frontend/src/app
  frontend/src/features/energysupply
  frontend/src/features/machinery
  frontend/src/features/plants
  frontend/src/features/visitormanagement
  frontend/src/features/watersupply
)

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
