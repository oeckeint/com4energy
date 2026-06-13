#!/bin/sh
# Generates /root/.my.cnf from environment variables (single source: root .env)
# so mysqldump (used by DatabaseService) can authenticate without depending on a
# host-mounted credentials file. Keeps credentials in sync with rotations.
set -e

umask 077
cat > /root/.my.cnf <<EOF
[client]
host=${DB_HOST:-database}
port=3306
user=${DB_USER_SGE}
password=${DB_PASSWORD_SGE}
EOF

exec java -jar /app/app.jar
