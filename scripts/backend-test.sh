#!/usr/bin/env bash
#
# backend-test.sh : run the backend tests (Maven, JDK 21).
#
# - With `java` on PATH: runs ./mvnw in backend/.
# - Without it: runs Maven in the Docker image maven:3.9-eclipse-temurin-21, on a
#   copy of backend/ (the work tree stays untouched), with the named volume
#   sf-m2 as the Maven cache.
#
# Database: the H2 test configuration of backend/src/test/resources by default
# (fast). SF_TEST_PG=1 runs them against a throwaway PostgreSQL 17 container,
# like the CI workflow (needs Docker in both modes).
#
# Usage: scripts/backend-test.sh [maven args...]   (default: -q -B test)

set -euo pipefail

REPO_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BACKEND="$REPO_ROOT/backend"
MAVEN_IMAGE="maven:3.9-eclipse-temurin-21"
PG_IMAGE="postgres:17"

if [ "$#" -gt 0 ]; then ARGS=("$@"); else ARGS=(-q -B test); fi

PG_NAME=""
NETWORK=""
cleanup() {
    [ -n "$PG_NAME" ] && docker rm -f "$PG_NAME" >/dev/null 2>&1 || true
    [ -n "$NETWORK" ] && docker network rm "$NETWORK" >/dev/null 2>&1 || true
}
trap cleanup EXIT

wait_for_pg() {
    for _ in $(seq 1 60); do
        if docker exec "$PG_NAME" pg_isready -U farm_admin -d sustainable_farm_test >/dev/null 2>&1; then
            sleep 2
            return 0
        fi
        sleep 1
    done
    echo "backend-test: PostgreSQL did not become ready" >&2
    return 1
}

PG_ENV=(
    SPRING_DATASOURCE_USERNAME=farm_admin
    SPRING_DATASOURCE_PASSWORD=changeme
    SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
)

if command -v java >/dev/null 2>&1; then
    if [ "${SF_TEST_PG:-0}" = "1" ]; then
        PG_NAME="sf-test-pg-$$"
        docker run -d --rm --name "$PG_NAME" -p 127.0.0.1::5432 \
            -e POSTGRES_DB=sustainable_farm_test -e POSTGRES_USER=farm_admin \
            -e POSTGRES_PASSWORD=changeme "$PG_IMAGE" >/dev/null
        wait_for_pg
        port=$(docker port "$PG_NAME" 5432/tcp | head -1 | sed 's/.*://')
        export SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:$port/sustainable_farm_test"
        for kv in "${PG_ENV[@]}"; do export "${kv?}"; done
    fi
    cd "$BACKEND"
    ./mvnw "${ARGS[@]}"
    exit $?
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "backend-test: neither java nor docker is available" >&2
    exit 1
fi

DOCKER_ARGS=()
if [ "${SF_TEST_PG:-0}" = "1" ]; then
    NETWORK="sf-test-net-$$"
    PG_NAME="sf-test-pg-$$"
    docker network create "$NETWORK" >/dev/null
    docker run -d --rm --name "$PG_NAME" --network "$NETWORK" \
        -e POSTGRES_DB=sustainable_farm_test -e POSTGRES_USER=farm_admin \
        -e POSTGRES_PASSWORD=changeme "$PG_IMAGE" >/dev/null
    wait_for_pg
    DOCKER_ARGS+=(--network "$NETWORK"
        -e "SPRING_DATASOURCE_URL=jdbc:postgresql://$PG_NAME:5432/sustainable_farm_test")
    for kv in "${PG_ENV[@]}"; do DOCKER_ARGS+=(-e "$kv"); done
fi

# Maven runs on a copy so that root-owned target/ files never land in the work tree.
docker run --rm "${DOCKER_ARGS[@]}" \
    -v sf-m2:/root/.m2 \
    -v "$BACKEND":/src:ro \
    "$MAVEN_IMAGE" \
    bash -c 'cp -r /src /tmp/backend && cd /tmp/backend && rm -rf target && mvn "$@"' \
    backend-test "${ARGS[@]}"
