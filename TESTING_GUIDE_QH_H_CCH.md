# Guía de Pruebas - Medidas QH, H, CCH

## 1️⃣ Compilar Backend

```bash
cd /Users/jesus/Development/Com4Energy/c4e-records-api
./mvnw clean package -DskipTests
```

**Salida esperada**:
```
BUILD SUCCESS
Total time: XX.XXXs
```

---

## 2️⃣ Compilar Frontend

```bash
cd /Users/jesus/Development/Com4Energy/frontend/c4e-dashboard
npm run build
```

**Salida esperada**:
```
✔ Building...
Application bundle generation complete. [X.XXX seconds]
```

---

## 3️⃣ Iniciar Backend (Records API)

```bash
# Opción A: Desde IDE (ejecutar Main class)
# c4e-records-api/src/main/java/com/com4energy/recordsapi/RecordsApiApplication.java

# Opción B: Línea de comandos
cd /Users/jesus/Development/Com4Energy/c4e-records-api
./mvnw spring-boot:run
```

**Verificar que esté corriendo**:
```bash
curl http://localhost:8080/actuator/health
```

**Salida esperada**:
```json
{"status":"UP"}
```

---

## 4️⃣ Iniciar Frontend (Dashboard)

```bash
cd /Users/jesus/Development/Com4Energy/frontend/c4e-dashboard
npm start
```

**Acceso**: `http://localhost:4200`

---

## 5️⃣ Pruebas de API (curl)

### 5.1 - Test MedidaQH

```bash
# Listar con filtros
curl -X GET \
  "http://localhost:8080/api/v1/medidaqh?idCliente=34133&startDate=2026-04-30&endDate=2026-05-01&page=0&size=10" \
  -H "Content-Type: application/json" | jq '.'

# Últimas 24H
curl -X GET \
  "http://localhost:8080/api/v1/medidaqh/last24h?idCliente=34133&page=0&size=10" \
  -H "Content-Type: application/json" | jq '.'

# Por ID
curl -X GET \
  "http://localhost:8080/api/v1/medidaqh/1" \
  -H "Content-Type: application/json" | jq '.'
```

### 5.2 - Test MedidaH

```bash
# Listar con filtros
curl -X GET \
  "http://localhost:8080/api/v1/medidah?idCliente=34133&startDate=2026-04-30&endDate=2026-05-01&page=0&size=10" \
  -H "Content-Type: application/json" | jq '.'

# Últimas 24H
curl -X GET \
  "http://localhost:8080/api/v1/medidah/last24h?idCliente=34133&page=0&size=10" \
  -H "Content-Type: application/json" | jq '.'

# Por ID
curl -X GET \
  "http://localhost:8080/api/v1/medidah/1" \
  -H "Content-Type: application/json" | jq '.'
```

### 5.3 - Test MedidaCCH

```bash
# Listar con filtros
curl -X GET \
  "http://localhost:8080/api/v1/medidacch?idCliente=34133&startDate=2026-04-30&endDate=2026-05-01&page=0&size=10" \
  -H "Content-Type: application/json" | jq '.'

# Últimas 24H
curl -X GET \
  "http://localhost:8080/api/v1/medidacch/last24h?idCliente=34133&page=0&size=10" \
  -H "Content-Type: application/json" | jq '.'

# Por ID
curl -X GET \
  "http://localhost:8080/api/v1/medidacch/1" \
  -H "Content-Type: application/json" | jq '.'
```

---

## 6️⃣ Pruebas de UI (Navegador)

### 6.1 - Navegar a Medida QH

1. Abrir: `http://localhost:4200`
2. Menú: **Vistas** → **Medida QH**
3. O directo: `http://localhost:4200/metrics`

**Acciones**:
- Ingrese cliente: `34133`
- Seleccione fecha: Hoy
- Click: **Buscar**
- ✅ Verificar tabla llena con 24 horas y columna ACTENT

### 6.2 - Navegar a Medida CCH

1. Menú: **Vistas** → **Medida CCH**
2. O directo: `http://localhost:4200/metrics/cch`

**Acciones**:
- Ingrese cliente: `34133`
- Seleccione fecha: Hoy
- Click: **Buscar**
- ✅ Verificar tabla llena con 24 horas

### 6.3 - Navegar a Medida H

1. Menú: **Vistas** → **Medida H**
2. O directo: `http://localhost:4200/metrics/h`

**Acciones**:
- Ingrese cliente: `34133`
- Seleccione fecha: Hoy
- Click: **Buscar**
- ✅ Verificar tabla llena con 24 horas

### 6.4 - Test: Múltiples Clientes

1. Ir a cualquier vista (ej: Medida QH)
2. Ingrese clientes: `34133, 55221, 12345`
3. Seleccione fecha
4. Click: **Buscar**
5. ✅ Verificar columnas para cada cliente

