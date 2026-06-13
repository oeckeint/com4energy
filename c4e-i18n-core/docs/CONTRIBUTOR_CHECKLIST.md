# Checklist: i18n para Contribuidores

Usa este checklist cuando agregues una nueva clave de mensajes.

---

## 📋 Antes de Empezar

- [ ] ¿Lei **I18N_QUICK_REFERENCE.md** del proyecto?
- [ ] ¿Sé exactamente dónde va la clave (enum correcto)?
- [ ] ¿Tengo clara la convención de naming para este contexto?

---

## 🔧 Implementación

### Paso 1: Crear Clave en Enum

- [ ] Localicé el archivo `{Proyecto}MessageKey.java`
- [ ] Agregué una constante con la clave (ej: `INCIDENT_SAVED("incident.saved")`)
- [ ] Usé UPPER_SNAKE_CASE para el nombre de la constante
- [ ] Usé la convención `{dominio}.{contexto}.{acción}` para la string
- [ ] Agregar comentario si la clave usa parámetros: `{0} = qué es`

### Paso 2: Agregar Entrada en Properties

- [ ] Localicé el archivo `messages.properties` del proyecto
- [ ] Agregué la entrada con la clave exacta del enum
- [ ] Agregué el texto del mensaje (puede tener parámetros tipo `{0}`)
- [ ] Validé que el format es correcto (ej: `{0}` no `{1}` si hay un solo parámetro)

### Paso 3: Usar en Código

- [ ] Reemplacé strings hardcodeados con `Messages.get(KEY)`
- [ ] Reemplacé logging/excepciones con `Messages.format(KEY, args)`
- [ ] Verifiqué que el import es correcto: `com.com4energy.i18n.core.Messages`
- [ ] Verifiqué que importé el enum correcto del proyecto

### Paso 4: Pruebas

- [ ] Compiló sin errores: `mvn compile -q`
- [ ] Ejecuté `mvn test` (si el proyecto tiene tests)
- [ ] Reviswé que los logs/errores muestren el mensaje completo (con parámetros resueltos)

---

## 📝 Ejemplos para Validar Naming

### ✅ Correcto

```
incident.event.id.empty              (dominio.entidad.atributo.estado)
medidaqh.not.found                   (dominio.estado)
validation.email.invalid             (contexto.atributo.estado)
incident.payload.json.parse.error    (dominio.entidad.acción.tipo)
publisher.connection.timeout         (contexto.acción.estado)
error.unexpected.no_param            (tipo.estado.detalle)
```

### ❌ Incorrecto

```
error                                (demasiado genérico)
INCIDENT_ERROR                       (mayúsculas en property)
incident_saved                       (underscores)
incident.save                        (verbo, debería ser participio/estado)
something_went_wrong                 (ambiguo)
```

---

## 🔍 Revisar Duplicados

Antes de agregar, pregúntate:

- [ ] ¿Esta clave ya existe en otro enum?
- [ ] ¿Podría ser compartida por múltiples módulos?
- [ ] ¿Estoy repitiendo un mensaje similar que ya existe?

Si la respuesta es sí a alguna, considera:
- Usar la clave existente en lugar de crear una nueva
- O mover a `PublisherCommonMessageKey` / `RecordsApiCommonMessageKey` si es compartida

---

## 🧪 Testing (Si aplica)

### Validar que la clave no es huérfana

```java
@Test
void shouldHavePropertyForKey() {
    String msg = Messages.get(MyMessageKey.NEW_KEY);
    assertNotNull(msg);
    assertTrue(msg.length() > 0);
}
```

### Validar que la property existe para todos los keys

```java
@Test
void shouldHavePropertiesForAllKeys() {
    for (MyMessageKey key : MyMessageKey.values()) {
        String msg = Messages.get(key);
        assertNotNull("Missing property for " + key.key(), msg);
    }
}
```

---

## 📤 Antes de Pushear (Git)

- [ ] Agregué el enum `XXXMessageKey`
- [ ] Agregué la entrada en `messages.properties`
- [ ] Reemplacé todos los hardcodeados que encontré
- [ ] Compiló sin errores
- [ ] Ningún archivo de configuración roto
- [ ] Mensaje de commit menciona "i18n: ..." (ej: `i18n: add incident.payload.json.parse.error`)

### Ejemplo de Commit

```
i18n: add INCIDENT_PAYLOAD_JSON_PARSE_ERROR message

- Added enum constant in RecordsApiCommonMessageKey
- Added property in messages.properties
- Updated IncidentEventMapper to use i18n instead of hardcoded string

Related to: #123
```

---

## 🆘 Si Algo Falla

### Error: "Missing message key: incident.saved"

→ La entrada NO está en `messages.properties`
→ Verifica que la clave en el enum coincide exactamente

### Error: "No properties found in classpath"

→ Maven no empaquetó `messages.properties`
→ Asegúrate que está en `src/main/resources/`

### Compilación falla con "Cannot find symbol"

→ ¿Importaste el enum correcto?
→ ¿Está en el paquete correcto?

---

## 🎓 Recursos

- **[I18N_QUICK_REFERENCE.md](./I18N_QUICK_REFERENCE.md)** — Referencia rápida (2 min)
- **[I18N_STANDARDS.md](./I18N_STANDARDS.md)** — Guía completa (10 min)
- **Project docs/I18N.md** — Specifics locales (3 min)

---

## 📊 Checklist de Código

```java
// ✅ CORRECTO
Messages.get(RecordsApiCommonMessageKey.INCIDENT_SAVED);
Messages.format(RecordsApiCommonMessageKey.USER_NOT_FOUND, userId);
log.warn(Messages.format(RecordsApiCommonMessageKey.ERROR_MSG, arg));

// ❌ INCORRECTO
Messages.get("incident.saved");                           // String
Messages.format(MessageKey.INCIDENT_SAVED, arg);         // Wrong type
"Incident saved with ID: " + id;                         // Hardcoded
```

---

**¿Listo?** ✅ Haz tu cambio y haz un PR con referencia a este checklist.


