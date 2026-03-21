# i18n Quick Reference (Cheat Sheet)

Referencia rápida para usar el pipeline i18n en Com4Energy.

---

## TL;DR — 3 Pasos

### 1️⃣ Crear Enum

```java
// En common/{Proyecto}MessageKey.java
public enum RecordsApiCommonMessageKey implements MessageKey {
    MY_KEY("my.key.name");
    
    private final String key;
    MyMessageKey(String key) { this.key = key; }
    @Override public String key() { return key; }
}
```

### 2️⃣ Agregar Entrada

```properties
# En messages.properties
my.key.name=Mensaje con parámetro: {0}
```

### 3️⃣ Usar en Código

```java
Messages.get(RecordsApiCommonMessageKey.MY_KEY)
Messages.format(RecordsApiCommonMessageKey.MY_KEY, "valor")
```

---

## Convención de Claves (Properties)

```
{dominio}.{sub}.{entidad}.{acción}

incident.event.id.empty          ✅
incident.payload.json.error      ✅
medidaqh.not.found               ✅
validation.email.invalid         ✅
error.unexpected.no_param        ✅

error                            ❌ (muy genérico)
something_wrong                  ❌ (underscores)
INCIDENT_ERROR                   ❌ (mayúsculas)
```

---

## Casos de Uso

### ✅ Validación

```java
throw new BusinessException(
    Messages.format(RecordsApiCommonMessageKey.INCIDENT_EVENT_ID_EMPTY)
);
```

### ✅ Logging

```java
log.warn(Messages.format(
    RecordsApiCommonMessageKey.INCIDENT_PAYLOAD_JSON_PARSE_ERROR, 
    e.getMessage()
));
```

### ✅ Respuesta HTTP

```java
ResponseHelper.okWithMessage(data, RecordsApiCommonMessageKey.USER_CREATED_SUCCESSFULLY);
```

### ✅ Constantes en Clases

```java
private ApiConstants() {
    throw new IllegalStateException(Messages.get(RecordsApiCommonMessageKey.UTILITY_CLASS));
}
```

---

## Organización por Proyecto

### c4e-i18n-core
- Interfaz: `MessageKey.java`
- Utilidad: `Messages.java`
- Properties: solo `utility.class.message`
- Documentación: `I18N_STANDARDS.md`

### c4e-event-publisher
- Enum: `PublisherCommonMessageKey.java` (compartidas)
- Enum: `IncidentPublisherMessageKey.java` (incident-específicas)
- Claves: `incident.publisher.*`, `publisher.*`
- Docs: `I18N.md`

### c4e-records-api
- Enum: `RecordsApiCommonMessageKey.java` (todas)
- Claves: `incident.*`, `medidaqh.*`, `user.*`, `validation.*`, `error.*`
- Docs: `I18N.md`

---

## Antipatrones (NO HACER)

```java
// ❌ NUNCA
log.warn("Payload is not valid");  // String hardcodeado
log.warn("Error: " + id);          // Concatenación
String msg = "User " + name;       // Construcción manual

// ✅ SIEMPRE
log.warn(Messages.format(MyKey.ERROR_PAYLOAD, id));
```

---

## FAQs

**¿Dónde pongo la clave si se usa en 2 proyectos?**
→ En el proyecto "dueño" del dominio. Si es técnica/compartida → `c4e-i18n-core`

**¿Necesito crear enum nuevo para cada módulo?**
→ Solo si crece a **>20 claves** en un módulo. Por ahora: uno global por proyecto.

**¿Cómo manejo múltiples idiomas?**
→ Java soporta `messages_es.properties`, `messages_en.properties`. Refactorizar `Messages.class` en futuro.

**¿Se puede compartir un enum entre proyectos?**
→ Sí, pero crea acoplamiento. Mejor: cada proyecto su enum.

---

## Más Info

- **[I18N_STANDARDS.md](./I18N_STANDARDS.md)** — Documento completo
- **[c4e-event-publisher/docs/I18N.md](../../c4e-event-publisher/docs/I18N.md)** — Specifics del publisher
- **[c4e-records-api/docs/I18N.md](../../c4e-records-api/docs/I18N.md)** — Specifics de records


