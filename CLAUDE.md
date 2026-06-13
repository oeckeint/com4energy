# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Com4Energy is a Java/Spring Boot microservices monorepo for energy measurement management. It uses a shared Maven parent POM (`com4energy-parent`, version 1.0.0, Spring Boot 3.5.4, Java 21).

## Modules

| Module | Port | Purpose |
|---|---|---|
| `c4e-records-api` | 8082 | Central data API — CRUD for energy measurements (MedidaQH, MedidaH, MedidaCCH), source of truth |
| `c4e-ingestion-service` | 8083 | File ingestion — receives multipart uploads, moves files through pipeline, publishes to RabbitMQ |
| `c4e-outbox-worker` | 8090 | Outbox pattern — polls `outbox_event` table and publishes to RabbitMQ |
| `ghost-flows` | 8084 | Background jobs (scheduled tasks, DB backup triggers) |
| `sistemagestion` | 7001 | Legacy Tomcat WAR frontend (JSP-based) |
| `c4e-event-publisher` | — | Shared library — `EventPublisher` with AMQP contracts per domain |
| `c4e-i18n-core` | — | Shared library — `MessageKey` interface + `Messages` resolver |
| `c4e-dashboard` | 4200 | Angular 21 + Bootstrap 5 + Chart.js frontend |

## Common Commands

### Java Modules (run from each module directory)

```bash
sdk env                        # activate correct Java/Maven via SDKMAN (java=21.0.9-tem, maven=3.9.12)
./mvnw clean test              # run tests
./mvnw verify                  # tests + checkstyle (required before committing in c4e-ingestion-service)
./mvnw spring-boot:run         # start locally
./mvnw clean install -DskipTests  # build without tests
```

### Angular Dashboard

```bash
cd c4e-dashboard
npm install
npm start                      # dev server with proxy (proxies to backend services)
npm run build                  # production build
npm test                       # vitest
```

### Full Stack with Docker

```bash
# From repo root — requires .env to be configured
docker-compose up -d           # start all services
docker-compose logs -f <service>
docker-compose down
```

## Environment Variables

All services share these required variables (set in `.env` at root):

```bash
DB_URL_SGE            # jdbc:mysql://localhost:3306/sge
DB_USER_SGE
DB_PASSWORD_SGE
SGE_LOCAL_DB_NAME=sge
RABBITMQ_USER / RABBITMQ_PASSWORD
TZ                    # e.g. America/Mexico_City
SPRING_PROFILES_ACTIVE  # e.g. dev
C4E_HOST_STORAGE_ROOT   # file storage root path
```

For `c4e-ingestion-service` locally: `export C4E_HOST_STORAGE_ROOT="$HOME/Downloads/com4energy"`

## Architecture

### Data Flow

```
c4e-ingestion-service          → RabbitMQ → c4e-records-api (saves to MySQL)
sistemagestion / dashboard      → REST   → c4e-records-api
c4e-records-api (outbox table)  → c4e-outbox-worker → RabbitMQ
ghost-flows                     → scheduled jobs, DB backups
```

### c4e-records-api Package Layout

The main package is `com.com4energy.recordsapi`. Follows layered architecture:
- `controller/` — REST controllers (MedidaQH, MedidaH, MedidaCCH, Cliente, Tarifa, FileRecord, Incident)
- `service/` — business logic
- `repository/` — Spring Data JPA repositories
- AOP aspects for logging, performance tracking, and audit

### Measurement Types

The core domain revolves around three measurement entities:
- `MedidaQH` — quarter-hourly measurements
- `MedidaH` — hourly measurements  
- `MedidaCCH` — CCH measurements

### Database Migrations

`c4e-records-api` uses **Liquibase** with SQL format. Migration files live in `src/main/resources/db/changelog/migrations/`. The master changelog is `db.changelog-master.xml`. Naming convention: `NNN_TICKET-ID_description.sql`.

### Shared Libraries

`c4e-i18n-core` and `c4e-event-publisher` must be installed to local Maven before building dependent services:

```bash
cd c4e-i18n-core && ./mvnw clean install
cd c4e-event-publisher && ./mvnw clean install
```

### c4e-ingestion-service Feature Flags

All major behaviors are toggled via `app.feature.enabled.*`:
- `persist-data`, `send-messages`, `receive-messages`
- `file-scanner-job`, `file-processing-job`, `file-retry-job`

### Checkstyle

`c4e-ingestion-service` enforces Checkstyle on `./mvnw verify`. Max line length is 192. Rules are in `checkstyle.xml` (unused imports, file newline, etc.).
