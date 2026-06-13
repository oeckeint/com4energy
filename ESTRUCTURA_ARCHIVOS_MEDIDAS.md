# Estructura de Archivos - Medidas QH y H

## Backend - c4e-records-api

```
src/main/java/com/com4energy/recordsapi/
├── domain/entity/medidas/
│   ├── MedidaCCH.java               ✅ Existente
│   ├── MedidaQH.java                ✨ Actualizada (tipos Integer/Float)
│   └── MedidaH.java                 🆕 CREADA
│
├── repository/
│   ├── MedidaCCHRepository.java      ✅ Existente
│   ├── MedidaQHRepository.java       ✨ Actualizada (clienteId)
│   └── MedidaHRepository.java        🆕 CREADA
│
├── service/
│   ├── MedidaCCHService.java         ✅ Existente
│   ├── MedidaQHService.java          ✅ Existente
│   └── MedidaHService.java           🆕 CREADA
│
└── controller/medidas/
    ├── cch/
    │   ├── MedidaCCHConstants.java    ✅ Existente
    │   └── MedidaCCHController.java   ✅ Existente
    ├── qh/
    │   ├── MedidaQHConstants.java     ✅ Existente
    │   └── MedidaQHController.java    ✅ Existente (PathVariable Integer)
    └── h/ 🆕 NUEVO DIRECTORIO
        ├── MedidaHConstants.java      🆕 CREADA
        └── MedidaHController.java     🆕 CREADA
```

## Frontend - c4e-dashboard

```
src/app/features/metrics/
├── models/medidas/
│   ├── cch/
│   │   └── ...                       ✅ Existente
│   ├── qh/
│   │   ├── MedidaQH.ts               ✅ Existente
│   │   └── MedidaQHConsumptionPoint.ts ✅ Existente
│   └── h/ 🆕 NUEVO DIRECTORIO
│       ├── MedidaH.ts                🆕 CREADA
│       └── MedidaHConsumptionPoint.ts 🆕 CREADA
│
├── services/medidas/
│   ├── cch/
│   │   └── medida-cch.service.ts     ✅ Existente
│   ├── qh/
│   │   └── MedidaQHService.ts        ✨ Actualizado (fetchMatrix)
│   └── h/ 🆕 NUEVO DIRECTORIO
│       └── MedidaHService.ts         🆕 CREADA (fetchMatrix)
│
└── pages/
    ├── medida-cch-view.page.ts       ✅ Existente
    ├── medida-cch.page.html          ✅ Existente
    ├── medida-qh-view.page.ts        🆕 CREADA
    ├── medida-qh.page.html           🆕 CREADA
    ├── medida-h-view.page.ts         🆕 CREADA
    ├── medida-h.page.html            🆕 CREADA
    └── ...otros archivos             ✅ Existentes
```

## Resumen de Cambios

### 🆕 Nuevos Archivos: 12
- Backend Java: 5 archivos (1 entidad, 1 repositorio, 1 servicio, 2 controlador+constantes)
- Frontend TypeScript: 7 archivos (2 modelos, 1 servicio, 2 componentes + templates)

### ✨ Archivos Actualizados: 2
- `MedidaQHRepository.java` - Corrección de queries
- `MedidaQHService.ts` - Adición de fetchMatrix()

### 📊 Endpoints Totales Disponibles: 12
- 4 endpoints para MedidaCCH
- 4 endpoints para MedidaQH  
- 4 endpoints para MedidaH

### 🎨 Páginas de Visualización: 3
- Medida CCH (página existente)
- Medida QH (nueva página)
- Medida H (nueva página)

## Reglas de Diseño Aplicadas

1. **Separación de Concernos**: Cada medida tiene su propia página y no comparten datos
2. **Herencia de Patrón**: Frontend y Backend siguen el mismo patrón CCH
3. **Tipos de Datos Apropiados**:
   - QH: Integer (valores 15-minutos)
   - H: Float (valores horarios con decimales)
   - CCH: Integer (valores centrales)
4. **Filtros Comunes**: Todas soportan filtro por cliente, rango de fechas
5. **Matriz de Visualización**: Cada página muestra matriz hora×cliente

## Notas sobre Herencia en Frontend

Aunque no se implementó herencia explícita (para mantener código simple y legible),
el patrón es consistente entre los 3 componentes:

- Mismo layout HTML
- Mismos métodos de filtrado
- Mismas funciones de formato
- Servicios con mismo patrón de consumo API

Se puede refactorizar a herencia en futuro si se requiere.

