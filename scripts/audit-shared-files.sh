#!/usr/bin/env bash
#
# audit-shared-files.sh : audit a branch's shared files before merging.
#
# Compares the shared files (frontend app/shared, backend, infra) between a
# branch under review and the integration branch (origin/develop by default),
# simulates the merge to detect real conflicts, and enforces the
# "module-local endpoints" convention.
#
# Usage:
#   scripts/audit-shared-files.sh [branch] [base_ref] [--no-fetch] [--cross-check]
#
#   branch         Branch to audit (default: current branch). May be a
#                  name, a remote ref (origin/xxx), a tag or a SHA.
#   base_ref       Integration branch (default: origin/develop).
#   --no-fetch     Skip the git fetch before auditing.
#   --cross-check  Also simulate merges with the other origin/feature/*
#                  branches and report cross-conflicts.
#
# Exit codes:
#   0 = no conflict or risk detected
#   1 = real merge conflict, key file diverged on both sides, or violation
#       of the module-local endpoints convention
#   2 = warning(s): shared files touched by the branch, lag, cross-conflicts
#       with other feature branches
#
# Examples:
#   scripts/audit-shared-files.sh
#   scripts/audit-shared-files.sh feature/plants/english-identifiers
#   scripts/audit-shared-files.sh feature/machinery/init origin/develop --no-fetch
#   scripts/audit-shared-files.sh feature/plants/init --cross-check

set -uo pipefail

if [ -t 1 ]; then
    C_RED=$'\033[31m'; C_GRN=$'\033[32m'; C_YEL=$'\033[33m'
    C_BLU=$'\033[36m'; C_BLD=$'\033[1m';  C_OFF=$'\033[0m'
else
    C_RED=''; C_GRN=''; C_YEL=''; C_BLU=''; C_BLD=''; C_OFF=''
fi

info()  { printf '%s\n' "$*"; }
title() { printf '\n%s==== %s ====%s\n' "$C_BLD$C_BLU" "$*" "$C_OFF"; }
ok()    { printf '%s%s%s\n' "$C_GRN" "$*" "$C_OFF"; }
warn()  { printf '%s%s%s\n' "$C_YEL" "$*" "$C_OFF"; }
bad()   { printf '%s%s%s\n' "$C_RED" "$*" "$C_OFF"; }

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || {
    echo "Error: run this script from a git repository." >&2
    exit 1
}
cd "$REPO_ROOT" || exit 1

# ---------------------------------------------------------------------------
# Key shared files to check in priority (extend freely).
# ---------------------------------------------------------------------------
KEY_FILES=(
    # Frontend: application and shared
    frontend/src/index.css
    frontend/src/main.jsx
    frontend/src/app/router.jsx
    frontend/src/app/RootLayout.jsx
    frontend/src/app/QueryProvider.jsx
    frontend/src/shared/api/client.js
    frontend/src/shared/api/endpoints.js
    frontend/src/shared/api/queryClient.js
    frontend/src/shared/utils/formatEnumLabel.js
    frontend/src/shared/components/EmptyState.jsx
    frontend/src/shared/components/Modal.jsx
    frontend/src/shared/components/Pagination.jsx
    frontend/package.json
    frontend/vite.config.js
    # Backend: configuration and build
    backend/pom.xml
    backend/src/main/resources/application.properties
    backend/src/test/resources/application-test.properties
    backend/Dockerfile
    # Infra / root
    docker-compose.yml
    .env.example
    .github/workflows/ci.yml
    .gitignore
)

# Shared endpoints file, target of the module-local convention.
ENDPOINTS_SHARED="frontend/src/shared/api/endpoints.js"

# ---------------------------------------------------------------------------
# Arguments
# ---------------------------------------------------------------------------
BRANCH_ARG=""
BASE_REF_ARG=""
NO_FETCH=0
CROSS_CHECK=0

