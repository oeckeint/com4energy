# c4e-event-publisher

Libreria compartida para publicar eventos entre microservicios Spring Boot.

## Que incluye

- Contratos AMQP por dominio, por ejemplo `incident.contract`.
- Publisher comun: `EventPublisher`.
- Auto-configuracion Spring Boot para crear `EventPublisher` automaticamente.

## Configuracion minima

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

c4e:
  incidents:
    enabled: true
    types:
      validation:
        exchange: incident.validation.exchange
        routingKey: incident.validation.key
      integration:
        exchange: incident.integration.exchange
        routingKey: incident.integration.key
      system:
        exchange: incident.system.exchange
        routingKey: incident.system.key
```

## Uso rapido

```java
@Service
@RequiredArgsConstructor
public class AnyService {

    private final EventPublisher eventPublisher;

    public void report(IncidentEvent event) {
        eventPublisher.send(IncidentType.INTEGRATION, event);
    }
}
```

## Publicar artefacto localmente

```bash
cd c4e-event-publisher
../mvnw clean install
```