### 6.5 - Test: Limpiar Filtros

1. Ir a cualquier vista
2. Ingrese datos
3. Click: **Limpiar**
4. ✅ Verificar campos vacíos

### 6.6 - Test: Manejo de Errores

1. Ingrese cliente: `-1` (inválido)
2. Click: **Buscar**
3. ✅ Verificar mensaje de error

---

## 7️⃣ Verificación de Compilación Sin Errores

### Backend

```bash
cd /Users/jesus/Development/Com4Energy/c4e-records-api
./mvnw -q -DskipTests compile
echo "Backend: $?"  # Esperado: 0
```

### Frontend

```bash
cd /Users/jesus/Development/Com4Energy/frontend/c4e-dashboard
npm run build > /dev/null 2>&1
echo "Frontend: $?"  # Esperado: 0
```

---

## 8️⃣ Validación de Datos

### Verificar Campos en JSON

```bash
# MedidaQH debe tener actent, qactent, actsal, qactsal, etc.
curl -s "http://localhost:8080/api/v1/medidaqh?idCliente=34133&page=0&size=1" | \
  jq '.content[0] | keys' | head -20

# Esperado:
# [
#   "actent",
#   "actsal",
#   "banderaInvVer",
#   "clienteId",
#   "createdBy",
#   "createdOn",
#   ... etc
# ]
```

---

## 9️⃣ Troubleshooting

| Problema | Solución |
|----------|----------|
| **Backend no inicia** | Verificar puerto 8080 no esté en uso: `lsof -i :8080` |
| **Frontend no carga** | Limpiar caché: `npm run clean && npm install` |
| **"404 Not Found" en API** | Verificar que backend esté corriendo: `curl http://localhost:8080/actuator/health` |
| **Tabla vacía en UI** | Verificar datos en DB: `curl http://localhost:8080/api/v1/medidaqh?idCliente=34133` |
| **"CORS error"** | Backend debe tener CORS configurado para `http://localhost:4200` |
| **Compilación fallida** | Limpiar: `./mvnw clean` y reintentar |

---

## 🔟 Performance

### Tamaño de Bundle

```bash
cd /Users/jesus/Development/Com4Energy/frontend/c4e-dashboard
npm run build --prod

# Resultados esperados:
# medida-qh-view-page:   ~6.8 kB
# medida-h-view-page:    ~6.8 kB
# medida-cch-view-page:  ~6.4 kB
```

### Tiempo de Compilación

```bash
# Backend: ~5-10 segundos
./mvnw clean compile -DskipTests

# Frontend: ~2-3 segundos
npm run build
```

---

## 📊 Dataset de Prueba Sugerido

Para pruebas realistas, insertar datos en las tablas:

```sql
-- MedidaQH (2 días × 24 horas × 2 clientes = 96 registros)
INSERT INTO medidaqh (id_cliente, fecha, actent, qactent, actsal, qactsal, origen)
VALUES 
  (34133, '2026-05-01 00:00:00', 100, 95, 5, 10, 'INGESTION'),
  (34133, '2026-05-01 01:00:00', 110, 100, 6, 11, 'INGESTION'),
  -- ... repetir para 24 horas ...
  (55221, '2026-05-01 00:00:00', 200, 190, 10, 20, 'INGESTION'),
  -- ... etc

-- MedidaH (similares)
INSERT INTO medida_h (id_cliente, fecha, actent, qactent, actsal, qactsal, origen)
VALUES 
  (34133, '2026-05-01 00:30:00', 100.5, 95.2, 5.3, 10.1, 'INGESTION'),
  -- ... etc

-- MedidaCCH (idéntico a QH pero sin reactivos)
INSERT INTO medida_cch (id_cliente, fecha, actent, origen)
VALUES 
  (34133, '2026-05-01 00:00:00', 100, 'INGESTION'),
  -- ... etc
```

---

## ✅ Checklist de Validación

- [ ] Backend compila sin errores
- [ ] Frontend compila sin errores
- [ ] Backend inicia correctamente
- [ ] Frontend inicia correctamente
- [ ] `/metrics` muestra Medida QH
- [ ] `/metrics/cch` muestra Medida CCH
- [ ] `/metrics/h` muestra Medida H
- [ ] Menú navegación con 3 opciones
- [ ] Endpoint `/api/v1/medidaqh` responde
- [ ] Endpoint `/api/v1/medidah` responde
- [ ] Endpoint `/api/v1/medidacch` responde
- [ ] Tabla llena correctamente (24 horas)
- [ ] Múltiples clientes funcionan
- [ ] "Limpiar" resetea filtros
- [ ] Errores se manejan correctamente

---

**Autor**: GitHub Copilot  
**Fecha**: 2026-05-01  
**Estado**: ✅ LISTO PARA TESTING

