#!/usr/bin/env bash
# =============================================================================
# Product Transformation — golden path de bout en bout
#
# Déroule le parcours métier complet sur l'API PT et vérifie chaque étape :
#
#   créer un lot → réception → lavage/tri → QC WASHING → séchage
#   → QC COOLING → conditionnement → conformité → COMPLETED → SHIPPED
#
# Le script échoue au premier code HTTP non 2xx ou à la première assertion
# fausse, et indique l'étape fautive.
#
# Usage :
#   ./scripts/pt-golden-path.sh                 # http://localhost:8080
#   BASE=http://localhost:18080 ./scripts/pt-golden-path.sh
#
# Les lignes créées ne sont PAS supprimées : leur identifiant porte le suffixe
# du run (RUN_ID) pour rester identifiables et ne pas entrer en collision avec
# les données de démonstration.
# =============================================================================
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
BATCH_ID="LOT-GP-${RUN_ID}"
TODAY="$(date +%F)"
NOW="$(date +%Y-%m-%dT%H:%M:%S)"

RESP=""

fail() {
  echo ""
  echo "ÉCHEC — $1" >&2
  exit 1
}

# api <METHOD> <PATH> [JSON_BODY]
api() {
  local method="$1" path="$2" body="${3:-}"
  local raw code

  if [ -n "$body" ]; then
    raw=$(curl -sS -X "$method" "$BASE$path" \
      -H 'Content-Type: application/json' -d "$body" \
      -w $'\n%{http_code}' --max-time 30)
  else
    raw=$(curl -sS -X "$method" "$BASE$path" \
      -w $'\n%{http_code}' --max-time 30)
  fi

  code=$(printf '%s' "$raw" | tail -n1)
  RESP=$(printf '%s' "$raw" | sed '$d')

  case "$code" in
    2??) printf '  OK   %-3s %s\n' "$code" "$method $path" ;;
    *)
      echo "  KO   $code $method $path" >&2
      echo "  réponse: $RESP" >&2
      fail "$method $path a renvoyé HTTP $code"
      ;;
  esac
}

# jq_field <expression> <attendu> <description>
jq_field() {
  local expr="$1" expected="$2" label="$3" actual
  actual=$(printf '%s' "$RESP" | jq -r "$expr" 2>/dev/null || echo '<illisible>')
  if [ "$actual" != "$expected" ]; then
    fail "$label — attendu « $expected », obtenu « $actual »"
  fi
  printf '       %s = %s\n' "$label" "$actual"
}

echo "============================================================"
echo " Golden path Product Transformation"
echo " BASE    : $BASE"
echo " RUN_ID  : $RUN_ID"
echo " Lot     : $BATCH_ID"
echo "============================================================"

# --- 0. disponibilité -------------------------------------------------------
echo "[0/12] disponibilité du backend"
api GET /actuator/health
jq_field '.status' 'UP' 'health.status'

# --- 1. création du lot -----------------------------------------------------
echo "[1/12] création du lot"
api POST /api/batches "$(cat <<JSON
{
  "batchId": "$BATCH_ID",
  "harvestDate": "$TODAY",
  "mangoVariety": "KEITT",
  "harvestQuantityKg": 500.00,
  "farmId": "FERME-GP",
  "blockId": "BLOC-GP"
}
JSON
)"
jq_field '.batchId' "$BATCH_ID" 'batchId'
jq_field '.currentStatus' 'CREATED' 'currentStatus'

# --- 2. réception -----------------------------------------------------------
echo "[2/12] réception (raw intake)"
api POST /api/raw-intakes "$(cat <<JSON
{
  "intakeId": "INT-GP-${RUN_ID}",
  "batchId": "$BATCH_ID",
  "sourceFarm": "FERME-GP",
  "sourceBlock": "BLOC-GP",
  "intakeDate": "$TODAY",
  "receivedQuantityKg": 495.00,
  "receivedVariety": "KEITT",
  "receivedGrade": "A"
}
JSON
)"

echo "[3/12] passage CREATED → INTAKE"
api POST "/api/batches/$BATCH_ID/advance-status" '{}'
jq_field '.currentStatus' 'INTAKE' 'currentStatus'

echo "[4/12] passage INTAKE → WASHING"
api POST "/api/batches/$BATCH_ID/advance-status" '{}'
jq_field '.currentStatus' 'WASHING' 'currentStatus'

