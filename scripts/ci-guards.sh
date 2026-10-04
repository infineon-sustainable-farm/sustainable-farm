#!/usr/bin/env bash
#
# ci-guards.sh : garde-fous statiques appliqués au diff d'une PR.
#
# Analyse les lignes/fichiers AJOUTÉS par une branche (vs la base d'intégration)
# et détecte les familles de défauts recensées dans pr-reviews/ (mass assignment,
# CORS localhost, @ManyToOne EAGER, secrets/données démo en migration, downgrade
# pom.xml, entité en @RequestBody, runner sans @Profile, 2e advice sans @Order,
# PK sans @GeneratedValue, URL codées en dur, findAll non borné...).
#
# Ne vérifie QUE le diff : la dette existante de la base ne bloque pas les PR,
# seul le nouveau code est gated.
#
# Usage :
#   scripts/ci-guards.sh [head_ref] [base_ref] [chemin_rapport]
#
#   head_ref        Branche/SHA à auditer (défaut : HEAD)
#   base_ref        Base d'intégration (défaut : origin/develop)
#   chemin_rapport  Fichier markdown de sortie (défaut : guards-report.md)
#
# Codes de sortie :
#   0 = rien à signaler
#   1 = au moins une violation BLOQUANTE
#   2 = uniquement des avertissements

set -uo pipefail

HEAD_REF_ARG="${1:-HEAD}"
BASE_REF_ARG="${2:-origin/develop}"
REPORT_PATH="${3:-guards-report.md}"

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null) || {
    echo "Erreur : exécutez le script depuis un dépôt git." >&2
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
    || resolve_ref "$HEAD_REF_ARG") || { echo "Erreur : ref introuvable : $HEAD_REF_ARG" >&2; exit 1; }
BASE_REF=$(resolve_ref "$BASE_REF_ARG") || { echo "Erreur : ref de base introuvable : $BASE_REF_ARG" >&2; exit 1; }
MERGE_BASE=$(git merge-base "$BASE_REF" "$HEAD_REF") || {
    echo "Erreur : pas d'ancêtre commun $BASE_REF..$HEAD_REF" >&2; exit 1
}

FAILS=()
WARNS=()
fail() { FAILS+=("$1"); }
warn() { WARNS+=("$1"); }

# Liste "N:ligne" des lignes ajoutées d'un fichier du diff (numéros fichier nouveau).
added_lines() {
    git diff -U0 "$MERGE_BASE" "$HEAD_REF" -- "$1" 2>/dev/null | awk '
        /^@@/ { if (match($0, /\+[0-9]+/)) ln = substr($0, RSTART + 1, RLENGTH - 1) + 0; next }
        /^\+\+\+/ { next }
        /^\+/ { print ln ":" substr($0, 2); ln++ }
    '
}

show_head() { git show "$HEAD_REF:$1" 2>/dev/null; }

