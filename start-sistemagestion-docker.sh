#!/usr/bin/env bash
set -euo pipefail

# Usa el docker-compose maestro en Com4Energy, no el local
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$SCRIPT_DIR"
COMPOSE_FILE="${ROOT_DIR}/docker-compose.yml"

log() {
  printf '[docker-stack] %s\n' "$1"
}

fail() {
  printf '[docker-stack][error] %s\n' "$1" >&2
  exit 1
}

resolve_compose_cmd() {
  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=(docker compose)
  elif command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=(docker-compose)
  else
    fail "No se encontro docker compose en PATH"
  fi
}

ensure_prerequisites() {
  [[ -f "$COMPOSE_FILE" ]] || fail "No existe ${COMPOSE_FILE}"
  command -v docker >/dev/null 2>&1 || fail "No se encontro docker en PATH"
  docker info >/dev/null 2>&1 || fail "Docker daemon no disponible"
}

main() {
  resolve_compose_cmd
  ensure_prerequisites

  cd "$ROOT_DIR"
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" config >/dev/null

  log "Ejecutando Docker desde: ${COMPOSE_FILE}"
  log "Ejecutando: docker compose down && docker compose build && docker compose up"
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" down && \
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" build && \
  "${COMPOSE_CMD[@]}" -f "$COMPOSE_FILE" up
}

main "$@"