for arg in "$@"; do
    case "$arg" in
        --no-fetch) NO_FETCH=1 ;;
        --cross-check) CROSS_CHECK=1 ;;
        -h|--help)
            awk 'NR>1 && /^#/ { sub(/^# ?/, ""); print; next } NR>1 { exit }' "$0"
            exit 0
            ;;
        *)
            if [ -z "$BRANCH_ARG" ]; then
                BRANCH_ARG="$arg"
            elif [ -z "$BASE_REF_ARG" ]; then
                BASE_REF_ARG="$arg"
            else
                echo "Unknown argument: $arg" >&2
                exit 1
            fi
            ;;
    esac
done

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
[ -z "$BRANCH_ARG" ] && BRANCH_ARG="$CURRENT_BRANCH"
[ -z "$BASE_REF_ARG" ] && BASE_REF_ARG="origin/develop"

# ---------------------------------------------------------------------------
# Ref resolution
# ---------------------------------------------------------------------------
resolve_ref() {
    local r="$1"
    if git rev-parse --verify --quiet "refs/remotes/origin/$r^{commit}" >/dev/null; then
        echo "origin/$r"; return 0
    fi
    if git rev-parse --verify --quiet "refs/heads/$r^{commit}" >/dev/null; then
        echo "$r"; return 0
    fi
    if git rev-parse --verify --quiet "$r^{commit}" >/dev/null; then
        echo "$r"; return 0
    fi
    return 1
}

if [ "$NO_FETCH" -eq 0 ]; then
    if git remote get-url origin >/dev/null 2>&1; then
        info "Fetching origin (git fetch --prune)..."
        if ! git fetch origin --prune --quiet; then
            warn "git fetch failed: auditing local refs."
        fi
    else
        warn "No 'origin' remote: auditing local refs."
    fi
fi

BRANCH_REF=$(resolve_ref "$BRANCH_ARG") || {
    echo "Error: branch not found: $BRANCH_ARG" >&2
    exit 1
}
BASE_REF=$(resolve_ref "$BASE_REF_ARG") || {
    echo "Error: base branch not found: $BASE_REF_ARG" >&2
    exit 1
}

MERGE_BASE=$(git merge-base "$BASE_REF" "$BRANCH_REF" 2>/dev/null) || {
    echo "Error: no common ancestor between $BASE_REF and $BRANCH_REF." >&2
    exit 1
}

# Module deduced from the branch name (feature/<module>/...), used by the
# convention checks and the shared-surface check.
MODULE=$(printf '%s' "$BRANCH_ARG" | sed -nE 's#^(feature|fix|chore|refactor|hotfix|release)/([^/]+)/.*#\2#p')

title "Context"
info "Audited branch: ${C_BLD}${BRANCH_REF}${C_OFF} ($(git rev-parse --short "$BRANCH_REF"))"
info "Integration base: ${C_BLD}${BASE_REF}${C_OFF} ($(git rev-parse --short "$BASE_REF"))"
info "Merge-base: $(git rev-parse --short "$MERGE_BASE")"
[ -n "$MODULE" ] && info "Module deduced from branch name: ${C_BLD}${MODULE}${C_OFF}"
if git merge-base --is-ancestor "$BASE_REF" "$BRANCH_REF"; then
    ok "The branch already contains the whole base: no merge needed."
fi
info "Commits absent from the branch: $(git rev-list --count "$MERGE_BASE..$BRANCH_REF") branch commit(s) / $(git rev-list --count "$MERGE_BASE..$BASE_REF") base commit(s)"

# ---------------------------------------------------------------------------
# 1. Merge simulation (real conflicts)
# ---------------------------------------------------------------------------
REAL_CONFLICT=0
CONFLICTED=""

title "1. Merge simulation $BASE_REF + $BRANCH_REF"
MERGE_TREE_HELP=$(git merge-tree -h 2>&1 || true)
case "$MERGE_TREE_HELP" in
    *--write-tree*) MERGE_TREE_SUPPORTED=1 ;;
    *) MERGE_TREE_SUPPORTED=0 ;;
esac

