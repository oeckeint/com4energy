# dev-env/database/Dockerfile
FROM mysql:8.0.40

# Opcional: copiar scripts de inicialización si quieres
COPY ./scripts/backup_20250816_001617.sql /docker-entrypoint-initdb.d/sge_backup.sql

# Exponer el puerto (opcional, docker-compose también lo hace)
EXPOSE 3306
