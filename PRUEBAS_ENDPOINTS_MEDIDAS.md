# Pruebas de Endpoints - Medidas QH y H

## Base URL
```
BASE_URL=http://localhost:8080/api/v1
```

## 1. Medida QH - Pruebas

### 1.1 Listar todo por cliente con rango de fechas
```bash
curl -X GET "${BASE_URL}/medidaqh?idCliente=34133&startDate=2025-05-01&endDate=2025-05-02&page=0&size=20&sort=fecha,asc" \
  -H "Content-Type: application/json"
```

### 1.2 Últimas 24 horas
```bash
curl -X GET "${BASE_URL}/medidaqh/last24h?idCliente=34133&page=0&size=20" \
  -H "Content-Type: application/json"
```

### 1.3 Obtener por ID
```bash
curl -X GET "${BASE_URL}/medidaqh/1" \
  -H "Content-Type: application/json"
```

### 1.4 Crear nuevo registro (si es aplicable)
```bash
curl -X POST "${BASE_URL}/medidaqh" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 34133,
    "tipomed": 1,
    "fecha": "2025-05-01T12:00:00",
    "banderaInvVer": 0,
    "actent": 100,
    "qactent": 98,
    "r_q1": 5,
    "qr_q1": 95,
    "r_q2": 3,
    "qr_q2": 98,
    "r_q3": 2,
    "qr_q3": 99,
    "r_q4": 1,
    "qr_q4": 99,
    "medres1": 0,
    "qmedres1": 100,
    "medres2": 0,
    "qmedres2": 100,
    "metodObt": 1,
    "origen": "INGESTION",
    "createdBy": "SYSTEM",
    "temporal": 0
  }'
```

## 2. Medida H - Pruebas

### 2.1 Listar todo por cliente con rango de fechas
```bash
curl -X GET "${BASE_URL}/medidah?idCliente=34133&startDate=2025-05-01&endDate=2025-05-02&page=0&size=20&sort=fecha,asc" \
  -H "Content-Type: application/json"
```

### 2.2 Últimas 24 horas
```bash
curl -X GET "${BASE_URL}/medidah/last24h?idCliente=34133&page=0&size=20" \
  -H "Content-Type: application/json"
```

### 2.3 Obtener por ID
```bash
curl -X GET "${BASE_URL}/medidah/1" \
  -H "Content-Type: application/json"
```

### 2.4 Crear nuevo registro (con valores Float)
```bash
curl -X POST "${BASE_URL}/medidah" \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": 34133,
    "tipoMedida": 1,
    "fecha": "2025-05-01T12:00:00",
    "banderaInvVer": 0.0,
    "actent": 100.5,
    "qactent": 98.3,
    "actsal": 5.2,
    "qactsal": 95.8,
    "r_q1": 5.5,
    "qr_q1": 95.1,
    "r_q2": 3.2,
    "qr_q2": 98.7,
    "r_q3": 2.1,
    "qr_q3": 99.2,
    "r_q4": 1.8,
    "qr_q4": 99.5,
    "medres1": 0.0,
    "qmedres1": 100.0,
    "medres2": 0.0,
    "qmedres2": 100.0,
    "metodObt": 1,
    "origen": "INGESTION",
    "createdBy": "SYSTEM",
    "temporal": 0
  }'
```

## 3. Respuestas Esperadas

