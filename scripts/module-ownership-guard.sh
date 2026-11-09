#!/usr/bin/env bash
#
# module-ownership-guard.sh : protect the repository layout and module ownership.
#
# Configuration: .github/module-owners.conf (always read from the BASE branch,
# so a PR cannot grant itself new rights).
#
# Check 1, repository layout (EVERY author, admins included):
#   - only the entries listed as "root" may exist at the root of the repository;
#   - docs/ holds general project documentation only: the files listed as
#     "doc", directly under docs/, no sub-folder, no per-module document.
#     Module documentation lives inside the module folders.
#   Only entries ADDED by the PR are reported: existing debt on the base never
#   blocks an unrelated PR.
#
# Check 2, module ownership (every author except admins):
#   - the PR must come from feature/<module>/init, <module> owned by the author;
#   - every changed path (added, modified, deleted, renamed) must belong to
#     that module:
#        backend/src/main/java/com/infineonbit/sustainablefarm/modules/<module>/
#        backend/src/test/java/com/infineonbit/sustainablefarm/modules/<module>/
#        backend/src/main/resources/<module>/
#        backend/src/test/resources/<module>/
#        backend/src/main/resources/seed/<module>-*  or  <module>_*
#        frontend/src/features/<module>/
#      plus NEW Flyway migrations named V<n>__<module>_<description>.sql
#      (existing migrations are never edited: Flyway checksums);
#   - everything else (docs, scripts, .github, core/, shared/, pom.xml,
#     package.json, docker files, root files) is reserved to admins.
#
# Usage:
#   scripts/module-ownership-guard.sh <author-login> <head-branch> [head_ref] [base_ref] [report_path]
#
#   author-login  GitHub login of the PR author
#   head-branch   Name of the PR source branch (feature/<module>/init)
#   head_ref      Commit or ref holding the PR changes (default: HEAD)
#   base_ref      Integration base (default: origin/develop)
#   report_path   Markdown report (default: ownership-report.md)
#
# Exit codes: 0 = allowed, 1 = violation, 3 = usage or configuration error.

set -uo pipefail

AUTHOR="${1:-}"
HEAD_BRANCH="${2:-}"
HEAD_REF="${3:-HEAD}"
BASE_REF="${4:-origin/develop}"
REPORT_PATH="${5:-ownership-report.md}"

if [ -z "$AUTHOR" ] || [ -z "$HEAD_BRANCH" ]; then
    echo "Usage: $0 <author-login> <head-branch> [head_ref] [base_ref] [report_path]" >&2
    exit 3
fi

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || { echo "Error: not a git repository." >&2; exit 3; }
cd "$REPO_ROOT" || exit 3

CONFIG_PATH=".github/module-owners.conf"
CONFIG=$(git show "$BASE_REF:$CONFIG_PATH" 2>/dev/null || cat "$CONFIG_PATH" 2>/dev/null) || {
    echo "Error: $CONFIG_PATH not found on $BASE_REF." >&2
    exit 3
}

MERGE_BASE=$(git merge-base "$BASE_REF" "$HEAD_REF" 2>/dev/null) || {
    echo "Error: no common ancestor between $BASE_REF and $HEAD_REF." >&2
    exit 3
}

lower() { printf '%s' "$1" | tr '[:upper:]' '[:lower:]'; }
AUTHOR_LC=$(lower "$AUTHOR")

ADMINS=()
OWNED=()
ROOT_ALLOWED=()
DOC_ALLOWED=()
while read -r key value _; do
    case "$key" in ''|\#*) continue ;; esac
    [ -z "${value:-}" ] && continue
    case "$key" in
        admin) ADMINS+=("$(lower "$value")") ;;
        root)  ROOT_ALLOWED+=("$value") ;;
        doc)   DOC_ALLOWED+=("$value") ;;
        *)
            [ "$(lower "$value")" = "$AUTHOR_LC" ] && OWNED+=("$key")
            ;;
    esac
done <<< "$CONFIG"

in_list() { # <needle> <items...>
    local needle="$1"; shift
    local item
    for item in "$@"; do [ "$item" = "$needle" ] && return 0; done
    return 1
}

VIOLATIONS=()
MODULE=""
IS_ADMIN=0
for a in "${ADMINS[@]:-}"; do
    [ -n "$a" ] && [ "$a" = "$AUTHOR_LC" ] && IS_ADMIN=1
done

# ---------------------------------------------------------------------------
# Check 1: repository layout (everyone, admins included)
# ---------------------------------------------------------------------------
in_base() { git cat-file -e "$MERGE_BASE:$1" 2>/dev/null; }

while IFS= read -r entry; do
    [ -z "$entry" ] && continue
    in_list "$entry" "${ROOT_ALLOWED[@]:-}" && continue
    in_base "$entry" && continue
    VIOLATIONS+=("\`$entry\` - new entry at the root of the repository. The global layout is fixed: module code goes in its module folders, anything else must be added to the \`root\` list of \`$CONFIG_PATH\` by an admin first.")
done < <(git ls-tree --name-only "$HEAD_REF")

