#!/usr/bin/env bash
#
# ci-guards.sh : static guards applied to a PR diff.
#
# Analyzes the lines/files ADDED by a branch (vs the integration base) and
# detects the defect families documented in pr-reviews/ (mass assignment,
# localhost CORS, EAGER @ManyToOne, secrets/demo data in migrations, pom.xml
# downgrades, entity bound to @RequestBody, runner without @Profile, second
# advice without @Order, PK without @GeneratedValue, hardcoded URLs,
# unbounded findAll...).
#
# Only the diff is checked: existing debt on the base never blocks PRs,
# only new code is gated.
#
# Usage:
#   scripts/ci-guards.sh [head_ref] [base_ref] [report_path]
#
#   head_ref        Branch/SHA to audit (default: HEAD)
#   base_ref        Integration base (default: origin/develop)
#   report_path     Output markdown file (default: guards-report.md)
#
# Exit codes:
#   0 = nothing to report
#   1 = at least one BLOCKING violation
#   2 = warnings only

set -uo pipefail

HEAD_REF_ARG="${1:-HEAD}"
BASE_REF_ARG="${2:-origin/develop}"
REPORT_PATH="${3:-guards-report.md}"

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || {
    echo "Error: run this script from a git repository." >&2
    exit 1
}
cd "$REPO_ROOT"

resolve_ref() {
    git rev-parse --verify --quiet "refs/remotes/origin/$1^{commit}" >/dev/null && { echo "origin/$1"; return; }
    git rev-parse --verify --quiet "refs/heads/$1^{commit}" >/dev/null && { echo "$1"; return; }
    git rev-parse --verify --quiet "$1^{commit}" >/dev/null && { echo "$1"; return; }
    return 1
}

HEAD_REF=$(git rev-parse --verify --quiet "$HEAD_REF_ARG^{commit}" 2>/dev/null \
    || resolve_ref "$HEAD_REF_ARG") || { echo "Error: ref not found: $HEAD_REF_ARG" >&2; exit 1; }
BASE_REF=$(resolve_ref "$BASE_REF_ARG") || { echo "Error: base ref not found: $BASE_REF_ARG" >&2; exit 1; }
MERGE_BASE=$(git merge-base "$BASE_REF" "$HEAD_REF") || {
    echo "Error: no common ancestor between $BASE_REF and $HEAD_REF" >&2
    exit 1
}

FAILS=()
WARNS=()
fail() { FAILS+=("$1"); }
warn() { WARNS+=("$1"); }

# "N:line" list of the added lines of a diffed file (new-file line numbers).
added_lines() {
    git diff -U0 "$MERGE_BASE" "$HEAD_REF" -- "$1" 2>/dev/null | awk '
        /^@@/ { if (match($0, /\+[0-9]+/)) ln = substr($0, RSTART + 1, RLENGTH - 1) + 0; next }
        /^\+\+\+/ { next }
        /^\+/ { print ln ":" substr($0, 2); ln++ }
    '
}

show_head() { git show "$HEAD_REF:$1" 2>/dev/null; }

# Added/modified files (C=created, M=modified, R=renamed) under a prefix, filtered by extension.
changed_files() { # <prefix> <extension regex>
    git diff --name-only --diff-filter=ACMR "$MERGE_BASE" "$HEAD_REF" -- "$1" 2>/dev/null | grep -E "$2" || true
}
added_files() {
    git diff --name-only --diff-filter=A "$MERGE_BASE" "$HEAD_REF" -- "$1" 2>/dev/null | grep -E "$2" || true
}

JAVA_RE='\.(java)$'
FE_RE='\.(js|jsx|ts|tsx)$'

mapfile -t BACKJAVA < <(changed_files 'backend/' "$JAVA_RE")
mapfile -t FEFILES < <(changed_files 'frontend/' "$FE_RE")

# ---------------------------------------------------------------------------
# F1 - @CrossOrigin added (per-controller CORS overrides central config)
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        n="${line%%:*}"
        fail "F1 **forbidden \`@CrossOrigin\`** - \`$f:$n\` - CORS must stay centralized (WebConfig / \`app.cors.allowed-origins\`). Remove the annotation."
    done < <(added_lines "$f" | grep '@CrossOrigin' || true)