if [ "$MERGE_TREE_SUPPORTED" -eq 1 ]; then
    MERGE_OUT=$(git merge-tree --write-tree --name-only "$BASE_REF" "$BRANCH_REF" 2>&1)
    MERGE_RC=$?
    if [ "$MERGE_RC" -eq 0 ]; then
        ok "No merge conflict detected."
    else
        REAL_CONFLICT=1
        CONFLICTED=$(printf '%s\n' "$MERGE_OUT" | tail -n +2 | sed '/^$/,$d' \
            | grep -vE '^(Auto-merging|CONFLICT|changed in both|added in|removed in)' || true)
        bad "Merge conflict(s) detected:"
        if [ -n "$CONFLICTED" ]; then
            printf '%s\n' "$CONFLICTED" | while IFS= read -r c; do
                printf '  - %s\n' "$c"
            done
        fi
    fi
else
    warn "git merge-tree --write-tree not supported (git < 2.38): simulation skipped."
fi

# ---------------------------------------------------------------------------
# 2. Key files
# ---------------------------------------------------------------------------
exists_at() { git cat-file -e "$1:$2" 2>/dev/null; }
changed_between() { ! git diff --quiet "$1" "$2" -- "$3" 2>/dev/null; }

RISK=0
WARNINGS=0

title "2. Key shared files"

printf '%-56s %s\n' "FILE" "STATUS"
printf '%-56s %s\n' "--------------------------------------------------------" "-----------------------------"

for f in "${KEY_FILES[@]}"; do
    eb=0; ed=0; eh=0
    exists_at "$MERGE_BASE" "$f" && eb=1
    exists_at "$BASE_REF" "$f" && ed=1
    exists_at "$BRANCH_REF" "$f" && eh=1

    state=""
    if [ "$eh" -eq 0 ] && [ "$ed" -eq 1 ] && [ "$eb" -eq 1 ]; then
        state="${C_RED}DELETED BY BRANCH (exists on develop) -> CONFLICT${C_OFF}"; RISK=1
    elif [ "$eh" -eq 0 ] && [ "$ed" -eq 1 ]; then
        state="${C_YEL}BEHIND (added on develop, missing from the branch)"
    elif [ "$eh" -eq 0 ]; then
        state="${C_YEL}absent on both sides${C_OFF}"
    elif [ "$ed" -eq 0 ]; then
        state="${C_YEL}BRANCH ADDITION (new, no conflict)"
    elif ! changed_between "$MERGE_BASE" "$BRANCH_REF" "$f"; then
        if changed_between "$MERGE_BASE" "$BASE_REF" "$f"; then
            state="${C_YEL}BEHIND develop (unmodified by the branch)"
        else
            state="${C_GRN}OK (identical to develop)${C_OFF}"
        fi
    elif ! changed_between "$MERGE_BASE" "$BASE_REF" "$f"; then
        state="${C_YEL}MODIFIED BY BRANCH (develop untouched)${C_OFF}"; WARNINGS=$((WARNINGS + 1))
    elif changed_between "$BASE_REF" "$BRANCH_REF" "$f"; then
        state="${C_RED}DIVERGED ON BOTH SIDES -> POTENTIAL CONFLICT${C_OFF}"; RISK=1
    else
        state="${C_GRN}OK (aligned with develop after adoption)${C_OFF}"
    fi
    printf '%-56s %b\n' "$f" "$state"
done

# ---------------------------------------------------------------------------
# 3. Module-local endpoints convention
# ---------------------------------------------------------------------------
# Rule: a module's endpoints must live in
# frontend/src/features/<module>/api/endpoints.js, not in the shared file
# frontend/src/shared/api/endpoints.js. Two modules adding keys to the same
# object produce a merge conflict on every merge.
CONVENTION=0

title "3. Module-local endpoints convention"

mapfile -t FEATURES < <(git ls-tree --name-only "$BRANCH_REF:frontend/src/features" 2>/dev/null || true)

