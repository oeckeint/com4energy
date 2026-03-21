# Estándares de Internacionalización (i18n) - Com4Energy

Documento que define los estándares y convenciones para usar el pipeline i18n en todos los microservicios de Com4Energy.

---

## Tabla de Contenidos

1. [Arquitectura General](#arquitectura-general)
2. [Crear un MessageKey](#crear-un-messagekey)
3. [Convenciones de Naming](#convenciones-de-naming)
4. [Organización de Claves](#organización-de-claves)
5. [Bundles de Propiedades](#bundles-de-propiedades)
6. [Uso en el Código](#uso-en-el-código)
7. [Patrones por Proyecto](#patrones-por-proyecto)
8. [Buenas Prácticas](#buenas-prácticas)
9. [Migración de Hardcodeados](#migración-de-hardcodeados)

---

## Arquitectura General

```
c4e-i18n-core (librería compartida)
├── MessageKey (interfaz)
├── Messages (utilidad estática)
└── messages.properties (claves base del core)

c4e-event-publisher (uses c4e-i18n-core)
├── common/
│   └── PublisherCommonMessageKey.java
├── incident/publisher/
│   └── IncidentPublisherMessageKey.java
└── messages.properties

c4e-records-api (uses c4e-i18n-core)
├── common/
│   └── RecordsApiCommonMessageKey.java
└── messages.properties
```

**Principio:** Cada proyecto tiene su propio enum de claves y su propio bundle de propiedades. El core solo define la interfaz y la utilidad.

---

## Crear un MessageKey

### Paso 1: Crear el Enum

**Ubicación:** `src/main/java/com/com4energy/{proyecto}/common/{Nombre}MessageKey.java`

```java
package com.com4energy.recordsapi.common;

import com.com4energy.i18n.core.MessageKey;

/**
 * Claves i18n para RecordsAPI.
 * Agrupa todos los mensajes del dominio de negocio específico.
 */
public enum RecordsApiCommonMessageKey implements MessageKey {

    // ---------------------------
    // DOMINIO / CONTEXTO
    // ---------------------------
    CLAVE_UNO("clave.uno"),
    CLAVE_DOS("clave.dos");

    private final String key;

    RecordsApiCommonMessageKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
```

### Paso 2: Agregar Entradas en Properties

**Ubicación:** `src/main/resources/messages.properties`

```properties
# ---------------------------
# DOMINIO / CONTEXTO
# ---------------------------
clave.uno=Mensaje parametrizado: {0}
clave.dos=Otro mensaje sin parámetros
```

### Paso 3: Usar en el Código

```java
import com.com4energy.i18n.core.Messages;

// Sin parámetros
String msg = Messages.get(RecordsApiCommonMessageKey.CLAVE_DOS);

// Con parámetros
String msg = Messages.format(RecordsApiCommonMessageKey.CLAVE_UNO, "valor");
```

---

## Convenciones de Naming

### Nombres de Claves (Properties)

**Formato:** `{dominio}.{subdominio}.{entidad}.{acción}`

| Contexto | Ejemplo | Explicación |
|----------|---------|-------------|
| **Errores generales** | `error.unexpected.no_param` | Errores comunes del sistema |
| **Dominio específico** | `incident.event.id.empty` | Validación en dominio "incident" |
| **Recursos** | `medidaqh.not.found` | Entidad MedidaQH no encontrada |
| **UI/Headers** | `header.info.message` | Cabeceras de respuesta HTTP |
| **Validación** | `validation.email.invalid` | Error de validación de email |

**Reglas:**
- ✅ Usar puntos (`.`) como separadores
- ✅ Nombres en minúsculas
- ✅ Prefijo con nombre del contexto (`incident.`, `medida.`, `publisher.`)
- ❌ Usar guiones o underscores (excepto `no_param` para casos específicos)
- ❌ Nombres genéricos sin contexto (`error`, `message` solito)

### Nombres de Enums Java

**Formato:** `{Prefijo}MessageKey`

| Proyecto | Enum | Ubicación |
|----------|------|-----------|
| `c4e-records-api` | `RecordsApiCommonMessageKey` | `common/` |
| `c4e-event-publisher` | `PublisherCommonMessageKey` | `common/` |
| `c4e-event-publisher` | `IncidentPublisherMessageKey` | `incident/publisher/` |

**Constantes del enum (en UPPER_SNAKE_CASE):**

```java
public enum IncidentPublisherMessageKey implements MessageKey {
    INCIDENT_TYPE_NOT_CONFIGURED("incident.publisher.type.not.configured"),
    CONNECTION_TIMEOUT("publisher.connection.timeout");
    // ...
}
```

---

## Organización de Claves

### Agrupar por Contexto

Usar comentarios para organizar enums grandes:

```java
public enum RecordsApiCommonMessageKey implements MessageKey {

    // ---------------------------
    // GENERAL / SYSTEM
    // ---------------------------
    SYSTEM_ERROR("system.error"),
    ERROR_UNEXPECTED_NO_PARAM("error.unexpected.no_param"),

    // ---------------------------
    // INCIDENT DOMAIN
    // ---------------------------
    INCIDENT_SAVED("incident.saved"),
    INCIDENT_LOG_NOT_FOUND("incident.log.not.found"),

    // ---------------------------
    // MEDIDAQH DOMAIN
    // ---------------------------
    MEDIDA_NOT_FOUND("medidaqh.not.found");

    // ... constructor e implementación
}
```

### Enums por Módulo (Escalabilidad)

Si un enum crece a **>20 claves**, considerar separar por módulo:

```
c4e-event-publisher/
├── common/
│   └── PublisherCommonMessageKey.java     (claves compartidas)
├── incident/publisher/
│   └── IncidentPublisherMessageKey.java   (solo claves incident)
└── alert/publisher/
    └── AlertPublisherMessageKey.java      (solo claves alert)
```

---

## Bundles de Propiedades

### Estructura Actual

**Un bundle global por proyecto:**

```
c4e-records-api/src/main/resources/messages.properties
c4e-event-publisher/src/main/resources/messages.properties
```

**Contenido:**

```properties
# En cada archivo, todas las claves de ese proyecto
# Organizadas por secciones con comentarios
```

### Estructura Futura (Si escala mucho)

Si el archivo crece a **>100 líneas**, se puede separar:

```
c4e-records-api/src/main/resources/
├── messages.properties                    (claves base)
├── messages_incident.properties           (claves incident)
└── messages_medida.properties             (claves medida)
```

Para esto, necesitaría refactorizar `Messages.class` en `c4e-i18n-core`.

**Por ahora:** Usar un bundle único.

---

## Uso en el Código

### Patrón 1: Lectura Simple

```java
import com.com4energy.i18n.core.Messages;
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;

String text = Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS);
// → "Utility class"
```

### Patrón 2: Con Parámetros

```java
String msg = Messages.format(RecordsApiCommonMessageKey.USER_NOT_FOUND, userId);
// Clave: "user.not.found=User with id {0} was not found"
// Resultado: "User with id 123 was not found"
```

### Patrón 3: En Excepciones

```java
throw new ResourceNotFoundException(
    Messages.format(RecordsApiCommonMessageKey.INCIDENT_LOG_NOT_FOUND, id)
);
```

### Patrón 4: En Logging

```java
log.warn(Messages.format(
    RecordsApiCommonMessageKey.INCIDENT_PAYLOAD_JSON_PARSE_ERROR, 
    e.getMessage()
));
```

### ❌ Evitar

```java
// NO: String hardcodeado
log.warn("This is hardcoded");

// NO: Interpolación manual
String msg = "User " + userId + " not found";

// NO: Mezclar claves de diferentes enums sin necesidad
Messages.get(IncidentPublisherMessageKey.SOMETHING);  // Si no está en IncidentPublisher
```

---

## Patrones por Proyecto

### c4e-i18n-core

**Rol:** Librería compartida (NO contiene claves de negocio).

**Estructura:**

```
c4e-i18n-core/
├── src/main/java/com/com4energy/i18n/core/
│   ├── MessageKey.java          (interfaz)
│   └── Messages.java            (utilidad)
├── src/main/resources/
│   └── messages.properties      (solo claves core: utility.class.message)
└── README.md, I18N_STANDARDS.md
```

**Contenido de messages.properties:**

```properties
# Solo claves técnicas/compartidas
utility.class.message=Utility class
```

---

### c4e-event-publisher

**Rol:** Librería de publicación de eventos.

**Estructura:**

```
c4e-event-publisher/
├── src/main/java/com/com4energy/event/publisher/
│   ├── common/
│   │   └── PublisherCommonMessageKey.java    (claves compartidas entre publishers)
│   ├── incident/publisher/
│   │   └── IncidentPublisherMessageKey.java  (solo claves incident)
│   └── alert/publisher/ (futuro)
│       └── AlertPublisherMessageKey.java
├── src/main/resources/
│   └── messages.properties
└── README.md
```

**Convención de claves:**

```
incident.publisher.*       → Claves específicas de IncidentPublisher
alert.publisher.*          → Claves específicas de AlertPublisher (futuro)
publisher.*                → Claves compartidas (si las hay)
```

**Ejemplo:**

```java
// PublisherCommonMessageKey (vacío por ahora, ready para futuro)
public enum PublisherCommonMessageKey implements MessageKey {
    // RESERVED FOR SHARED PUBLISHER KEYS
}

// IncidentPublisherMessageKey
public enum IncidentPublisherMessageKey implements MessageKey {
    INCIDENT_TYPE_NOT_CONFIGURED("incident.publisher.type.not.configured");
}
```

---

### c4e-records-api

**Rol:** API de dominio (contiene claves de negocio).

**Estructura:**

```
c4e-records-api/
├── src/main/java/com/com4energy/recordsapi/
│   ├── common/
│   │   └── RecordsApiCommonMessageKey.java   (todas las claves)
│   ├── service/
│   ├── controller/
│   └── mapper/
├── src/main/resources/
│   └── messages.properties
└── README.md
```

**Convención de claves:**

```
incident.*                 → Validaciones y errores de incident
medidaqh.*                 → Validaciones y errores de medida
user.*                     → Errores relacionados a usuarios
error.*                    → Errores generales
validation.*               → Validaciones genéricas
```

---

## Buenas Prácticas

### 1. Naming Predecible

✅ **Bueno:**
- `incident.saved` (qué pasó)
- `incident.log.not.found` (qué no se encontró)
- `validation.email.invalid` (qué validó y qué falló)

❌ **Malo:**
- `error` (demasiado genérico)
- `something_went_wrong` (poco descriptivo)
- `USER_VALIDATION_FAILED_DUE_TO_EMAIL` (demasiado largo)

### 2. Documentar Parámetros

```java
/**
 * {0} = userId
 * {1} = timestamp
 */
INCIDENT_SAVED("incident.saved")
```

### 3. Evitar Duplicación

Si la misma clave se usa en múltiples lugares:
- ✅ Poner en enum global (`PublisherCommonMessageKey`, `RecordsApiCommonMessageKey`)
- ❌ Copiar en varios enums locales

### 4. Versioning

Si cambias una clave, considera:
- ¿Es breaking? (cambio de clave existente) → Requiere migración
- ¿Es aditivo? (nueva clave) → Safe
- ¿Es deprecado? (claves viejas no usadas) → Se pueden remover

### 5. Testing

Toda clave en el enum debe tener entrada en `messages.properties`:

```java
// En los tests, validar que no hay huérfanos
@Test
void shouldHavePropertiesForAllKeys() {
    for (RecordsApiCommonMessageKey key : RecordsApiCommonMessageKey.values()) {
        String msg = Messages.get(key);
        assertNotNull(msg, "Missing property for " + key.key());
    }
}
```

---

## Migración de Hardcodeados

### Proceso

1. **Identificar** strings hardcodeados en logs/excepciones
2. **Crear clave** en el enum apropiado
3. **Agregar entrada** en `messages.properties`
4. **Reemplazar** en el código
5. **Compilar y validar**

### Ejemplo Real

**Antes:**

```java
log.warn("Payload is not valid JSON, storing as plain text: {}", e.getMessage());
```

**Después:**

Paso 1: Enum
```java
public enum RecordsApiCommonMessageKey implements MessageKey {
    // ...
    INCIDENT_PAYLOAD_JSON_PARSE_ERROR("incident.payload.json.parse.error");
}
```

Paso 2: Properties
```properties
incident.payload.json.parse.error=Payload is not valid JSON, storing as plain text: {0}
```

Paso 3: Código
```java
log.warn(Messages.format(RecordsApiCommonMessageKey.INCIDENT_PAYLOAD_JSON_PARSE_ERROR, e.getMessage()));
```

---

## Preguntas Frecuentes

### ¿Dónde pongo una clave si se usa en múltiples proyectos?

→ En `c4e-i18n-core` si es técnica (ej: `utility.class.message`)  
→ En el proyecto "dueño" del dominio si es de negocio

### ¿Puedo reutilizar claves entre proyectos?

✅ Sí, pero importa el enum específico:

```java
// En c4e-event-publisher
import com.com4energy.recordsapi.common.RecordsApiCommonMessageKey;

Messages.format(RecordsApiCommonMessageKey.INCIDENT_LOG_NOT_FOUND, id);
```

⚠️ Cuidado: Esto crea acoplamiento. Mejor crear tu propia clave.

### ¿Y si necesito idiomas múltiples?

→ En Java, `ResourceBundle` soporta sufijos: `messages_es.properties`, `messages_en.properties`

Actualizaría `Messages.class` en c4e-i18n-core para aceptar locale.

### ¿Qué pasa si cambio una clave que ya está en producción?

→ Riesgo de logs/errores inconsistentes  
→ Mejor: mantener la clave antigua y crear una nueva (deprecated)

---

## Recursos

- [Guía de MessageKey en c4e-i18n-core](../README.md)
- [Java ResourceBundle Docs](https://docs.oracle.com/javase/8/docs/api/java/util/ResourceBundle.html)
- [MessageFormat Syntax](https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html)

---

## Control de Cambios

| Versión | Fecha | Cambios |
|---------|-------|---------|
| 1.0 | 2026-03-21 | Documento inicial |