while IFS= read -r doc; do
    [ -z "$doc" ] && continue
    name="${doc#docs/}"
    in_list "$name" "${DOC_ALLOWED[@]:-}" && continue
    in_base "$doc" && continue
    VIOLATIONS+=("\`$doc\` - \`docs/\` is the general project documentation, not a per-module documentation. Put module documentation in the module folder (for example \`modules/<module>/README.md\`); a new general document must be added to the \`doc\` list of \`$CONFIG_PATH\` by an admin first.")
done < <(git ls-tree -r --name-only "$HEAD_REF" -- docs/)

# ---------------------------------------------------------------------------
# Check 2: module ownership (every author except admins)
# ---------------------------------------------------------------------------
if [ "$IS_ADMIN" -eq 0 ]; then
    if [ "${#OWNED[@]}" -eq 0 ]; then
        VIOLATIONS+=("\`$AUTHOR\` owns no module in \`$CONFIG_PATH\`: only module owners and admins may open PRs.")
    else
        # Hyphens are ignored when matching (product-transformation = producttransformation).
        BRANCH_MODULE=""
        if [[ "$HEAD_BRANCH" =~ ^feature/([a-z0-9-]+)/init$ ]]; then
            BRANCH_MODULE="${BASH_REMATCH[1]//-/}"
        fi
        for m in "${OWNED[@]}"; do
            [ "$m" = "$BRANCH_MODULE" ] && MODULE="$m"
        done
        if [ -z "$MODULE" ]; then
            VIOLATIONS+=("Branch \`$HEAD_BRANCH\` is not the \`feature/<module>/init\` branch of a module owned by \`$AUTHOR\` (owned: \`${OWNED[*]}\`).")
        fi
    fi
fi

path_allowed() { # <status> <path>
    local status="$1" p="$2" M="$MODULE"
    local PKG="com/infineonbit/sustainablefarm/modules/$M"
    case "$p" in
        "backend/src/main/java/$PKG/"*|"backend/src/test/java/$PKG/"*) return 0 ;;
        "backend/src/main/resources/$M/"*|"backend/src/test/resources/$M/"*) return 0 ;;
        "backend/src/main/resources/seed/$M-"*|"backend/src/main/resources/seed/${M}_"*) return 0 ;;
        "frontend/src/features/$M/"*) return 0 ;;
    esac
    if [ "$status" = "A" ] && [[ "$p" =~ ^backend/src/main/resources/db/migration/V[0-9_]+__${M}_[a-z0-9_]+\.sql$ ]]; then
        return 0
    fi
    return 1
}

if [ "$IS_ADMIN" -eq 0 ] && [ -n "$MODULE" ]; then
    # --no-renames: a rename is checked as a deletion plus an addition.
    while IFS=$'\t' read -r status path; do
        [ -z "${path:-}" ] && continue
        status="${status:0:1}"
        path_allowed "$status" "$path" && continue
        case "$status" in
            A) what="added" ;;
            D) what="deleted" ;;
            *) what="modified" ;;
        esac
        case "$path" in
            backend/src/main/resources/db/migration/*)
                VIOLATIONS+=("\`$path\` ($what) - only NEW migrations named \`V<n>__${MODULE}_<description>.sql\` are allowed; existing migrations are never edited.") ;;
            docs/*)
                VIOLATIONS+=("\`$path\` ($what) - \`docs/\` is the general project documentation, maintained by the admin. Module documentation goes in the \`$MODULE\` module folder.") ;;
            *)
                VIOLATIONS+=("\`$path\` ($what) - outside the \`$MODULE\` module folders.") ;;
        esac
    done < <(git diff --no-renames --name-status "$MERGE_BASE" "$HEAD_REF")
fi

# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
if [ "$IS_ADMIN" -eq 1 ]; then
    ROLE="admin"
else
    ROLE="module \`${MODULE:-none}\`"
fi

{
    echo "<!-- farm-module-ownership -->"
    echo "### Layout and module ownership (\`module-ownership-guard.sh\`)"
    echo
    echo "Author: \`$AUTHOR\` ($ROLE) - branch: \`$HEAD_BRANCH\`"
    echo
    if [ "${#VIOLATIONS[@]}" -eq 0 ]; then
        echo "Repository layout respected; every changed path is allowed for this author."
    else
        echo "#### Blocking (${#VIOLATIONS[@]})"
        echo
        i=1
        for v in "${VIOLATIONS[@]}"; do printf '%d. %s\n' "$i" "$v"; i=$((i + 1)); done
        echo
        echo "Each contributor works only inside their own module folders. The global layout"
        echo "(root folders, docs, scripts, CI, shared code, build files) is managed by the"
        echo "project admin."
    fi
} > "$REPORT_PATH"

if [ "${#VIOLATIONS[@]}" -gt 0 ]; then
    echo "module-ownership-guard: ${#VIOLATIONS[@]} violation(s), see $REPORT_PATH." >&2
    exit 1
fi
echo "module-ownership-guard: allowed ($ROLE)."
exit 0
