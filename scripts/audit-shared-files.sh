#!/usr/bin/env bash
#
# audit-shared-files.sh : audit des fichiers partagés d'une branche avant merge.
#
# Compare l'état des fichiers partagés (frontend app/shared, backend, infra)
# entre une branche à reviewer et la branche d'intégration (origin/develop par
# défaut), simule le merge pour détecter les conflits réels, et vérifie la
# convention "endpoints module-local".
#
# Usage :
#   scripts/audit-shared-files.sh [branche] [ref_base] [--no-fetch] [--cross-check]
#
#   branche        Branche à auditer (défaut : branche courante). Peut être un
#                  nom, un ref distant (origin/xxx), un tag ou un SHA.
#   ref_base       Branche d'intégration (défaut : origin/develop).
#   --no-fetch     Ne pas exécuter git fetch avant l'audit.
#   --cross-check  Simule aussi les merges avec les autres branches
#                  origin/feature/* et signale les conflits croisés.
#
# Codes de sortie :
#   0 = aucun conflit ni risque détecté
#   1 = conflit de merge réel, fichier clé divergent des deux côtés, ou
#       violation de la convention endpoints module-local
#   2 = avertissement(s) : fichiers partagés touchés par la branche, retards,
#       conflits croisés avec d'autres branches feature
#
# Exemples :
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
    echo "Erreur : exécutez le script depuis un dépôt git." >&2
    exit 1
}
cd "$REPO_ROOT" || exit 1

# ---------------------------------------------------------------------------
# Fichiers clés partagés à vérifier en priorité (complétez librement).
# ---------------------------------------------------------------------------
KEY_FILES=(
    # Frontend : application et partagé
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
    # Backend : configuration et build
    backend/pom.xml
    backend/src/main/resources/application.properties
    backend/src/test/resources/application-test.properties
    backend/Dockerfile
    # Infra / racine
    docker-compose.yml
    .env.example
    .github/workflows/ci.yml
    .gitignore
)

# Fichier partagé des endpoints, cible de la convention module-local.
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
                echo "Argument inconnu : $arg" >&2
                exit 1
            fi
            ;;
    esac
done

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
[ -z "$BRANCH_ARG" ] && BRANCH_ARG="$CURRENT_BRANCH"
[ -z "$BASE_REF_ARG" ] && BASE_REF_ARG="origin/develop"

# ---------------------------------------------------------------------------
# Résolution des refs
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
        info "Récupération de origin (git fetch --prune)..."
        if ! git fetch origin --prune --quiet; then
            warn "git fetch a échoué : audit sur les refs locales."
        fi
    else
        warn "Pas de remote 'origin' : audit sur les refs locales."
    fi
fi

BRANCH_REF=$(resolve_ref "$BRANCH_ARG") || {
    echo "Erreur : branche introuvable : $BRANCH_ARG" >&2
    exit 1
}
BASE_REF=$(resolve_ref "$BASE_REF_ARG") || {
    echo "Erreur : branche de base introuvable : $BASE_REF_ARG" >&2
    exit 1
}

MERGE_BASE=$(git merge-base "$BASE_REF" "$BRANCH_REF" 2>/dev/null) || {
    echo "Erreur : aucun ancêtre commun entre $BASE_REF et $BRANCH_REF." >&2
    exit 1
}

# Module déduit du nom de branche (feature/<module>/...), utilisé par les
# vérifications de convention et de surface partagée.
MODULE=$(printf '%s' "$BRANCH_ARG" | sed -nE 's#^(feature|fix|chore|refactor|hotfix|release)/([^/]+)/.*#\2#p')

title "Contexte"
info "Branche auditée : ${C_BLD}${BRANCH_REF}${C_OFF} ($(git rev-parse --short "$BRANCH_REF"))"
info "Base d'intégration : ${C_BLD}${BASE_REF}${C_OFF} ($(git rev-parse --short "$BASE_REF"))"
info "Merge-base : $(git rev-parse --short "$MERGE_BASE")"
[ -n "$MODULE" ] && info "Module déduit du nom de branche : ${C_BLD}${MODULE}${C_OFF}"
if git merge-base --is-ancestor "$BASE_REF" "$BRANCH_REF"; then
    ok "La branche contient déjà toute la base : aucun merge nécessaire."
fi
info "Commits de la base absents de la branche : $(git rev-list --count "$MERGE_BASE..$BRANCH_REF") commit(s) branche / $(git rev-list --count "$MERGE_BASE..$BASE_REF") commit(s) base"

# ---------------------------------------------------------------------------
# 1. Simulation du merge (conflits réels)
# ---------------------------------------------------------------------------
REAL_CONFLICT=0
CONFLICTED=""

title "1. Simulation du merge $BASE_REF + $BRANCH_REF"
MERGE_TREE_HELP=$(git merge-tree -h 2>&1 || true)
case "$MERGE_TREE_HELP" in
    *--write-tree*) MERGE_TREE_SUPPORTED=1 ;;
    *) MERGE_TREE_SUPPORTED=0 ;;
