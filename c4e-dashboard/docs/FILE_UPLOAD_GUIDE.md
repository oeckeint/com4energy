# Interfaz de Carga de Archivos - Documentación

## Descripción General

Se ha implementado una interfaz completa para subir archivos de medición al endpoint de ingesta. La solución incluye:

- **Componente UI**: `FileUploadComponent` - Interfaz visual para cargar archivos
- **Servicio**: `FileUploadService` - Lógica para comunicación con el API
- **Modelos**: `file-upload.types.ts` - Tipos TypeScript para las respuestas

## Características

### ✅ Completamente Implementadas

- ✔️ Carga de archivos via drag-and-drop
- ✔️ Selección de archivos mediante explorador
- ✔️ Validación de extensiones (.xml, .0)
- ✔️ Validación de tamaño (máximo 10MB por archivo)
- ✔️ Validación de archivos no vacíos
- ✔️ Feedback visual en tiempo real
- ✔️ Manejo de errores
- ✔️ Indicador de progreso
- ✔️ Resumen de carga (exitosos, duplicados, rechazados, errores)

## Archivos Creados

### 1. Tipos TypeScript
**Ubicación**: `/Users/jesus/Development/Com4Energy/frontend/c4e-dashboard/src/app/features/metrics/models/file-upload.types.ts`

```typescript
export interface FileBatchItemResponse {
  filename: string;
  status: 'PENDING' | 'DUPLICATED' | 'REJECTED' | 'ERROR';
  reason?: string;
  message?: string;
}

export interface FileUploadBatchResponse {
  uploaded: FileBatchItemResponse[];
  duplicated: FileBatchItemResponse[];
  rejected: FileBatchItemResponse[];
  errors: FileBatchItemResponse[];
}

export interface ApiResponse<T> {
  status: string;
  statusCode: number;
  statusMessage: string;
  data?: T;
  errors?: string[];
}

export interface FileUploadResult {
  success: boolean;
  successCount: number;
  duplicateCount: number;
  errorCount: number;
  rejectedCount: number;
  message: string;
  timestamp: string;
}
```

### 2. Servicio de Carga
**Ubicación**: `/Users/jesus/Development/Com4Energy/frontend/c4e-dashboard/src/app/features/metrics/services/file-upload.service.ts`

**Métodos principales**:

- `uploadFiles(files: File[]): Promise<FileUploadResult | null>` - Carga archivos al servidor
- `clearStatus(): void` - Limpia errores y resultados

**Propiedades reactivas** (signals):

- `uploading`: Indica si se está realizando la carga
- `uploadError`: Contiene mensaje de error si ocurre uno
- `uploadResult`: Contiene el resultado de la carga
- `progress`: Progreso de la carga (0-100)

**Validaciones**:

- Máximo 10MB por archivo
- Extensiones permitidas: .xml, .0
- Archivos no vacíos

### 3. Componente UI
**Ubicación**: `/Users/jesus/Development/Com4Energy/frontend/c4e-dashboard/src/app/features/metrics/components/file-upload.component.ts`

**Características UI**:

- Zona de drop de archivos con feedback visual
- Botón para seleccionar archivos
- Lista de archivos seleccionados con opción de eliminar
- Barra de progreso durante la carga
- Alertas de éxito/error
- Resumen de resultados

## Integración en la Página de Métricas

El componente ha sido integrado en la página de métricas (`metrics.page.ts`):

```typescript
import { FileUploadComponent } from '../components/file-upload.component';

@Component({
  // ...
  imports: [CommonModule, EnergyListComponent, EnergyChartComponent, FileUploadComponent],
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Medida QH</h1>
      <!-- ... otros elementos ... -->
      <app-file-upload />
      <!-- ... resto del template ... -->
    </div>
  `
})
```

## Endpoint Backend Utilizado

El servicio se conecta al endpoint expuesto en `c4e-ingestion-service`:

```
POST /files
Content-Type: multipart/form-data

Body:
  files: File[] (array de archivos)

Response:
  {
    "status": "ACCEPTED",
    "statusCode": 202,
    "statusMessage": "Files received for processing",
    "data": {
      "uploaded": [...],
      "duplicated": [...],
      "rejected": [...],
      "errors": [...]
    }
  }
```

## Flujo de Uso

1. **Cargar archivos**: El usuario arrastra archivos o hace clic para seleccionar
2. **Validación local**: Se validan las extensiones y tamaño
3. **Envío**: Clic en "Cargar Archivos"
4. **Progreso**: Se muestra barra de progreso
5. **Resultado**: Se muestra resumen con éxitos, duplicados, rechazados y errores

## Configuración (si es necesaria)

### Proxy Backend en Angular

Asegúrate de que la configuración del proxy (`proxy.conf.json`) mapee `/api/files`:

```json
{
  "/api/files": {
    "target": "http://localhost:8090",
    "pathRewrite": {
      "^/api/files": "/files"
    }
  }
}
```

## Ejemplos de Respuestas

### ✅ Carga Exitosa
```
Archivo1.xml: PENDING
Archivo2.0: PENDING
Resultado: 2 archivos cargados correctamente
```

### ⚠️ Con Duplicados
```
Archivo1.xml: PENDING
Archivo2.xml: DUPLICATED
Resultado: 1 archivo cargado, 1 duplicado omitido
```

### ❌ Con Errores
```
Archivo1.xml: REJECTED (Invalid format)
Archivo2.0: ERROR (Processing failed)
Resultado: 2 archivos rechazados/con error
```

## Notas Importantes

- Los archivos deben estar en formato XML o extensión .0
- El tamaño máximo permitido es de 10MB por archivo
- Los archivos duplicados se detectan automáticamente
- La interfaz es completamente reactiva con signals de Angular
- El componente es standalone y no requiere módulos
- Compatible con Bootstrap 5 para estilos

## Próximas Mejoras Sugeridas (Opcional)

- [ ] Agregar vista previa de contenido de archivos
- [ ] Permitir configuración del tamaño máximo desde backend
- [ ] Historial de cargas realizadas
- [ ] Descarga de reporte de errores
- [ ] Integración con sistema de notificaciones
- [ ] Soporte para más formatos de archivo

