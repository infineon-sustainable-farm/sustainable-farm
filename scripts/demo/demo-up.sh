#!/usr/bin/env bash
#
# PT demo — charge (ou recharge) le jeu de données de démonstration.
#
# Le script est IDEMPOTENT : le seed SQL termine chaque bloc par
# `ON CONFLICT DO NOTHING`, donc deux exécutions donnent les mêmes comptages.
#
# Ce qu'il fait, dans l'ordre :
#   1. vérifie les prérequis (docker, démon, seed, script de vérification) ;
#   2. trouve le conteneur Postgres (ou le démarre si DEMO_AUTOSTART=1) ;
#   3. attend que Postgres accepte les connexions ;
#   4. vérifie que les tables PT existent (sinon démarre le backend, qui joue
#      les migrations Flyway) ;
#   5. applique le seed PT — les 3 équipements EQ-DEMO-01..03 sont dans sa
#      section 2, avant les tables qui les référencent ;
#   6. exécute verify_demo_data.sql, qui échoue si un comptage est faux.
#
# Variables d'environnement (toutes optionnelles) :
#   DEMO_DB_CONTAINER   conteneur Postgres à utiliser (défaut : service compose)
#   DEMO_DB_USER        utilisateur Postgres (défaut : postgres)
#   DEMO_DB_NAME        base de données (défaut : sustainable_farm)
#   DEMO_AUTOSTART     1 = démarre la base/le backend si besoin (défaut : 1)
#   DEMO_READY_TIMEOUT  secondes d'attente de Postgres (défaut : 60)
#   DEMO_BACKEND_TIMEOUT secondes d'attente du backend (défaut : 240)
#
# Exemples :
#   scripts/demo/demo-up.sh
#   DEMO_DB_CONTAINER=pt-verify-db DEMO_DB_USER=farm scripts/demo/demo-up.sh
#
# Auteur : Abdoul Ben Fatao SANON
# Version : 1.0.0

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd -- "$SCRIPT_DIR/../.." && pwd)
SEED_FILE="$ROOT_DIR/backend/src/main/resources/seed/producttransformation-demo-seed.sql"
VERIFY_SQL="$SCRIPT_DIR/verify_demo_data.sql"
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"

DB_CONTAINER=${DEMO_DB_CONTAINER:-}
DB_USER=${DEMO_DB_USER:-postgres}
DB_NAME=${DEMO_DB_NAME:-sustainable_farm}
AUTOSTART=${DEMO_AUTOSTART:-1}
READY_TIMEOUT=${DEMO_READY_TIMEOUT:-60}
BACKEND_TIMEOUT=${DEMO_BACKEND_TIMEOUT:-240}

log() { printf '[demo-up] %s\n' "$*"; }
warn() { printf '[demo-up] ATTENTION : %s\n' "$*" >&2; }
die() {
    printf '[demo-up] ERREUR : %s\n' "$*" >&2
    exit 1
}

# psql dans le conteneur, arrêt à la première erreur SQL.
psql_run() {
    docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" "$@"
}

# ---------------------------------------------------------------------------
# 1. Prérequis
# ---------------------------------------------------------------------------
log "vérification des prérequis"

command -v docker >/dev/null 2>&1 || die "docker est introuvable dans le PATH."
docker info >/dev/null 2>&1 || die "le démon Docker ne répond pas (lancez Docker Desktop / dockerd)."

[ -s "$SEED_FILE" ] || die "seed introuvable ou vide : $SEED_FILE"
[ -s "$VERIFY_SQL" ] || die "script de vérification introuvable : $VERIFY_SQL"

if [ -z "$DB_CONTAINER" ]; then
    [ -f "$COMPOSE_FILE" ] || die "docker-compose.yml introuvable : $COMPOSE_FILE"
    [ -f "$ROOT_DIR/.env" ] || warn ".env absent : docker compose utilisera ses valeurs par défaut."
fi

# ---------------------------------------------------------------------------
# 2. Conteneur Postgres
# ---------------------------------------------------------------------------
if [ -z "$DB_CONTAINER" ]; then
    DB_CONTAINER=$(docker compose -f "$COMPOSE_FILE" ps -q postgres 2>/dev/null || true)
