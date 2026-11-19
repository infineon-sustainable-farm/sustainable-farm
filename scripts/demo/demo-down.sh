#!/usr/bin/env bash
#
# PT demo — arrête proprement la pile de démonstration.
#
# `docker compose stop` conserve les conteneurs ET le volume Postgres : les
# données restent en place et `docker compose start` (ou demo-up.sh) les
# retrouve. Aucun `down -v`, aucun volume supprimé.
#
# Par défaut le script arrête `backend` puis `postgres`, c'est-à-dire les deux
# services dont dépend la démo PT. Si vous voulez arrêter d'autres services :
#
#   DEMO_SERVICES="frontend backend postgres" scripts/demo/demo-down.sh
#
# Auteur : Abdoul Ben Fatao SANON
# Version : 1.0.0

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd -- "$SCRIPT_DIR/../.." && pwd)
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"
SERVICES=${DEMO_SERVICES:-"backend postgres"}

log() { printf '[demo-down] %s\n' "$*"; }
die() {
    printf '[demo-down] ERREUR : %s\n' "$*" >&2
    exit 1
}

command -v docker >/dev/null 2>&1 || die "docker est introuvable dans le PATH."
docker info >/dev/null 2>&1 || die "le démon Docker ne répond pas."
[ -f "$COMPOSE_FILE" ] || die "docker-compose.yml introuvable : $COMPOSE_FILE"

# shellcheck disable=SC2086 # SERVICES est volontairement une liste de mots.
log "arrêt des services : $SERVICES"
# shellcheck disable=SC2086
docker compose -f "$COMPOSE_FILE" stop $SERVICES

log "conteneurs arrêtés, volume conservé."
log "redémarrage : docker compose -f ${COMPOSE_FILE#$ROOT_DIR/} start $SERVICES"