# --- 3. lavage / tri --------------------------------------------------------
echo "[5/12] lavage / tri"
api POST /api/wash-sort-records "$(cat <<JSON
{
  "recordId": "WS-GP-${RUN_ID}",
  "batchId": "$BATCH_ID",
  "inputQuantityKg": 495.00,
  "outputQuantityKg": 420.00,
  "wasteQuantityKg": 75.00,
  "waterUsageLiters": 900.00,
  "startTime": "$NOW",
  "endTime": "$NOW"
}
JSON
)"

# --- 4. QC WASHING (obligatoire avant DRYING) -------------------------------
echo "[6/12] contrôle qualité WASHING (obligatoire avant séchage)"
api POST /api/qc-checkpoints "$(cat <<JSON
{
  "checkpointId": "QC-WASH-GP-${RUN_ID}",
  "batchId": "$BATCH_ID",
  "stage": "WASHING",
  "result": "PASS",
  "defects": "aucun",
  "defectsCount": 0,
  "checkpointTime": "$NOW"
}
JSON
)"

echo "[7/12] passage WASHING → DRYING"
api POST "/api/batches/$BATCH_ID/advance-status" '{}'
jq_field '.currentStatus' 'DRYING' 'currentStatus'

# --- 5. séchage -------------------------------------------------------------
echo "[8/12] séchage"
api POST /api/drying-runs "$(cat <<JSON
{
  "runId": "DRY-GP-${RUN_ID}",
  "batchId": "$BATCH_ID",
  "durationHours": 18.00,
  "targetTemperatureC": 55.00,
  "actualTemperatureC": 54.50,
  "startMoisturePct": 82.00,
  "endMoisturePct": 12.00,
  "energyUsageKwh": 145.00,
  "startTime": "$NOW",
  "endTime": "$NOW"
}
JSON
)"

# --- 6. QC COOLING (obligatoire avant PACKAGING) ----------------------------
echo "[9/12] contrôle qualité COOLING (obligatoire avant conditionnement)"
api POST /api/qc-checkpoints "$(cat <<JSON
{
  "checkpointId": "QC-COOL-GP-${RUN_ID}",
  "batchId": "$BATCH_ID",
  "stage": "COOLING",
  "result": "PASS",
  "defects": "aucun",
  "defectsCount": 0,
  "checkpointTime": "$NOW"
}
JSON
)"

echo "[10/12] passage DRYING → PACKAGING"
api POST "/api/batches/$BATCH_ID/advance-status" '{}'
jq_field '.currentStatus' 'PACKAGING' 'currentStatus'

# --- 7. conditionnement + conformité ----------------------------------------
echo "[11/12] conditionnement et conformité"
api POST /api/packaging-records "$(cat <<JSON
{
  "recordId": "PKG-GP-${RUN_ID}",
  "batchId": "$BATCH_ID",
  "packageType": "ONE_KG_BAG",
  "packageQuantityKg": 95.00,
  "lotCode": "LOTCODE-GP-${RUN_ID}",
  "exportReady": true,
  "packagingDate": "$TODAY"
}
JSON
)"

api POST /api/compliance-records "$(cat <<JSON
{
  "recordId": "COMP-GP-${RUN_ID}",
  "batchId": "$BATCH_ID",
  "complianceType": "HACCP",
  "requirement": "Chaîne du froid et hygiène vérifiées",
  "result": "COMPLIANT",
  "evidence": "Rapport QC-GP-${RUN_ID}",
  "auditDate": "$TODAY"
}
JSON
)"

# --- 8. clôture -------------------------------------------------------------
echo "[12/12] clôture : PACKAGING → COMPLETED → SHIPPED"
api POST "/api/batches/$BATCH_ID/advance-status" '{}'
jq_field '.currentStatus' 'COMPLETED' 'currentStatus'

api POST "/api/batches/$BATCH_ID/advance-status" '{}'
jq_field '.currentStatus' 'SHIPPED' 'currentStatus'

# --- vérification finale en relecture --------------------------------------
echo "vérification finale par relecture"
api GET "/api/batches/$BATCH_ID"
jq_field '.currentStatus' 'SHIPPED' 'currentStatus (relu)'

echo ""
echo "============================================================"
echo " GOLDEN PATH OK — lot $BATCH_ID terminé en SHIPPED"
echo "============================================================"
