import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EnergyMeasurementService } from '../services/energy-measurement.service';
import { BadgeComponent } from '../../../shared/components/badge';

// Formatea números con formato europeo: miles (.) y redondea hacia arriba (Math.ceil)
const formatEnergyValue = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '-';
  return Math.ceil(value).toLocaleString('es-ES');
};

@Component({
  selector: 'app-energy-list',
  standalone: true,
  imports: [CommonModule, BadgeComponent],
  template: `
    <div class="p-4">
      @if (service.rawMeasurements().length > 0) {
        <div class="row g-4">
          <!-- Columna: Resumen de Mediciones -->
          <div class="col-md-7">
            <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
              <div class="card-header bg-white border-bottom py-3">
                <h3 class="text-lg font-semibold mb-0">Resumen de Medida QH</h3>
              </div>
              <div class="card-body">
                <div class="d-flex flex-wrap gap-2">
                  <c4e-badge variant="primary" icon="📊">
                    <strong>Total:</strong>&nbsp;{{ service.rawMeasurements().length }}
                  </c4e-badge>
                  <c4e-badge variant="info" icon="⚡">
                    <strong>Energía:</strong>&nbsp;{{ formatEnergyValue(service.totalDailyConsumption()) }} kWh
                  </c4e-badge>
                  <c4e-badge variant="danger" icon="▲">
                    <strong>Máx:</strong>&nbsp;{{ formatEnergyValue(service.maxConsumption()) }} kWh
                  </c4e-badge>
                  <c4e-badge variant="success" icon="▼">
                    <strong>Mín:</strong>&nbsp;{{ formatEnergyValue(service.minConsumption()) }} kWh
                  </c4e-badge>
                  <c4e-badge variant="warning" icon="~">
                    <strong>Prom:</strong>&nbsp;{{ formatEnergyValue(service.avgConsumption()) }} kWh
                  </c4e-badge>
                </div>
              </div>
            </div>
          </div>

          <!-- Columna: Información Complementaria -->
          <div class="col-md-5">
            <div class="card border-0 shadow-sm rounded-4 overflow-hidden h-100">
              <div class="card-header bg-white border-bottom py-3">
                <h3 class="text-lg font-semibold mb-0">Información Complementaria</h3>
              </div>
              <div class="card-body d-flex align-items-center justify-content-center bg-light-subtle">
                <div class="text-center text-muted py-4">
                  <div class="fs-2 mb-2">📋</div>
                  <p class="mb-0 small italic">Área para información adicional del sistema...</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      }

      @if (service.loading()) {
        <div class="d-flex align-items-center justify-content-center p-5 text-primary opacity-75">
          <div class="spinner-border spinner-border-sm me-2" role="status"></div>
          <span class="fw-medium">Cargando datos…</span>
        </div>
      }

      @if (service.error()) {
        <div class="alert alert-danger border-danger-subtle bg-danger-subtle text-danger-emphasis d-flex align-items-center mb-4 p-4 rounded-4 shadow-sm">
          <div class="me-3 fs-3">⚠️</div>
          <div>
            <h5 class="alert-heading mb-1 fw-bold">Ha ocurrido un error</h5>
            <p class="mb-0 opacity-75">{{ service.error() }}</p>
          </div>
        </div>
      }

      @if (service.rawMeasurements().length === 0 && !service.loading() && !service.error()) {
        <div class="alert alert-info border-info-subtle bg-info-subtle text-info-emphasis d-flex align-items-center py-4 px-5 rounded-4 shadow-sm">
          <div class="me-4 fs-1">
            <i class="bi bi-info-circle"></i> ℹ️
          </div>
          <div>
            <h4 class="alert-heading mb-1 fw-bold">No hay datos disponibles</h4>
            <p class="mb-0 opacity-75">No se encontraron mediciones para el rango de fechas y cliente seleccionados. Intenta cambiar los filtros o navegar a otro periodo.</p>
          </div>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EnergyListComponent {
  service = inject(EnergyMeasurementService);
  formatEnergyValue = formatEnergyValue;
}