esac

if [ "$MERGE_TREE_SUPPORTED" -eq 1 ]; then
    MERGE_OUT=$(git merge-tree --write-tree --name-only "$BASE_REF" "$BRANCH_REF" 2>&1)
    MERGE_RC=$?
    if [ "$MERGE_RC" -eq 0 ]; then
        ok "Aucun conflit de merge détecté."
    else
        REAL_CONFLICT=1
        CONFLICTED=$(printf '%s\n' "$MERGE_OUT" | tail -n +2 | sed '/^$/,$d' \
            | grep -vE '^(Auto-merging|CONFLICT|changed in both|added in|removed in)' || true)
        bad "Conflit(s) de merge détecté(s) :"
        if [ -n "$CONFLICTED" ]; then
            printf '%s\n' "$CONFLICTED" | while IFS= read -r c; do
                printf '  - %s\n' "$c"
            done
        fi
    fi
else
    warn "git merge-tree --write-tree non supporté (git < 2.38) : simulation ignorée."
fi

# ---------------------------------------------------------------------------
# 2. Fichiers clés
# ---------------------------------------------------------------------------
exists_at() { git cat-file -e "$1:$2" 2>/dev/null; }
changed_between() { ! git diff --quiet "$1" "$2" -- "$3" 2>/dev/null; }

RISK=0
WARNINGS=0

title "2. Fichiers clés partagés"

printf '%-56s %s\n' "FICHIER" "ÉTAT"
printf '%-56s %s\n' "--------------------------------------------------------" "-----------------------------"

for f in "${KEY_FILES[@]}"; do
    eb=0; ed=0; eh=0
    exists_at "$MERGE_BASE" "$f" && eb=1
    exists_at "$BASE_REF" "$f" && ed=1
    exists_at "$BRANCH_REF" "$f" && eh=1

    state=""
    if [ "$eh" -eq 0 ] && [ "$ed" -eq 1 ] && [ "$eb" -eq 1 ]; then
        state="${C_RED}SUPPRIMÉ PAR LA BRANCHE (existe sur develop) -> CONFLIT${C_OFF}"; RISK=1
    elif [ "$eh" -eq 0 ] && [ "$ed" -eq 1 ]; then
        state="${C_YEL}EN RETARD (ajouté sur develop, absent de la branche)"
    elif [ "$eh" -eq 0 ]; then
        state="${C_YEL}absent des deux côtés${C_OFF}"
    elif [ "$ed" -eq 0 ]; then
        state="${C_YEL}AJOUT BRANCHE (nouveau, pas de conflit)"
    elif ! changed_between "$MERGE_BASE" "$BRANCH_REF" "$f"; then
        if changed_between "$MERGE_BASE" "$BASE_REF" "$f"; then
            state="${C_YEL}EN RETARD sur develop (non modifié par la branche)"
        else
            state="${C_GRN}OK (identique à develop)${C_OFF}"
        fi
    elif ! changed_between "$MERGE_BASE" "$BASE_REF" "$f"; then
        state="${C_YEL}MODIFIÉ PAR LA BRANCHE (develop intact)${C_OFF}"; WARNINGS=$((WARNINGS + 1))
    elif changed_between "$BASE_REF" "$BRANCH_REF" "$f"; then
        state="${C_RED}DIVERGENT DES DEUX CÔTÉS -> CONFLIT POTENTIEL${C_OFF}"; RISK=1
    else
        state="${C_GRN}OK (aligné sur develop après adoption)${C_OFF}"
    fi
    printf '%-56s %b\n' "$f" "$state"
done

# ---------------------------------------------------------------------------
# 3. Convention endpoints module-local
# ---------------------------------------------------------------------------
# Règle : les endpoints d'un module doivent vivre dans
# frontend/src/features/<module>/api/endpoints.js, pas dans le fichier partagé
# frontend/src/shared/api/endpoints.js. Deux modules qui ajoutent des clés au
# même objet produisent un conflit de merge à chaque merge.
CONVENTION=0

title "3. Convention endpoints module-local"

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
                    bad "  - $ENDPOINTS_SHARED : $key = \"$value\" (endpoint du module '$mod' dans le fichier partagé)"
                    CONVENTION=1
                    ;;
            esac
        done
    done <<< "$SHARED_CONTENT"
else
    warn "Vérification impossible : $ENDPOINTS_SHARED ou le dossier features est absent de $BRANCH_REF."
fi

# Contrôle inverse : un module qui importe encore le fichier partagé.
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
                bad "  - $api_file importe shared/api/endpoints alors que le fichier module-local existe"
            else
                warn "  - $api_file importe shared/api/endpoints (créer frontend/src/features/$mod/api/endpoints.js)"
            fi
            CONVENTION=1
        fi
    done < <(git ls-tree -r --name-only "$BRANCH_REF" -- "frontend/src/features/$mod/api" 2>/dev/null | grep '\.js$' || true)
done