fi

is_running() {
    [ -n "$1" ] && [ "$(docker inspect -f '{{.State.Running}}' "$1" 2>/dev/null || echo false)" = "true" ]
}

if ! is_running "$DB_CONTAINER"; then
    if [ -n "${DEMO_DB_CONTAINER:-}" ]; then
        die "le conteneur '$DEMO_DB_CONTAINER' n'existe pas ou n'est pas démarré."
    fi
    [ "$AUTOSTART" = "1" ] || die "Postgres n'est pas démarré et DEMO_AUTOSTART=0 (démarrez-le : docker compose up -d postgres)."
    log "démarrage de Postgres (docker compose up -d postgres)"
    docker compose -f "$COMPOSE_FILE" up -d postgres
    DB_CONTAINER=$(docker compose -f "$COMPOSE_FILE" ps -q postgres)
    is_running "$DB_CONTAINER" || die "Postgres n'a pas démarré (voir docker compose logs postgres)."
fi

log "conteneur Postgres : $(docker inspect -f '{{.Name}}' "$DB_CONTAINER" 2>/dev/null | sed 's|^/||') ($DB_USER@$DB_NAME)"

# ---------------------------------------------------------------------------
# 3. Attente de Postgres
# ---------------------------------------------------------------------------
log "attente de Postgres (max ${READY_TIMEOUT}s)"
ready=0
for _ in $(seq 1 "$READY_TIMEOUT"); do
    if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
        ready=1
        break
    fi
    sleep 1
done
[ "$ready" = "1" ] || die "Postgres n'accepte pas les connexions après ${READY_TIMEOUT}s."

# ---------------------------------------------------------------------------
# 4. Tables PT (créées par Flyway au démarrage du backend)
# ---------------------------------------------------------------------------
has_pt_tables() {
    [ "$(psql_run -tAc "SELECT to_regclass('public.batch') IS NOT NULL" 2>/dev/null | tr -d '[:space:]')" = "t" ]
}

if ! has_pt_tables; then
    if [ -n "${DEMO_DB_CONTAINER:-}" ]; then
        die "les tables PT sont absentes de la base '$DB_NAME' (conteneur '$DEMO_DB_CONTAINER') : démarrez le backend (Flyway) avant de semer."
    fi
    [ "$AUTOSTART" = "1" ] || die "les tables PT sont absentes et DEMO_AUTOSTART=0 (démarrez le backend pour jouer les migrations)."
    log "tables PT absentes : démarrage du backend pour jouer les migrations Flyway"
    docker compose -f "$COMPOSE_FILE" up -d backend

    backend_id=$(docker compose -f "$COMPOSE_FILE" ps -q backend)
    [ -n "$backend_id" ] || die "le service backend n'a pas démarré."

    log "attente du backend (max ${BACKEND_TIMEOUT}s)"
    healthy=0
    for _ in $(seq 1 "$BACKEND_TIMEOUT"); do
        status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$backend_id" 2>/dev/null || echo none)
        if [ "$status" = "healthy" ]; then
            healthy=1
            break
        fi
        if [ "$(docker inspect -f '{{.State.Running}}' "$backend_id" 2>/dev/null || echo false)" != "true" ]; then
            die "le backend s'est arrêté au démarrage (voir docker compose logs backend)."
        fi
        sleep 1
    done
    [ "$healthy" = "1" ] || die "le backend n'est pas healthy après ${BACKEND_TIMEOUT}s (voir docker compose logs backend)."
    has_pt_tables || die "les tables PT sont toujours absentes après le démarrage du backend."
fi

# ---------------------------------------------------------------------------
# 5. Seed
# ---------------------------------------------------------------------------
log "application du seed : ${SEED_FILE#$ROOT_DIR/}"
# -q : psql n'affiche pas les étiquettes BEGIN/INSERT 0 0, inutiles en démo.
psql_run -q <"$SEED_FILE"

# ---------------------------------------------------------------------------
# 6. Vérification
# ---------------------------------------------------------------------------
log "vérification des comptages (verify_demo_data.sql)"
psql_run <"$VERIFY_SQL"

log "OK — jeu de démonstration chargé et vérifié."
