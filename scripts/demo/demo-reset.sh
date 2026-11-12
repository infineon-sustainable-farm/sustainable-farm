#!/usr/bin/env bash
#
# PT demo — retire UNIQUEMENT les lignes de démonstration.
#
# Aucun DROP, aucun TRUNCATE, aucun DELETE non préfixé : le script ne touche
# qu'aux identifiants DEMO (LOT-DEMO-*, RI-DEMO-*, QC-DEMO-*, EQ-DEMO-*, …) et
# aux 12 mois de historical_harvest marqués weather_condition = 'DEMO'.
# Les autres modules (Plants, Energy, Machinery…) et les lots LOT-GP-* du
# golden path ne sont jamais supprimés.
#
# Ordre des suppressions : tables filles d'abord, puis les données maîtres.
# Les opérateurs et équipements DEMO ne sont supprimés que s'ils ne sont plus
# référencés ; sinon ils restent en place et demo-up.sh les réutilise
# (ON CONFLICT DO NOTHING), donc les comptages restent exacts.
#
# Le script réussit quand les tables de traitement (lots, réceptions, QC,
# séchages, conditionnements, conformité, historique) ne contiennent plus
# aucune ligne DEMO. Les données maîtres encore référencées sont signalées.
#
# Variables d'environnement : identiques à demo-up.sh
#   DEMO_DB_CONTAINER, DEMO_DB_USER, DEMO_DB_NAME
#
# Usage :
#   scripts/demo/demo-reset.sh            # supprime les lignes DEMO
#   scripts/demo/demo-reset.sh --dry-run  # affiche ce qui serait supprimé
#
# Auteur : Abdoul Ben Fatao SANON
# Version : 1.0.0

set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ROOT_DIR=$(cd -- "$SCRIPT_DIR/../.." && pwd)
COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"

DB_CONTAINER=${DEMO_DB_CONTAINER:-}
DB_USER=${DEMO_DB_USER:-postgres}
DB_NAME=${DEMO_DB_NAME:-sustainable_farm}
DRY_RUN=0

log() { printf '[demo-reset] %s\n' "$*"; }
warn() { printf '[demo-reset] ATTENTION : %s\n' "$*" >&2; }
die() {
    printf '[demo-reset] ERREUR : %s\n' "$*" >&2
    exit 1
}

while [ $# -gt 0 ]; do
    case "$1" in
        --dry-run) DRY_RUN=1 ;;
        -h | --help)
            sed -n '2,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *) die "argument inconnu : $1 (utilisez --dry-run ou --help)" ;;
    esac
    shift
done

psql_run() {
    docker exec -i "$DB_CONTAINER" psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_NAME" "$@"
}

# Nombre de lignes DEMO dans les tables de traitement (celles qui doivent
# disparaître), hors données maîtres.
total_tx() {
    psql_run -tA <<'SQL' | tr -d '[:space:]'
SELECT (SELECT count(*) FROM batch              WHERE batch_id LIKE 'LOT-DEMO-%')
     + (SELECT count(*) FROM harvest_event      WHERE harvest_id LIKE 'HE-DEMO-%')
     + (SELECT count(*) FROM raw_intake         WHERE intake_id LIKE 'RI-DEMO-%')
     + (SELECT count(*) FROM qc_checkpoint      WHERE checkpoint_id LIKE 'QC-DEMO-%')
     + (SELECT count(*) FROM drying_run         WHERE run_id LIKE 'DR-DEMO-%')
     + (SELECT count(*) FROM wash_sort_record   WHERE record_id LIKE 'WS-DEMO-%')
     + (SELECT count(*) FROM packaging_record   WHERE record_id LIKE 'PKG-DEMO-%')
     + (SELECT count(*) FROM compliance_record  WHERE record_id LIKE 'CR-DEMO-%')
     + (SELECT count(*) FROM historical_harvest WHERE weather_condition = 'DEMO');
SQL
}

# Nombre de lignes DEMO dans les données maîtres (opérateurs, équipements).
total_master() {
    psql_run -tA <<'SQL' | tr -d '[:space:]'
SELECT (SELECT count(*) FROM operator     WHERE operator_id LIKE 'OP-DEMO-%')
     + (SELECT count(*) FROM pt_equipment WHERE equipment_id LIKE 'EQ-DEMO-%');
SQL
}

# ---------------------------------------------------------------------------
# Prérequis
# ---------------------------------------------------------------------------
command -v docker >/dev/null 2>&1 || die "docker est introuvable dans le PATH."
docker info >/dev/null 2>&1 || die "le démon Docker ne répond pas."

if [ -z "$DB_CONTAINER" ]; then
    [ -f "$COMPOSE_FILE" ] || die "docker-compose.yml introuvable : $COMPOSE_FILE"
    DB_CONTAINER=$(docker compose -f "$COMPOSE_FILE" ps -q postgres 2>/dev/null || true)
fi

[ -n "$DB_CONTAINER" ] || die "Postgres n'est pas démarré (docker compose up -d postgres), rien à nettoyer."
[ "$(docker inspect -f '{{.State.Running}}' "$DB_CONTAINER" 2>/dev/null || echo false)" = "true" ] ||
    die "le conteneur '$DB_CONTAINER' n'est pas démarré, rien à nettoyer."

log "conteneur Postgres : $DB_CONTAINER ($DB_USER@$DB_NAME)"

if [ "$(psql_run -tAc "SELECT to_regclass('public.batch') IS NOT NULL" | tr -d '[:space:]')" != "t" ]; then
    die "les tables PT sont absentes : base vierge, rien à nettoyer."
fi