if [ "$CONVENTION" -eq 0 ]; then
    ok "Convention respectée : aucun endpoint de module dans $ENDPOINTS_SHARED."
fi

# ---------------------------------------------------------------------------
# 4. Surface partagée modifiée par la branche (hors module)
# ---------------------------------------------------------------------------
title "4. Surface partagée modifiée par la branche (hors module)"

if [ -n "$MODULE" ]; then
    info "Module détecté depuis le nom de branche : ${C_BLD}${MODULE}${C_OFF}"
    mapfile -t BRANCH_FILES < <(git diff --name-only "$MERGE_BASE" "$BRANCH_REF" | grep -v "/$MODULE/" || true)
else
    warn "Nom de branche hors convention feature/<module>/... : listing de tous les fichiers modifiés."
    mapfile -t BRANCH_FILES < <(git diff --name-only "$MERGE_BASE" "$BRANCH_REF")
fi

SHARED_COUNT=0
SHARED_RISKY=()
SHARED_OTHER=()
for f in "${BRANCH_FILES[@]}"; do
    [ -z "$f" ] && continue
    # Ne pas répéter les fichiers clés déjà couverts en section 2.
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
    bad "Fichiers partagés modifiés par la branche ET par develop :"
    printf '  - %s\n' "${SHARED_RISKY[@]}"
fi

if [ "${#SHARED_OTHER[@]}" -gt 0 ]; then
    warn "${#SHARED_OTHER[@]} fichier(s) partagé(s) ajouté(s)/modifié(s) par la branche (pas de conflit actuel) :"
    for i in "${!SHARED_OTHER[@]}"; do
        [ "$i" -ge "$MAX_LIST" ] && { warn "  ... et $(( ${#SHARED_OTHER[@]} - MAX_LIST )) autre(s)"; break; }
        printf '  - %s\n' "${SHARED_OTHER[$i]}"
    done
    WARNINGS=$((WARNINGS + 1))
fi

if [ "$SHARED_COUNT" -eq 0 ]; then
    ok "Aucun fichier partagé hors module touché par la branche."
fi

# ---------------------------------------------------------------------------
# 5. Cross-check avec les autres branches feature (--cross-check)
# ---------------------------------------------------------------------------
if [ "$CROSS_CHECK" -eq 1 ]; then
    title "5. Cross-check avec les autres branches feature"
    if [ "$MERGE_TREE_SUPPORTED" -eq 1 ]; then
        CROSS_BRANCHES=0
        CROSS_CONFLICTS=0
        SEEN_SHAS=""
        while IFS= read -r other; do
            [ -z "$other" ] && continue
            [ "$other" = "$BRANCH_REF" ] && continue
            # Éviter de tester deux refs identiques (ex. Feature/ et feature/).
            sha=$(git rev-parse "$other" 2>/dev/null || true)
            case " $SEEN_SHAS " in
                *" $sha "*) continue ;;
            esac
            SEEN_SHAS="$SEEN_SHAS $sha"
            # Ignorer les branches déjà contenues dans la base : leur contenu
            # est déjà dans develop, le cross-check n'apporte rien.
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
                warn "  - ${other#origin/} : ${files:-conflit}"
                CROSS_CONFLICTS=$((CROSS_CONFLICTS + 1))
            fi
        done < <(git for-each-ref --format='%(refname:short)' 'refs/remotes/origin/[Ff]eature/**' 2>/dev/null || true)

        if [ "$CROSS_CONFLICTS" -eq 0 ]; then
            ok "Aucun conflit croisé avec les $CROSS_BRANCHES branche(s) feature distante(s)."
        else
            warn "$CROSS_CONFLICTS conflit(s) croisé(s) sur $CROSS_BRANCHES branche(s) : le merge dans develop dépendra de l'ordre de merge."
            WARNINGS=$((WARNINGS + 1))
        fi
    else
        warn "git merge-tree --write-tree non supporté : cross-check ignoré."
    fi
fi

# ---------------------------------------------------------------------------
# 6. Rapport final
# ---------------------------------------------------------------------------
title "6. Rapport"

if [ "$REAL_CONFLICT" -eq 1 ]; then
    bad "VERDICT : CONFLIT(S) DE MERGE RÉEL(S) : ne pas merger en l'état."
    exit 1
fi

if [ "$RISK" -eq 1 ]; then
    bad "VERDICT : RISQUE DÉTECTÉ : vérifier les fichiers signalés avant merge."
    exit 1
fi

if [ "$CONVENTION" -eq 1 ]; then
    bad "VERDICT : CONVENTION ENDPOINTS MODULE-LOCAL NON RESPECTÉE : à corriger avant merge."
    exit 1
fi

if [ "$WARNINGS" -gt 0 ]; then
    warn "VERDICT : OK POUR LE MERGE, mais des fichiers partagés sont touchés."
    warn "Le merge actuel est propre ; relancer l'audit si develop bouge encore."
    exit 2
fi

ok "VERDICT : AUCUN SOUCI - fichiers partagés intacts, merge propre."
exit 0
