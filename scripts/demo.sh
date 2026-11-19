#!/usr/bin/env bash
# Run Sustainable Farm locally with the demo data set, in one command.
#
#   scripts/demo.sh           build and start PostgreSQL, the backend and the frontend, demo data loaded
#   scripts/demo.sh reset     empty the database, replay the migration and reload the demo data
#   scripts/demo.sh stop      stop the stack (the data is kept)
#   scripts/demo.sh status    show the containers
#   scripts/demo.sh logs      follow the backend logs
#
# The demo data comes from one file: backend/src/main/resources/seed/demo-data.sql. It is loaded by
# the backend in the "dev" profile when the database has no farm yet. Edit the file, then run
# "scripts/demo.sh reset" to see the change.
#
# Needs Docker with the compose plugin. Settings (ports, database credentials) come from .env,
# created from .env.example on the first run.
set -euo pipefail

cd "$(dirname "$0")/.."

if ! docker compose version >/dev/null 2>&1; then
  echo "Docker with the compose plugin is required: https://docs.docker.com/compose/install/" >&2
  exit 1
fi

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env from .env.example (local placeholder values)."
fi

env_value() {
  local value
  value=$(grep -E "^$1=" .env | tail -1 | cut -d= -f2- || true)
  echo "${value:-$2}"
}

BACKEND_PORT=$(env_value BACKEND_PORT 8080)
FRONTEND_PORT=$(env_value FRONTEND_PORT 3000)

# The demo always runs the "dev" profile, whatever .env says, so the demo data is loaded.
export SPRING_PROFILES_ACTIVE=dev

wait_until_ready() {
  echo "Waiting for the backend (migration and demo data)..."
  for _ in $(seq 1 90); do
    if curl -fs "http://localhost:${BACKEND_PORT}/actuator/health" >/dev/null 2>&1 &&
       curl -fs "http://localhost:${FRONTEND_PORT}/" >/dev/null 2>&1; then
      echo
      echo "Sustainable Farm is running:"
      echo "  Application   http://localhost:${FRONTEND_PORT}"
      echo "  API           http://localhost:${BACKEND_PORT}/api"
      echo "  API docs      http://localhost:${BACKEND_PORT}/swagger-ui.html"
      echo
      echo "Stop it with: scripts/demo.sh stop   Start again from empty data: scripts/demo.sh reset"
      return 0
    fi
    sleep 2
  done
  echo "The stack did not become ready in 3 minutes. Backend logs:" >&2
  docker compose logs --tail 50 backend >&2
  return 1
}

case "${1:-up}" in
  up)
    docker compose up -d --build
    wait_until_ready
    ;;
  reset)
    docker compose down -v
    docker compose up -d --build
    wait_until_ready
    ;;
  stop)
    docker compose down
    ;;
  status)
    docker compose ps
    ;;
  logs)
    docker compose logs -f backend
    ;;
  *)
    echo "Usage: scripts/demo.sh [up|reset|stop|status|logs]" >&2
    exit 1
    ;;
esac