# ---------------------------------------------------------------------------
# État avant
# ---------------------------------------------------------------------------
tx_before=$(total_tx)
master_before=$(total_master)
log "lignes DEMO : $tx_before dans les tables de traitement, $master_before en données maîtres"

if [ "$tx_before" = "0" ] && [ "$master_before" = "0" ]; then
    log "rien à supprimer (base déjà propre)."
    exit 0
fi

if [ "$DRY_RUN" = "1" ]; then
    psql_run <<'SQL'
SELECT 'batch' AS table_name, count(*) FROM batch WHERE batch_id LIKE 'LOT-DEMO-%'
UNION ALL SELECT 'harvest_event',      count(*) FROM harvest_event      WHERE harvest_id LIKE 'HE-DEMO-%'
UNION ALL SELECT 'raw_intake',         count(*) FROM raw_intake         WHERE intake_id LIKE 'RI-DEMO-%'
UNION ALL SELECT 'pt_equipment',       count(*) FROM pt_equipment       WHERE equipment_id LIKE 'EQ-DEMO-%'
UNION ALL SELECT 'qc_checkpoint',      count(*) FROM qc_checkpoint      WHERE checkpoint_id LIKE 'QC-DEMO-%'
UNION ALL SELECT 'drying_run',         count(*) FROM drying_run         WHERE run_id LIKE 'DR-DEMO-%'
UNION ALL SELECT 'wash_sort_record',   count(*) FROM wash_sort_record   WHERE record_id LIKE 'WS-DEMO-%'
UNION ALL SELECT 'packaging_record',   count(*) FROM packaging_record   WHERE record_id LIKE 'PKG-DEMO-%'
UNION ALL SELECT 'compliance_record',  count(*) FROM compliance_record  WHERE record_id LIKE 'CR-DEMO-%'
UNION ALL SELECT 'operator',           count(*) FROM operator           WHERE operator_id LIKE 'OP-DEMO-%'
UNION ALL SELECT 'historical_harvest', count(*) FROM historical_harvest WHERE weather_condition = 'DEMO';
SQL
    log "--dry-run : aucune suppression effectuée."
    exit 0
fi

# ---------------------------------------------------------------------------
# Suppression (une seule transaction)
# ---------------------------------------------------------------------------
log "suppression des lignes DEMO"
psql_run <<'SQL'
BEGIN;

DELETE FROM compliance_record WHERE record_id LIKE 'CR-DEMO-%';
DELETE FROM packaging_record  WHERE record_id LIKE 'PKG-DEMO-%';
DELETE FROM wash_sort_record  WHERE record_id LIKE 'WS-DEMO-%';
DELETE FROM drying_run        WHERE run_id LIKE 'DR-DEMO-%';
DELETE FROM qc_checkpoint     WHERE checkpoint_id LIKE 'QC-DEMO-%';
DELETE FROM raw_intake        WHERE intake_id LIKE 'RI-DEMO-%';
DELETE FROM harvest_event     WHERE harvest_id LIKE 'HE-DEMO-%';
DELETE FROM batch             WHERE batch_id LIKE 'LOT-DEMO-%';
DELETE FROM historical_harvest WHERE weather_condition = 'DEMO';

-- Données maîtres : uniquement si plus aucune ligne ne les référence.
DELETE FROM operator o
 WHERE o.operator_id LIKE 'OP-DEMO-%'
   AND NOT EXISTS (SELECT 1 FROM qc_checkpoint     q WHERE q.inspector_id = o.operator_id)
   AND NOT EXISTS (SELECT 1 FROM drying_run        d WHERE d.operator_id  = o.operator_id)
   AND NOT EXISTS (SELECT 1 FROM wash_sort_record  w WHERE w.operator_id  = o.operator_id)
   AND NOT EXISTS (SELECT 1 FROM packaging_record  p WHERE p.operator_id  = o.operator_id)
   AND NOT EXISTS (SELECT 1 FROM compliance_record c WHERE c.auditor_id   = o.operator_id);

DELETE FROM pt_equipment e
 WHERE e.equipment_id LIKE 'EQ-DEMO-%'
   AND NOT EXISTS (SELECT 1 FROM drying_run       d WHERE d.equipment_id = e.equipment_id)
   AND NOT EXISTS (SELECT 1 FROM wash_sort_record w WHERE w.equipment_id = e.equipment_id)
   AND NOT EXISTS (SELECT 1 FROM packaging_record p WHERE p.equipment_id = e.equipment_id);

COMMIT;
SQL

# ---------------------------------------------------------------------------
# État après
# ---------------------------------------------------------------------------
tx_after=$(total_tx)
master_after=$(total_master)
log "lignes DEMO restantes : $tx_after dans les tables de traitement, $master_after en données maîtres"

if [ "$master_after" != "0" ]; then
    operators=$(psql_run -tAc "SELECT count(*) FROM operator WHERE operator_id LIKE 'OP-DEMO-%'" | tr -d '[:space:]')
    equipment=$(psql_run -tAc "SELECT count(*) FROM pt_equipment WHERE equipment_id LIKE 'EQ-DEMO-%'" | tr -d '[:space:]')
    warn "$operators opérateur(s) et $equipment équipement(s) DEMO conservés :"
    warn "des lignes non-DEMO (lots LOT-GP-* du golden path, ou lots créés à la main) les référencent."
    warn "C'est sans effet : demo-up.sh les réutilise (ON CONFLICT DO NOTHING)."
fi

if [ "$tx_after" = "0" ]; then
    log "OK — base nettoyée, seules les lignes DEMO ont été retirées."
else
    die "il reste $tx_after ligne(s) DEMO dans les tables de traitement après nettoyage."
fi
