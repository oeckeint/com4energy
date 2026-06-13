# 🏃 Quick Reference: Dónde Están Los Datos

## 30 Segundos Para Saber Todo

```
Tu archivo:     F5D_0031_0894_20250311.0
Total registros:     53,352
├─ Persistidos: 3,720 ✅ → Tabla: sge.medida
├─ Defectos:     672   ❌ → Archivo: .sge_defects.json
└─ Omitidos:  48,960   ⏭️ → (tarifa 20TD, no se guardan)

Verificar:
  mysql -u root -p sge -e "SELECT COUNT(*) FROM medida WHERE created_by='SYSTEM';"
  Resultado esperado: 3720 ✅
```

---

## 🚀 Verificación Rápida (30 segundos)

```bash
# Opción 1: En terminal
mysql -u root -p sge -e "SELECT COUNT(*) FROM medida WHERE created_by='SYSTEM';"

# Opción 2: En MySQL prompt
mysql -u root -p
USE sge;
SELECT COUNT(*) FROM medida WHERE created_by='SYSTEM';
```

**Si devuelve `3720` → ✅ Todo está bien**

---

## 📊 Tabla Rápida

| Métrica | Valor |
|---------|-------|
| Total registros archivo | 53,352 |
| Insertados en BD | 3,720 |
| Con defectos | 672 |
| Omitidos | 48,960 |
| Base de datos | sge |
| Tabla | medida |
| Columna Usuario | created_by |
| Usuario | SYSTEM |
| Tiempo persistencia | 1,736 ms |
| Estado | ✅ COMPLETED |

---

## 💻 SQL Más Usadas

### #1 Contar (La Principal)
```sql
SELECT COUNT(*) FROM sge.medida WHERE created_by='SYSTEM';
-- Respuesta: 3720
```

### #2 Ver Muestra
```sql
SELECT * FROM sge.medida WHERE created_by='SYSTEM' LIMIT 5;
```

### #3 Por Cliente
```sql
SELECT id_cliente, COUNT(*) FROM sge.medida 
WHERE created_by='SYSTEM' GROUP BY id_cliente;
```

### #4 Validar Integridad
```sql
SELECT COUNT(*) FROM sge.medida 
WHERE created_by='SYSTEM' AND id_cliente IS NOT NULL;
```

### #5 Rango de Fechas
```sql
SELECT MIN(fecha), MAX(fecha) FROM sge.medida 
WHERE created_by='SYSTEM';
```

---

## 🔗 Mapeo Tipo → Tabla

| Tipo Archivo | Tabla BD | Entity Java |
|---|---|---|
| P1 | medida_h | MedidaHEntity |
| P2 | medida_qh | MedidaQHEntity |
| **F5** | **medida** | **MedidaLegacyEntity** |
| P5 | medida_cch | MedidaCCHEntity |

---

## 📁 Archivos Importantes

| Archivo | Línea | Qué hace |
|---------|-------|----------|
| MeasureFileTypeProcessor.java | 204-212 | Detecta F5 → medida_legacy |
| JpaMeasurePersistenceAdapter.java | 44-91 | Inserta en BD (⭐ CLAVE) |
| MedidaLegacyEntity.java | 18 | Mapea a tabla medida |
| MedidaLegacyRepository.java | - | JPA Repository |
| application.yml | 9-13 | Config BD |

---

## 🎯 Ubicación Física

```
Base de datos: sge
  └─ Tabla: medida
     ├─ 3,720 filas
     ├─ created_by = 'SYSTEM'
     ├─ created_on = 2025-03-11 (aprox)
     └─ ✅ INSERTADAS
```

---

## ❓ FAQ Rápida

**P: ¿Dónde están los 3,720?**
R: Tabla `sge.medida`

**P: ¿Cómo verifico?**
R: `SELECT COUNT(*) FROM sge.medida WHERE created_by='SYSTEM';`

**P: ¿Dónde están los 672 defectos?**
R: Archivo `*.sge_defects.json`

**P: ¿Por qué 48,960 no están?**
R: Se omiten automáticamente (tarifa 20TD)

**P: ¿Total = 3,720 + 672 + 48,960?**
R: Sí, 53,352 ✅

**P: ¿Cómo le cambio nombre a la tabla?**
R: Abre `MedidaLegacyEntity.java` línea 18

**P: ¿Cómo veo todos mis datos?**
R: `SELECT * FROM sge.medida WHERE created_by='SYSTEM' LIMIT 100;`

**P: ¿Puedo insertarlos desde aquí?**
R: No, ya está insertado. Solo leer/analizar.

---

## 🚨 Si No Ves Los Datos

1. ¿MySQL corre?
   ```bash
   ps aux | grep mysql
   ```

2. ¿BD existe?
   ```sql
   SHOW DATABASES LIKE 'sge';
   ```

3. ¿Tabla existe?
   ```sql
   SHOW TABLES LIKE 'medida';
   ```

4. ¿Hay registros?
   ```sql
   SELECT COUNT(*) FROM sge.medida LIMIT 1;
   ```

---

## 📚 Documentación Disponible

| Necesito | Leer | Tiempo |
|----------|------|--------|
| Respuesta rápida | RESPUESTA_RAPIDA.md | 2 min |
| Entender flujo | PERSISTENCIA_DATOS_MEDIDAS.md | 10 min |
| Paso a paso | HOW_TO_VERIFY_PERSISTENCE.md | 15 min |
| Diagrama | DIAGRAMA_PERSISTENCIA_FLUJO.md | 5 min |
| Código | UBICACIONES_CODIGO_PERSISTENCIA.md | 20 min |
| Queries SQL | EJEMPLOS_PRACTICOS_SQL.md | 10 min |
| Todo | INDICE_PERSISTENCIA.md | 5 min |
| Mapa mental | MAPA_MENTAL.md | 3 min |

---

## ✨ Síntesis Completa

```
┌─────────────────────────────────────────┐
│ PREGUNTA: Dónde se persistieron?        │
├─────────────────────────────────────────┤
│ RESPUESTA: Tabla sge.medida             │
│                                         │
│ CONFIRMACIÓN:                           │
│ SELECT COUNT(*) FROM sge.medida         │
│ WHERE created_by='SYSTEM';              │
│ → 3720 ✅                               │
└─────────────────────────────────────────┘
```

---

## 🎉 ¡Listo!

**Los 3,720 registros están en BD, tabla `sge.medida`**

**Verifica AHORA:**
```bash
mysql -u root -p sge -e "SELECT COUNT(*) FROM medida WHERE created_by='SYSTEM';"
```

Si devuelve `3720` → **¡Todo perfecto!** ✅