### 3.1 Éxito (200 OK) - Lista Paginada
```json
{
  "content": [
    {
      "idMedidaQH": 1,
      "clienteId": 34133,
      "tipomed": 1,
      "fecha": "2025-05-01T12:15:00",
      "banderaInvVer": 0,
      "actent": 100,
      "qactent": 98,
      "actsal": 0,
      "qactsal": 0,
      "r_q1": 5,
      "qr_q1": 95,
      "r_q2": 3,
      "qr_q2": 98,
      "r_q3": 2,
      "qr_q3": 99,
      "r_q4": 1,
      "qr_q4": 99,
      "medres1": 0,
      "qmedres1": 100,
      "medres2": 0,
      "qmedres2": 100,
      "metodObt": 1,
      "origen": "INGESTION",
      "createdOn": "2025-05-01T12:15:00",
      "createdBy": "SYSTEM",
      "updatedOn": null,
      "updatedBy": null,
      "temporal": 0
    }
  ],
  "totalPages": 1,
  "totalElements": 1,
  "number": 0,
  "size": 20,
  "sort": [
    {
      "property": "fecha",
      "direction": "ASC",
      "ignoreCase": false,
      "nullHandling": "NATIVE",
      "ascending": true,
      "descending": false
    }
  ],
  "first": true,
  "last": true,
  "empty": false
}
```

### 3.2 Éxito (201 Created) - Crear
```json
{
  "idMedidaQH": 123,
  "clienteId": 34133,
  "tipomed": 1,
  "fecha": "2025-05-01T12:00:00",
  "banderaInvVer": 0,
  "actent": 100,
  "qactent": 98,
  "...": "..."
}
```

### 3.3 Error (404) - No encontrado
```json
{
  "status": 404,
  "message": "Medida not found with id: 999",
  "timestamp": "2025-05-01T12:00:00"
}
```

### 3.4 Error (400) - Validación
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "clienteId",
      "message": "must not be null"
    }
  ]
}
```

## 4. Parámetros de Query Válidos

### 4.1 Filtrado
| Parámetro | Tipo | Obligatorio | Ejemplo |
|-----------|------|------------|---------|
| idCliente | Integer | No | 34133 |
| startDate | String (YYYY-MM-DD) | No | 2025-05-01 |
| endDate | String (YYYY-MM-DD) | No | 2025-05-02 |

### 4.2 Paginación
| Parámetro | Tipo | Obligatorio | Default | Ejemplo |
|-----------|------|------------|---------|---------|
| page | Integer | No | 0 | 0 |
| size | Integer | No | 20 | 20 |
| sort | String | No | fecha,asc | fecha,desc |

## 5. Casos de Prueba Completos

### Caso 1: Datos de QH para 2 clientes en 1 día
```bash
# Fetch
curl -X GET "http://localhost:8080/api/v1/medidaqh?idCliente=34133&startDate=2025-05-01&endDate=2025-05-01&size=100" \
  -H "Content-Type: application/json" | jq '.content | length'

# Expected: Array con ~24 registros (15min cada uno = 96 posibles, pero puede haber menos)
```

### Caso 2: Últimas 24h sin cliente (debe retornar vacío)
```bash
curl -X GET "http://localhost:8080/api/v1/medidah/last24h" \
  -H "Content-Type: application/json" | jq '.content | length'

# Expected: 0
```

### Caso 3: Búsqueda con múltiples páginas
```bash
# Primera página
curl -X GET "http://localhost:8080/api/v1/medidaqh?idCliente=34133&page=0&size=10" | jq '.totalPages'

# Segunda página si existe
curl -X GET "http://localhost:8080/api/v1/medidaqh?idCliente=34133&page=1&size=10" | jq '.content | length'
```

## 6. Headers Recomendados

```bash
-H "Content-Type: application/json" \
-H "Accept: application/json" \
-H "X-Request-ID: $(uuidgen)"  # Para trazabilidad
```

## 7. Validación de Compilación

```bash
# Backend
cd /Users/jesus/Development/Com4Energy/c4e-records-api
mvn clean compile -DskipTests
# Esperado: BUILD SUCCESS

# Frontend (si aplica)
cd /Users/jesus/Development/Com4Energy/frontend/c4e-dashboard
npm run build
# Esperado: Build successful
```

## 📌 Notas Importantes

1. Los endpoints requieren la base de datos y los datos deben existir
2. Los filtros de fecha son INCLUSIVOS (between)
3. Valores nulos en el JSON son válidos
4. MedidaH usa Float, MedidaQH usa Integer
5. La paginación máxima recomendada es 200 items por página

