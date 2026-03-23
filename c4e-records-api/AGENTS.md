# AGENTS Guide (Com4Energy ecosystem)

## Scope
- This workspace is multi-repo; `c4e-records-api` is the runtime center and consumes local libs (`c4e-event-publisher`, `c4e-i18n-core`).
- `c4e-ingestion-service` and `dev-env/ghost-flows` are adjacent services/environments; align AMQP conventions before changing incident flows.

## Big picture architecture
- Core flow: producer services publish incidents via `Publisher.send(...)` (`c4e-event-publisher`), `c4e-records-api` dynamically consumes/persists them.
- Publisher contract is stable by ADR: depend on `Publisher` interface, not concrete classes (`docs/adr/0001-base-publisher-contract-send.md`).
- Incident routing config is unified under `c4e.incidents.types.*` (`docs/adr/0002-unified-incidents-prefix.md`, `src/main/resources/application.yml`).
- `c4e-records-api` is currently the single incident consumer with dynamic listeners (`docs/adr/0003-records-api-single-consumer-dynamic-listeners.md`, `messaging/incident/IncidentConsumer.java`).
- DB schema changes are managed via Liquibase (`spring.liquibase.change-log` in `src/main/resources/application.yml`).

## Build / test / run workflow
- Build shared libs first when changing contracts:
  - `cd /Users/jesus/Development/Com4Energy/c4e-i18n-core && mvn test install`
  - `cd /Users/jesus/Development/Com4Energy/c4e-event-publisher && mvn test install`
- Then run API:
  - `cd /Users/jesus/Development/Com4Energy/c4e-records-api && ./mvnw clean install`
  - `cd /Users/jesus/Development/Com4Energy/c4e-records-api && ./mvnw spring-boot:run`
- Fast health check from docs: `curl http://localhost:8082/actuator/health` (`QUICKSTART.md`).
- `c4e-ingestion-service` enforces Checkstyle in `verify`; run `./mvnw verify` before touching that repo.

## Code conventions specific to this codebase
- i18n is mandatory-by-pattern: use `Messages.get/format` + enum keys; avoid hardcoded user/log strings (`docs/I18N.md`, `c4e-i18n-core/docs/I18N_STANDARDS.md`).
- Keep message keys in per-project enums (`RecordsApiCommonMessageKey`, `IncidentPublisherMessageKey`) and `messages.properties` in sync.
- Lombok rule in `c4e-records-api/lombok.config`: `@Data` is prohibited; compose explicit Lombok annotations (ADR-0005).
- If multiple `Publisher` beans exist, use `@Qualifier("incidentPublisher")`; Lombok is configured to copy qualifier annotations (ADR-0004).

## Integration points and messaging rules
- Publisher side: `c4e-event-publisher/.../IncidentPublisher.java` resolves `IncidentType` -> exchange/routingKey from `c4e.incidents.types`.
- Consumer side: `c4e-records-api/.../IncidentConfig.java` declares queue/exchange/DLX/DLQ dynamically from same property tree.
- Adding an incident type is config-first: extend `c4e.incidents.types` and verify queue/exchange/DLQ creation in RabbitMQ.
- Use `c4e-records-api/RABBITMQ.md` runbook for DLQ handling and naming (`incident.<type>.queue|exchange|key|*.dlq|*.dlx`).

## Useful anchors
- ADR index: `c4e-records-api/docs/adr/README.md`
- API quick start: `c4e-records-api/QUICKSTART.md`
- i18n contributor checklist: `c4e-i18n-core/docs/CONTRIBUTOR_CHECKLIST.md`
- Ghost flows env note: `dev-env/ghost-flows/docker-compose.yml` maps host `.env` and backup volumes; README is minimal, inspect code (for example `src/main/java/com/com4energy/jobs/service/DatabaseService.java`).

