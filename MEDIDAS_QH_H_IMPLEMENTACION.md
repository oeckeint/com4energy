# Implementación de Medidas QH y H - Guía de Completación

## ✅ Completado - Backend (c4e-records-api)

### Entidades
- [x] `MedidaQH.java` - Actualizada con tipos de datos consistentes
- [x] `MedidaH.java` - Creada con tipos de datos Float para precisión horaria

### Repositorios
- [x] `MedidaQHRepository.java` - Actualizada con queries correctas (clienteId)
- [x] `MedidaHRepository.java` - Creada con métodos de filtrado

### Servicios
- [x] `MedidaQHService.java` - Existe, actualizada si es necesario
- [x] `MedidaHService.java` - Creada con métodos de búsqueda y filtrado

### Controladores
- [x] `MedidaQHController.java` - Actualizada (PathVariable Integer)
- [x] `MedidaQHConstants.java` - Existe (BASE_PATH: /api/v1/medidaqh)
- [x] `MedidaHController.java` - Creada (BASE_PATH: /api/v1/medidah)
- [x] `MedidaHConstants.java` - Creada

### Endpoints Disponibles

#### MedidaQH
```
GET    /api/v1/medidaqh                    - Listar con filtros
GET    /api/v1/medidaqh/last24h            - Últimas 24 horas
GET    /api/v1/medidaqh/{id}               - Obtener por ID
POST   /api/v1/medidaqh                    - Crear
```

**Parámetros de filtro:**
- `idCliente` (opcional) - ID del cliente
- `startDate` (opcional) - Fecha inicio (YYYY-MM-DD)
- `endDate` (opcional) - Fecha fin (YYYY-MM-DD)
- `page` (opcional) - Página (default: 0)
- `size` (opcional) - Tamaño página (default: 20)
- `sort` (opcional) - Ordenación (default: fecha ASC)

#### MedidaH
```
GET    /api/v1/medidah                     - Listar con filtros
GET    /api/v1/medidah/last24h             - Últimas 24 horas
GET    /api/v1/medidah/{id}                - Obtener por ID
POST   /api/v1/medidah                     - Crear
```

## ✅ Completado - Frontend (c4e-dashboard)

### Modelos de Datos
- [x] `MedidaQH.ts` - Interface con campos de QH
- [x] `MedidaQHConsumptionPoint.ts` - Interface para gráficos
- [x] `MedidaH.ts` - Interface con campos de H (Float)
- [x] `MedidaHConsumptionPoint.ts` - Interface para gráficos

### Servicios
- [x] `MedidaQHService.ts` - Actualizado con método fetchMatrix()
- [x] `MedidaHService.ts` - Creado con método fetchMatrix()

### Páginas de Visualización
- [x] `medida-qh-view.page.ts` - Componente QH
- [x] `medida-qh.page.html` - Template QH con tabla matriz
- [x] `medida-h-view.page.ts` - Componente H
- [x] `medida-h.page.html` - Template H con tabla matriz

## 📝 Próximos Pasos

### 1. Integración en Routing
El responsable del frontend debe:
```typescript
// En el módulo de routing
const routes: Routes = [
  {
    path: 'medidas',
    children: [
      { path: 'cch', component: MedidaCchPage },
      { path: 'qh', component: MedidaQHPage },    // NUEVO
      { path: 'h', component: MedidaHPage }       // NUEVO
    ]
  }
];
```

### 2. Navegación en Menú
Agregar enlaces en el menú lateral:
```html
<a routerLink="/metrics/medidas/qh">Medida QH</a>
<a routerLink="/metrics/medidas/h">Medida H</a>
```

### 3. Base de Datos
Verificar que las tablas existan:
- `medidaqh` - Para medidas cuartohorarias
- `medida_h` - Para medidas horarias

Campos principales por tabla:

**medidaqh:**
- id_medidaQH (PK, INT)
- id_cliente (FK, INT)
- tipomed (INT)
- fecha (TIMESTAMP)
- actent (INT) - Energía activa entrante
- qactent (INT) - Calidad
- ... (campos reactivos y residuales)

**medida_h:**
- id_medida_h (PK, INT)
- id_cliente (FK, INT)
- tipo_medida (INT)
- fecha (TIMESTAMP)
- actent (FLOAT) - Energía activa entrante
- qactent (FLOAT) - Calidad
- actsal (FLOAT) - Energía activa saliente
- qactsal (FLOAT) - Calidad saliente
- ... (campos reactivos y residuales)

### 4. Datos de Prueba
Para probar, usar en el filtro:
```
Clientes: 34133, 55221
Fecha: [Seleccionar una fecha con datos]
```

## 🏗️ Opciones de Herencia para el Frontend (Bonus)

Si quieres crear un componente base para reutilización:

```typescript
// base-medida-matrix.component.ts
export abstract class BaseMedidaMatrixComponent<T> implements OnInit {
  abstract readonly service: any;
  abstract readonly filters: any;
  
  readonly selectedClientIds = signal<number[]>([]);

  ngOnInit(): void {
    this.applyFilters();
  }

  async applyFilters(): Promise<void> {
    const clientIds = this.parseClientIds(this.filters.clientIdsText);
    this.selectedClientIds.set(clientIds);
    await this.service.fetchMatrix(this.filters.day, clientIds);
  }

  // Métodos comunes...
}
```

Luego heredar en los componentes específicos:

```typescript
export class MedidaQHPage extends BaseMedidaMatrixComponent<MedidaQH> {
  readonly service = inject(MedidaQHService);
  // ...
}
```

## 🔍 Características Principales

✅ **Filtros:**
- Por cliente (múltiples)
- Por rango de fechas
- Últimas 24 horas

✅ **Visualización:**
- Tabla matriz (filas = horas, columnas = clientes)
- Valores con formato de 3 decimales
- Valores nulos mostrados como "-"
- Carga asíncrona con paginación

✅ **Separación de Interfaces:**
- Medida CCH en página propia
- Medida QH en página propia (separada de CCH)
- Medida H en página propia (separada de ambas)

## 📧 Notas Importantes

### Tipos de Datos
- **MedidaQH**: Usa `Integer` (valores cuartohorarios)
- **MedidaH**: Usa `Float` (valores horarios con decimales)
- **MedidaCCH**: Usa `Integer` (valores centrales)

### Diferencias Clave
| Aspecto | QH | H | CCH |
|--------|--|----|-----|
| Granularidad | 15 min | 1 hora | Central |
| Tipo dato | Integer | Float | Integer |
| Campo principal | actent | actent | actent |
| Tabla | medidaqh | medida_h | medida_cch |

## ✨ Mejoras Futuras

1. Crear interfaz genérica `BaseMedidaEntity` para evitar duplicación
2. Agregar filtro por `tipo_medida`
3. Agregar exportación a Excel/CSV
4. Gráficos de consumo histórico
5. Alertas por picos de consumo

