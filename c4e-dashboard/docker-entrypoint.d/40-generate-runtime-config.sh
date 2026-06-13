#!/bin/sh
set -eu

TZ_VALUE="${TZ:-America/Mexico_City}"
LOCALE_VALUE="${DASHBOARD_LOCALE:-}"
HOUR12_VALUE="${DASHBOARD_HOUR12:-}"

if [ -z "$LOCALE_VALUE" ]; then
  case "$TZ_VALUE" in
    Europe/*)
      LOCALE_VALUE="es-ES"
      ;;
    America/*)
      LOCALE_VALUE="es-MX"
      ;;
    *)
      LOCALE_VALUE="es-ES"
      ;;
  esac
fi

if [ -z "$HOUR12_VALUE" ]; then
  case "$TZ_VALUE" in
    Europe/*)
      HOUR12_VALUE="false"
      ;;
    America/*)
      HOUR12_VALUE="true"
      ;;
    *)
      HOUR12_VALUE="false"
      ;;
  esac
fi

cat >/usr/share/nginx/html/app-config.js <<EOF
window.__C4E_DASHBOARD_CONFIG__ = Object.freeze({
  timeZone: '${TZ_VALUE}',
  locale: '${LOCALE_VALUE}',
  hour12: ${HOUR12_VALUE}
});
EOF