done

# ---------------------------------------------------------------------------
# F2 - Hardcoded URL/IP (localhost / private IP) in Java/JS/TS code
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}" "${FEFILES[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        content="${line#*:}"
        printf '%s' "$content" | grep -q '\${' && continue
        n="${line%%:*}"
        fail "F2 **hardcoded URL/IP** - \`$f:$n\` - use configuration (env vars) or the shared API client (\`shared/api/client.js\`)."
    done < <(added_lines "$f" | grep -E 'https?://(localhost|127\.0\.0\.1|10\.[0-9]+\.[0-9]+\.[0-9]+|192\.168\.[0-9]+\.[0-9]+)' || true)
done

# ---------------------------------------------------------------------------
# F3/F4 - Secrets and demo data inside Flyway migrations
# ---------------------------------------------------------------------------
mapfile -t MIGFILES < <(changed_files 'backend/' 'db/migration/.*\.(sql|java)$')
for f in "${MIGFILES[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"; c="${line#*:}"
        if printf '%s' "$c" | grep -qE '\$2[aby]\$|password_hash|bcrypt|NO_LOGIN'; then
            fail "F3 **password hash/secret in migration** - \`$f:$n\` - Flyway has no profile mechanism: these values ship to production. Remove them."
        elif printf '%s' "$c" | grep -qiE "insert into (public\.)?(users|app_user)s?\b|'Demo |demo@|example\.com|admin@"; then
            fail "F4 **demo data/accounts in migration** - \`$f:$n\` - migrations must not INSERT accounts or demo data (see PR-33)."
        fi
    done < <(added_lines "$f" | grep -iE '\$2[aby]\$|password_hash|bcrypt|NO_LOGIN|insert into|demo|example\.com|admin@' || true)
done

# ---------------------------------------------------------------------------
# F5 - Shared pom.xml protection (removed starters, downgraded java.version)
# ---------------------------------------------------------------------------
if ! git diff --quiet "$MERGE_BASE" "$HEAD_REF" -- backend/pom.xml 2>/dev/null; then
    removed=$(git diff "$MERGE_BASE" "$HEAD_REF" -- backend/pom.xml | grep '^-.*<artifactId>spring-boot-starter-' \
        | grep -oE 'spring-boot-starter-[a-z0-9-]+' | sort -u || true)
    added=$(git diff "$MERGE_BASE" "$HEAD_REF" -- backend/pom.xml | grep '^+.*<artifactId>spring-boot-starter-' \
        | grep -oE 'spring-boot-starter-[a-z0-9-]+' | sort -u || true)
    if [ -n "$removed" ]; then
        while IFS= read -r dep; do
            [ -z "$dep" ] && continue
            printf '%s\n' "$added" | grep -qxF "$dep" || \
                fail "F5 **dependency removed from the shared pom.xml** - \`$dep\` deleted (see PR-42: starter-mail removal broke develop). Restore it or open a dedicated \`shared-config\` PR."
        done <<< "$removed"
    fi
    jbase=$(git show "$MERGE_BASE:backend/pom.xml" | grep -oE '<java\.version>[0-9.]+' | head -1 | sed 's/.*<java\.version>//')
    jhead=$(git show "$HEAD_REF:backend/pom.xml" | grep -oE '<java\.version>[0-9.]+' | head -1 | sed 's/.*<java\.version>//')
    if [ -n "$jbase" ] && [ -n "$jhead" ] && [ "$jbase" != "$jhead" ]; then
        lower=$(awk -v a="$jhead" -v b="$jbase" 'BEGIN { print (a < b) ? "1" : "0" }')
        [ "$lower" = "1" ] && fail "F5 **java.version downgrade $jbase to $jhead** in \`backend/pom.xml\` (see PR-29: silent 21 -> 17 downgrade)."
    fi
fi

# ---------------------------------------------------------------------------
# F6 - Mass assignment: JPA entity bound directly to @RequestBody
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    show_head "$f" | grep -qE '@RestController|@Controller' || continue
    entities=$(show_head "$f" | grep -oE '^import [A-Za-z0-9_.]*\.(entity|entities|domain|model)\.[A-Za-z0-9_.]+;' \
        | sed -E 's/.*\.([A-Za-z0-9_]+);/\1/' | sort -u || true)
    [ -z "$entities" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"; c="${line#*:}"
        type=$(printf '%s' "$c" | grep -oE '@RequestBody[[:space:]]+(final[[:space:]]+)?[A-Za-z0-9_]+' | awk '{print $NF}' | head -1)
        [ -z "$type" ] && continue
        if printf '%s\n' "$entities" | grep -qxF "$type" \
           && ! printf '%s' "$type" | grep -qE '(Dto|DTO|Request|Response|Payload|Input|Form|Command|VM)$'; then
            fail "F6 **mass assignment** - \`$f:$n\` - \`@RequestBody $type\` binds the JPA entity to the HTTP body: the client controls PK/server fields (see PR-26/27/43). Use a request DTO without \`id\`."
        fi
    done < <(added_lines "$f" | grep '@RequestBody' || true)
done

# ---------------------------------------------------------------------------
# F7 - @ManyToOne without LAZY (EAGER by default: N+1, see PR-43)
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        content="${line#*:}"
        printf '%s' "$content" | grep -q 'LAZY' && continue
        n="${line%%:*}"
        fail "F7 **\`@ManyToOne\` without \`fetch = LAZY\`** - \`$f:$n\` - EAGER by default = systematic N+1. Add \`fetch = FetchType.LAZY\`."
    done < <(added_lines "$f" | grep '@ManyToOne' || true)
done

# ---------------------------------------------------------------------------
# F8 - CommandLineRunner/ApplicationRunner without @Profile (seeder in prod)
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    show_head "$f" | grep -qE 'CommandLineRunner|ApplicationRunner' || continue
    added_lines "$f" | grep -qE 'CommandLineRunner|ApplicationRunner' || continue
    show_head "$f" | grep -q '@Profile' || \
        fail "F8 **seed runner without \`@Profile\`** - \`$f\` - a CommandLineRunner without \`@Profile(\"dev\")\` runs in production (see PR-26)."
done

# ---------------------------------------------------------------------------
# F9 - Second @RestControllerAdvice without @Order (competing error envelopes)
# ---------------------------------------------------------------------------
BASE_ADVICES=$(git grep -l '@RestControllerAdvice' "$MERGE_BASE" -- backend 2>/dev/null | wc -l || true)
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    show_head "$f" | grep -q '@RestControllerAdvice' || continue
    added_lines "$f" | grep -q '@RestControllerAdvice' || continue
    if [ "$BASE_ADVICES" -ge 1 ] && ! show_head "$f" | grep -q '@Order'; then
        fail "F9 **second \`@RestControllerAdvice\` without \`@Order\`** - \`$f\` - two unordered advices = competing error envelopes, frontend per-field errors break (see PR-42)."
    fi
done

# ---------------------------------------------------------------------------
# F10 / W2 - Added entities: PK without @GeneratedValue, missing @Version
# ---------------------------------------------------------------------------
mapfile -t NEWENT < <(added_files 'backend/' "$JAVA_RE")
for f in "${NEWENT[@]:-}"; do
    [ -z "$f" ] && continue
    content=$(show_head "$f")
    printf '%s' "$content" | grep -q '@Entity' || continue
    if printf '%s' "$content" | grep -q '@Id' && ! printf '%s' "$content" | grep -q '@GeneratedValue'; then
        fail "F10 **PK without \`@GeneratedValue\`** - \`$f\` - a POST with an existing \`id\` overwrites the record (see PR-43)."
    fi
    if ! printf '%s' "$content" | grep -q '@Version' \
       && ! printf '%s' "$content" | grep -qE 'extends\s+[A-Za-z0-9_]*(BaseEntity|Auditable)'; then
        warn "W2 entity without \`@Version\` or audited BaseEntity (possible lost updates, see PR-27/29/31) - \`$f\`"
    fi
done

# ---------------------------------------------------------------------------
# F11 - Hardcoded literal secrets (excluding .example / compose / docs)
# ---------------------------------------------------------------------------
mapfile -t SECRETFILES < <(changed_files 'backend/' "$JAVA_RE|\.properties$")
for f in "${SECRETFILES[@]:-}" "${FEFILES[@]:-}"; do
    [ -z "$f" ] && continue
    case "$f" in *.example|*docker-compose*|*.md) continue ;; esac
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"
        fail "F11 **hardcoded literal secret** - \`$f:$n\` - use an environment variable (never a literal value, see PR-33)."
    done < <(added_lines "$f" | grep -iE '(password|passwd|secret|api[-_]?key|token)[[:space:]]*[:=][[:space:]]*["'"'"'][^"'"'"'${}]{6,}' || true)
done

# ---------------------------------------------------------------------------
# W1 - Unbounded findAll() in services/controllers
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    case "$f" in */service/*|*Controller*) ;; *) continue ;; esac
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"
        warn "W1 potentially unbounded read (\`findAll\`/unclamped page size, see PR-29, stack M4) - \`$f:$n\`"
    done < <(added_lines "$f" | grep -E '\.findAll\(|getSize' || true)
done

# ---------------------------------------------------------------------------
# W3 - Raw exceptions thrown (to 500 instead of 4xx)
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"
        warn "W3 raw exception -> risk of 500 instead of 4xx (see PR-27, PR-42) - \`$f:$n\` - prefer \`ResourceNotFoundException\` / business exceptions."
    done < <(added_lines "$f" | grep -E 'throw new (RuntimeException|IllegalArgumentException|IllegalStateException)\(' || true)
done

# ---------------------------------------------------------------------------
# W4 - Direct fetch() outside the frontend API layer
# ---------------------------------------------------------------------------
for f in "${FEFILES[@]:-}"; do
    [ -z "$f" ] && continue
    case "$f" in */api/*) continue ;; esac
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"
        warn "W4 \`fetch()\` outside the shared API layer - \`$f:$n\` - go through \`shared/api/client.js\` (timeout, consistent errors, see PR-26)."
    done < <(added_lines "$f" | grep -E '\bfetch\(' || true)
done

# ---------------------------------------------------------------------------
# Report
# ---------------------------------------------------------------------------
NFILES=$(git diff --name-only --diff-filter=ACMR "$MERGE_BASE" "$HEAD_REF" 2>/dev/null | wc -l)

VERDICT="NO VIOLATION"
[ "${#WARNS[@]}" -gt 0 ] && VERDICT="OK - warnings to consider"
[ "${#FAILS[@]}" -gt 0 ] && VERDICT="BLOCKING - fix before merge"

{
    echo "<!-- farm-quality-gate-guards -->"
    echo "### Static guards (\`ci-guards.sh\`)"
    echo
    echo "Audited diff: \`${BASE_REF}...${HEAD_REF}\` - ${NFILES} file(s) changed. Only added lines are checked."
    echo
    if [ "${#FAILS[@]}" -gt 0 ]; then
        echo "#### Blocking (${#FAILS[@]})"
        echo
        i=1
        for msg in "${FAILS[@]}"; do printf '%d. %s\n' "$i" "$msg"; i=$((i + 1)); done
        echo
    fi
    if [ "${#WARNS[@]}" -gt 0 ]; then
        echo "#### Warnings (${#WARNS[@]})"
        echo
        i=1
        for msg in "${WARNS[@]}"; do printf '%d. %s\n' "$i" "$msg"; i=$((i + 1)); done
        echo
    fi
    if [ "${#FAILS[@]}" -eq 0 ] && [ "${#WARNS[@]}" -eq 0 ]; then
        echo "No forbidden pattern detected in added lines."
        echo
    fi
    echo "**Verdict: ${VERDICT}**"
} > "$REPORT_PATH"

echo "ci-guards.sh verdict: ${VERDICT} (blocking=${#FAILS[@]}, warnings=${#WARNS[@]})"
[ "${#FAILS[@]}" -gt 0 ] && exit 1
[ "${#WARNS[@]}" -gt 0 ] && exit 2
exit 0
