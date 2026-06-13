# c4e-i18n-core

Utilidades compartidas de internacionalizacion para microservicios Com4Energy.

## Incluye

- Interfaz `MessageKey` para desacoplar enums de dominio.
- Clase `Messages` para leer/formatear mensajes desde `messages.properties`.

## 📚 Documentación

Para entender cómo usar i18n en **TODO el ecosistema** (c4e-event-publisher, c4e-records-api, etc.):

1. **[I18N_QUICK_REFERENCE.md](./docs/I18N_QUICK_REFERENCE.md)** ← Empieza aquí (2 min)
2. **[I18N_STANDARDS.md](./docs/I18N_STANDARDS.md)** ← Detalles completos (10 min)
3. **Proyecto-specific:** `docs/I18N.md` en cada proyecto (3 min)
4. **[CONTRIBUTOR_CHECKLIST.md](./docs/CONTRIBUTOR_CHECKLIST.md)** ← Antes de hacer un change (5 min)

## Uso rapido

```java
import com.com4energy.i18n.core.Messages;

public enum MyMessageKey implements com.com4energy.i18n.core.MessageKey {
    SAMPLE("sample.message");

    private final String key;

    MyMessageKey(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}

String text = Messages.get(MyMessageKey.SAMPLE);
String formatted = Messages.format(MyMessageKey.SAMPLE, "arg");
```

## Build e instalacion local

```bash
mvn test
mvn install
```

