# RabbitMQ - Regla para exchanges compartidos

## Objetivo
Documentar la regla que deben seguir los nuevos consumers/producers cuando reutilizan un exchange RabbitMQ que ya existe en la plataforma.

## Regla principal
Si un microservicio declara un **exchange compartido** que ya existe en RabbitMQ, debe usar **exactamente el mismo tipo** de exchange.

Ejemplos de tipos de exchange:
- `topic`
- `direct`
- `fanout`
- `headers`

RabbitMQ **no permite** redeclarar un exchange existente con un tipo distinto.

## Error típico
Cuando un servicio intenta declarar un exchange con otro tipo, RabbitMQ devuelve un error como este:

```text
PRECONDITION_FAILED - inequivalent arg 'type' for exchange 'c4e.exchange' in vhost '/': received 'direct' but current is 'topic'
```

## Caso real en `records-api`
El exchange compartido `c4e.exchange` ya existía en RabbitMQ como `topic`.

Por eso, en `records-api` los consumers que usen ese exchange deben declararlo como:

```java
TopicExchange mainExchange = new TopicExchange(config.getExchange());
```

Y no como:

```java
DirectExchange mainExchange = new DirectExchange(config.getExchange());
```

## ¿Todos los próximos consumers deben seguir esta regla?
### Sí, si reutilizan un exchange ya existente
Si el nuevo consumer usa un exchange compartido ya creado por otro servicio o por infraestructura:
- debe respetar el **mismo nombre**
- debe respetar el **mismo tipo**
- debe respetar la semántica esperada de `routingKey`

### No necesariamente, si crean un exchange nuevo
Si el nuevo flujo crea un exchange nuevo y propio, entonces se puede elegir el tipo más adecuado (`topic`, `direct`, etc.).

## Regla práctica de diseño
### Usa `TopicExchange` cuando:
- el exchange ya existe como `topic`
- quieres poder usar patrones como `c4e.file.*` o `c4e.#`
- quieres mantener flexibilidad futura sin romper exact matches

### Usa `DirectExchange` cuando:
- el exchange es nuevo
- solo necesitas coincidencia exacta por `routingKey`
- el contrato del broker está controlado por este mismo módulo

## Importante: `topic` también sirve para claves exactas
Un `TopicExchange` no obliga a usar comodines.

También puede trabajar con una routing key exacta:

- routing key publicada: `c4e.file.rejected`
- binding: `c4e.file.rejected`

Eso funciona correctamente.

## Estado actual en `records-api`
### Consumers de incidentes
En `messaging/incident/IncidentConfig.java`:
- exchange principal: `TopicExchange`
- DLX: `TopicExchange`

### Consumers de file records
En `messaging/filerecord/FileRecordConfig.java`:
- exchange principal: `TopicExchange`
- DLX: `DirectExchange`

> Nota: el DLX puede ser distinto del exchange principal, siempre que no exista previamente en RabbitMQ con otro tipo incompatible.

## Checklist para crear un nuevo consumer RabbitMQ
Antes de declarar un exchange nuevo o reutilizado, revisar:

- [ ] ¿El exchange ya existe en RabbitMQ?
- [ ] Si existe, ¿qué tipo tiene realmente (`topic`, `direct`, etc.)?
- [ ] ¿La clase Spring AMQP usada coincide con ese tipo?
- [ ] ¿La `routingKey` que publica el emisor coincide con el binding del consumer?
- [ ] ¿La DLQ/DLX también respeta el tipo real si ya existía?

## Recomendación para este proyecto
Para `records-api`, si el consumer va a escuchar mensajes publicados a un exchange compartido de Com4Energy:
1. mirar primero cómo lo declara el servicio emisor
2. confirmar el tipo real del exchange en RabbitMQ
3. usar el mismo tipo en el consumer
4. documentar la nueva topología si introduce una convención nueva