# Fichiers ajoutés/modifiés (C=créé, M=modifié, R=renommé) sous un préfixe, filtrés par extension.
changed_files() { # <préfixe> <regex extensions>
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
# F1 — @CrossOrigin ajouté (CORS par controller : écrase la config centrale)
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        n="${line%%:*}"
        fail "F1 **@CrossOrigin interdit** — \`$f:$n\` — le CORS doit rester centralisé (WebConfig / \`app.cors.allowed-origins\`). Supprimez l'annotation."
    done < <(added_lines "$f" | grep '@CrossOrigin' || true)
done

# ---------------------------------------------------------------------------
# F2 — URL codée en dur (localhost / IP privée) dans le code Java/JS/TS
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}" "${FEFILES[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        content="${line#*:}"
        printf '%s' "$content" | grep -q '\${' && continue
        n="${line%%:*}"
        fail "F2 **URL/IP codée en dur** — \`$f:$n\` — passez par la config (env vars) ou le client API partagé (\`shared/api/client.js\`)."
    done < <(added_lines "$f" | grep -E 'https?://(localhost|127\.0\.0\.1|10\.[0-9]+\.[0-9]+\.[0-9]+|192\.168\.[0-9]+\.[0-9]+)' || true)
done

# ---------------------------------------------------------------------------
# F3/F4 — Secrets et données démo dans les migrations Flyway
# ---------------------------------------------------------------------------
mapfile -t MIGFILES < <(changed_files 'backend/' 'db/migration/.*\.(sql|java)$')
for f in "${MIGFILES[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"; c="${line#*:}"
        if printf '%s' "$c" | grep -qE '\$2[aby]\$|password_hash|bcrypt|NO_LOGIN'; then
            fail "F3 **secret/hash de mot de passe en migration** — \`$f:$n\` — Flyway n'a pas de notion de profil : ces valeurs partent en production. À retirer."
        elif printf '%s' "$c" | grep -qiE "insert into (public\.)?(users|app_user)s?\b|'Demo |demo@|example\.com|admin@"; then
            fail "F4 **données de démo/comptes en migration** — \`$f:$n\` — les migrations ne doivent contenir aucun INSERT de comptes ou de données de démo (cf. PR-33)."
        fi
    done < <(added_lines "$f" | grep -iE '\$2[aby]\$|password_hash|bcrypt|NO_LOGIN|insert into|demo|example\.com|admin@' || true)
done

# ---------------------------------------------------------------------------
# F5 — Protection du pom.xml partagé (starters supprimés, java.version baissé)
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
                fail "F5 **dépendance retirée du pom.xml partagé** — \`$dep\` supprimé (cf. PR-42 : suppression de starter-mail = develop cassé). Restoration requise ou PR dédiée \`shared-config\`."
        done <<< "$removed"
    fi
    jbase=$(git show "$MERGE_BASE:backend/pom.xml" | grep -oE '<java\.version>[0-9.]+' | head -1 | sed 's/.*<java\.version>//')
    jhead=$(git show "$HEAD_REF:backend/pom.xml" | grep -oE '<java\.version>[0-9.]+' | head -1 | sed 's/.*<java\.version>//')
    if [ -n "$jbase" ] && [ -n "$jhead" ] && [ "$jbase" != "$jhead" ]; then
        lower=$(awk -v a="$jhead" -v b="$jbase" 'BEGIN { print (a < b) ? "1" : "0" }')
        [ "$lower" = "1" ] && fail "F5 **downgrade java.version $jbase → $jhead** dans \`backend/pom.xml\` (cf. PR-29 : rétrogradation silencieuse 21 → 17)."
    fi
fi

# ---------------------------------------------------------------------------
# F6 — Mass assignment : entité JPA liée directement au @RequestBody
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
            fail "F6 **mass assignment** — \`$f:$n\` — \`@RequestBody $type\` lie l'entité JPA au corps HTTP : le client contrôle PK/champs serveur (cf. PR-26/27/43). Utilisez un DTO de requête sans \`id\`."
        fi
    done < <(added_lines "$f" | grep '@RequestBody' || true)
done

# ---------------------------------------------------------------------------
# F7 — @ManyToOne sans LAZY (EAGER par défaut : N+1, cf. PR-43)
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        content="${line#*:}"
        printf '%s' "$content" | grep -q 'LAZY' && continue
        n="${line%%:*}"
        fail "F7 **\`@ManyToOne\` sans \`fetch = LAZY\`** — \`$f:$n\` — EAGER par défaut = N+1 systématique. Ajoutez \`fetch = FetchType.LAZY\`."
    done < <(added_lines "$f" | grep '@ManyToOne' || true)
done

# ---------------------------------------------------------------------------
# F8 — CommandLineRunner/ApplicationRunner sans @Profile (seeder en prod)
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    show_head "$f" | grep -qE 'CommandLineRunner|ApplicationRunner' || continue
    added_lines "$f" | grep -qE 'CommandLineRunner|ApplicationRunner' || continue
    show_head "$f" | grep -q '@Profile' || \
        fail "F8 **runner de seeding sans \`@Profile\`** — \`$f\` — un CommandLineRunner sans \`@Profile(\"dev\")\` s'exécute en production (cf. PR-26)."
done

# ---------------------------------------------------------------------------
# F9 — Second @RestControllerAdvice sans @Order (conflit d'enveloppe d'erreur)
# ---------------------------------------------------------------------------
BASE_ADVICES=$(git grep -l '@RestControllerAdvice' "$MERGE_BASE" -- backend 2>/dev/null | wc -l || true)
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    show_head "$f" | grep -q '@RestControllerAdvice' || continue
    added_lines "$f" | grep -q '@RestControllerAdvice' || continue
    if [ "$BASE_ADVICES" -ge 1 ] && ! show_head "$f" | grep -q '@Order'; then
        fail "F9 **second \`@RestControllerAdvice\` sans \`@Order\`** — \`$f\` — deux advices non ordonnés = enveloppes d'erreur concurrentes, les erreurs par champ du frontend cassent (cf. PR-42)."
    fi
done

# ---------------------------------------------------------------------------
# F10 / W2 — Entités ajoutées : PK sans @GeneratedValue, absence de @Version
# ---------------------------------------------------------------------------
mapfile -t NEWENT < <(added_files 'backend/' "$JAVA_RE")
for f in "${NEWENT[@]:-}"; do
    [ -z "$f" ] && continue
    content=$(show_head "$f")
    printf '%s' "$content" | grep -q '@Entity' || continue
    if printf '%s' "$content" | grep -q '@Id' && ! printf '%s' "$content" | grep -q '@GeneratedValue'; then
        fail "F10 **PK sans \`@GeneratedValue\`** — \`$f\` — un POST avec un \`id\` existant écrase l'enregistrement (cf. PR-43)."
    fi
    if ! printf '%s' "$content" | grep -q '@Version' \
       && ! printf '%s' "$content" | grep -qE 'extends\s+[A-Za-z0-9_]*(BaseEntity|Auditable)'; then
        warn "W2 entité sans \`@Version\` ni BaseEntity auditée (lost updates possibles, cf. PR-27/29/31) — \`$f\`"
    fi
done

# ---------------------------------------------------------------------------
# F11 — Secrets littéraux codés en dur (hors .example / compose / docs)
# ---------------------------------------------------------------------------
mapfile -t SECRETFILES < <(changed_files 'backend/' "$JAVA_RE|\.properties$")
for f in "${SECRETFILES[@]:-}" "${FEFILES[@]:-}"; do
    [ -z "$f" ] && continue
    case "$f" in *.example|*docker-compose*|*.md) continue ;; esac
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"
        fail "F11 **secret littéral codé en dur** — \`$f:$n\` — utilisez une variable d'environnement (jamais de valeur en dur, cf. PR-33)."
    done < <(added_lines "$f" | grep -iE '(password|passwd|secret|api[-_]?key|token)[[:space:]]*[:=][[:space:]]*["'"'"'][^"'"'"'${}]{6,}' || true)
done

# ---------------------------------------------------------------------------
# W1 — findAll() non borné dans services/controllers
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    case "$f" in */service/*|*Controller*) ;; *) continue ;; esac
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"
        warn "W1 lecture potentiellement non bornée (\`findAll\`/taille de page non plafonnée, cf. PR-29, stack M4) — \`$f:$n\`"
    done < <(added_lines "$f" | grep -E '\.findAll\(|getSize' || true)
done

# ---------------------------------------------------------------------------
# W3 — Exceptions brutes jetées (→ 500 au lieu de 4xx)
# ---------------------------------------------------------------------------
for f in "${BACKJAVA[@]:-}"; do
    [ -z "$f" ] && continue
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"
        warn "W3 exception brute → risque de 500 au lieu de 4xx (cf. PR-27, PR-42) — \`$f:$n\` — préférez \`ResourceNotFoundException\` / exceptions métier."
    done < <(added_lines "$f" | grep -E 'throw new (RuntimeException|IllegalArgumentException|IllegalStateException)\(' || true)
done

# ---------------------------------------------------------------------------
# W4 — fetch() direct hors couche API frontend
# ---------------------------------------------------------------------------
for f in "${FEFILES[@]:-}"; do
    [ -z "$f" ] && continue
    case "$f" in */api/*) continue ;; esac
    while IFS= read -r line; do
        [ -z "$line" ] && continue
        n="${line%%:*}"
        warn "W4 \`fetch()\` hors de la couche API partagée — \`$f:$n\` — passez par \`shared/api/client.js\` (timeout, erreurs cohérentes, cf. PR-26)."
    done < <(added_lines "$f" | grep -E '\bfetch\(' || true)
done

# ---------------------------------------------------------------------------
# Rapport
# ---------------------------------------------------------------------------
NFILES=$(git diff --name-only --diff-filter=ACMR "$MERGE_BASE" "$HEAD_REF" 2>/dev/null | wc -l)

VERDICT="✅ AUCUNE VIOLATION"
[ "${#WARNS[@]}" -gt 0 ] && VERDICT="🟡 OK — avertissements à considérer"
[ "${#FAILS[@]}" -gt 0 ] && VERDICT="🔴 BLOQUANT — corriger avant merge"

{
    echo "<!-- farm-quality-gate-guards -->"
    echo "### 🛡️ Garde-fous statiques (\`ci-guards.sh\`)"
    echo
    echo "Diff audité : \`${BASE_REF}...${HEAD_REF}\` — ${NFILES} fichier(s) modifié(s). Seules les lignes ajoutées sont vérifiées."
    echo
    if [ "${#FAILS[@]}" -gt 0 ]; then
        echo "#### ❌ Bloquants (${#FAILS[@]})"
        echo
        i=1
        for msg in "${FAILS[@]}"; do printf '%d. %s\n' "$i" "$msg"; i=$((i + 1)); done
        echo
    fi
    if [ "${#WARNS[@]}" -gt 0 ]; then
        echo "#### 🟡 Avertissements (${#WARNS[@]})"
        echo
        i=1
        for msg in "${WARNS[@]}"; do printf '%d. %s\n' "$i" "$msg"; i=$((i + 1)); done
        echo
    fi
    if [ "${#FAILS[@]}" -eq 0 ] && [ "${#WARNS[@]}" -eq 0 ]; then
        echo "Aucun motif interdit détecté dans les lignes ajoutées."
        echo
    fi
    echo "**Verdict : ${VERDICT}**"
} > "$REPORT_PATH"

echo "=== ci-guards.sh : ${VERDICT} (bloquants=${#FAILS[@]}, avertissements=${#WARNS[@]}) ==="
[ "${#FAILS[@]}" -gt 0 ] && exit 1
[ "${#WARNS[@]}" -gt 0 ] && exit 2
exit 0