if [ "${#FEATURES[@]}" -gt 0 ] && exists_at "$BRANCH_REF" "$ENDPOINTS_SHARED"; then
    SHARED_CONTENT=$(git show "$BRANCH_REF:$ENDPOINTS_SHARED" 2>/dev/null || true)
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        key=$(printf '%s' "$line" | sed -nE 's/^[[:space:]]*([A-Z][A-Z0-9_]*)[[:space:]]*:.*/\1/p')
        value=$(printf '%s' "$line" | sed -nE 's/^[^"]*"([^"]+)".*/\1/p')
        if [ -z "$key" ] || [ -z "$value" ]; then
            continue
        fi
        for mod in "${FEATURES[@]}"; do
            case "$value" in
                */api/"$mod"/*)
                    bad "  - $ENDPOINTS_SHARED : $key = \"$value\" (module '$mod' endpoint in the shared file)"
                    CONVENTION=1
                    ;;
            esac
        done
    done <<< "$SHARED_CONTENT"
else
    warn "Cannot check: $ENDPOINTS_SHARED or the features folder is missing from $BRANCH_REF."
fi

# Reverse check: a module still importing the shared file.
if [ -n "$MODULE" ]; then
    MODULES_TO_CHECK=("$MODULE")
else
    MODULES_TO_CHECK=("${FEATURES[@]}")
fi

for mod in "${MODULES_TO_CHECK[@]}"; do
    [ -z "$mod" ] && continue
    while IFS= read -r api_file; do
        [ -z "$api_file" ] && continue
        if git show "$BRANCH_REF:$api_file" 2>/dev/null | grep -q 'shared/api/endpoints'; then
            if exists_at "$BRANCH_REF" "frontend/src/features/$mod/api/endpoints.js"; then
                bad "  - $api_file imports shared/api/endpoints while the module-local file exists"
            else
                warn "  - $api_file imports shared/api/endpoints (create frontend/src/features/$mod/api/endpoints.js)"
            fi
            CONVENTION=1
        fi
    done < <(git ls-tree -r --name-only "$BRANCH_REF" -- "frontend/src/features/$mod/api" 2>/dev/null | grep '\.js$' || true)
done

if [ "$CONVENTION" -eq 0 ]; then
    ok "Convention respected: no module endpoint in $ENDPOINTS_SHARED."
fi

# ---------------------------------------------------------------------------
# 4. Shared surface modified by the branch (outside the module)
# ---------------------------------------------------------------------------
title "4. Shared surface modified by the branch (outside the module)"

if [ -n "$MODULE" ]; then
    info "Module detected from branch name: ${C_BLD}${MODULE}${C_OFF}"
    mapfile -t BRANCH_FILES < <(git diff --name-only "$MERGE_BASE" "$BRANCH_REF" | grep -v "/$MODULE/" || true)
else
    warn "Branch name outside the feature/<module>/... convention: listing all changed files."
    mapfile -t BRANCH_FILES < <(git diff --name-only "$MERGE_BASE" "$BRANCH_REF")
fi

SHARED_COUNT=0
SHARED_RISKY=()
SHARED_OTHER=()
for f in "${BRANCH_FILES[@]}"; do
    [ -z "$f" ] && continue
    # Do not repeat key files already covered in section 2.
    for k in "${KEY_FILES[@]}"; do
        if [ "$f" = "$k" ]; then
            f=""
            break
        fi
    done
    [ -z "$f" ] && continue
    SHARED_COUNT=$((SHARED_COUNT + 1))
    if exists_at "$MERGE_BASE" "$f" && changed_between "$MERGE_BASE" "$BASE_REF" "$f"; then
        SHARED_RISKY+=("$f")
        RISK=1
    else
        SHARED_OTHER+=("$f")
    fi
done

MAX_LIST=15
if [ "${#SHARED_RISKY[@]}" -gt 0 ]; then
    bad "Files modified by the branch AND by develop:"
    printf '  - %s\n' "${SHARED_RISKY[@]}"
fi

if [ "${#SHARED_OTHER[@]}" -gt 0 ]; then
    warn "${#SHARED_OTHER[@]} shared file(s) added/modified by the branch (no current conflict):"
    for i in "${!SHARED_OTHER[@]}"; do
        [ "$i" -ge "$MAX_LIST" ] && { warn "  ... and $(( ${#SHARED_OTHER[@]} - MAX_LIST )) more"; break; }
        printf '  - %s\n' "${SHARED_OTHER[$i]}"
    done
    WARNINGS=$((WARNINGS + 1))
fi

if [ "$SHARED_COUNT" -eq 0 ]; then
    ok "No shared file outside the module touched by the branch."
fi

# ---------------------------------------------------------------------------
# 5. Cross-check with the other feature branches (--cross-check)
# ---------------------------------------------------------------------------
if [ "$CROSS_CHECK" -eq 1 ]; then
    title "5. Cross-check with the other feature branches"
    if [ "$MERGE_TREE_SUPPORTED" -eq 1 ]; then
        CROSS_BRANCHES=0
        CROSS_CONFLICTS=0
        SEEN_SHAS=""
        while IFS= read -r other; do
            [ -z "$other" ] && continue
            [ "$other" = "$BRANCH_REF" ] && continue
            # Avoid testing two identical refs (e.g. Feature/ and feature/).
            sha=$(git rev-parse "$other" 2>/dev/null || true)
            case " $SEEN_SHAS " in
                *" $sha "*) continue ;;
            esac
            SEEN_SHAS="$SEEN_SHAS $sha"
            # Skip branches already contained in the base: their content is
            # already in develop, the cross-check adds nothing.
            if git merge-base --is-ancestor "$other" "$BASE_REF" 2>/dev/null; then
                continue
            fi
            CROSS_BRANCHES=$((CROSS_BRANCHES + 1))
            out=$(git merge-tree --write-tree --name-only "$BRANCH_REF" "$other" 2>&1)
            rc=$?
            if [ "$rc" -ne 0 ]; then
                files=$(printf '%s\n' "$out" | tail -n +2 | sed '/^$/,$d' \
                    | grep -vE '^(Auto-merging|CONFLICT|changed in both|added in|removed in)' \
                    | tr '\n' ',' | sed 's/,$//; s/,/, /g')
                warn "  - ${other#origin/} : ${files:-conflict}"
                CROSS_CONFLICTS=$((CROSS_CONFLICTS + 1))
            fi
        done < <(git for-each-ref --format='%(refname:short)' 'refs/remotes/origin/[Ff]eature/**' 2>/dev/null || true)

        if [ "$CROSS_CONFLICTS" -eq 0 ]; then
            ok "No cross-conflict with the $CROSS_BRANCHES remote feature branch(es)."
        else
            warn "$CROSS_CONFLICTS cross-conflict(s) across $CROSS_BRANCHES branch(es): merging into develop will depend on merge order."
            WARNINGS=$((WARNINGS + 1))
        fi
    else
        warn "git merge-tree --write-tree not supported: cross-check skipped."
    fi
fi

# ---------------------------------------------------------------------------
# 6. Final report
# ---------------------------------------------------------------------------
title "6. Report"

if [ "$REAL_CONFLICT" -eq 1 ]; then
    bad "VERDICT: REAL MERGE CONFLICT(S): do not merge as is."
    exit 1
fi

if [ "$RISK" -eq 1 ]; then
    bad "VERDICT: RISK DETECTED: review the flagged files before merging."
    exit 1
fi

if [ "$CONVENTION" -eq 1 ]; then
    bad "VERDICT: MODULE-LOCAL ENDPOINTS CONVENTION VIOLATED: fix before merging."
    exit 1
fi

if [ "$WARNINGS" -gt 0 ]; then
    warn "VERDICT: OK TO MERGE, but shared files are touched."
    warn "The current merge is clean; re-run the audit if develop moves again."
    exit 2
fi

ok "VERDICT: ALL CLEAR - shared files intact, clean merge."
exit 0
