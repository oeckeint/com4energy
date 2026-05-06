import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

// Formatea números con formato europeo: miles (.) y redondea hacia arriba (Math.ceil)
const formatEnergyValue = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '-';
  return Math.ceil(value).toLocaleString('es-ES');
};

interface MeasureMatrixRow {
  hour: string;
  values: Record<string, number | null>;
}

interface MeasureColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

@Component({
  selector: 'app-measure-matrix-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
      <div class="measure-table-shell">
        <div class="measure-table-scroll">
          <table
            class="table table-sm align-middle mb-0 measure-table"
            (mouseleave)="clearCellHover()"
            [style.--measure-hour-width.px]="hourColumnWidth"
          >
            <thead class="table-light">
              <tr>
                <th class="measure-sticky-col measure-sticky-head" [style.min-width.px]="hourColumnWidth">Hora</th>
                <th class="measure-sticky-total measure-sticky-head" [style.min-width.px]="totalColumnWidth">Total</th>
                @for (clientId of clientIds; track clientId) {
                  <th [style.min-width.px]="clientColumnMinWidth" [class.measure-cross-col]="isColumnActive(clientId)">
                    <span>{{ clientId }}</span>
                    @if ((columnValidation[clientId]?.missing ?? 0) > 0) {
                      <span
                        class="measure-missing-header"
                        [attr.title]="'Faltan ' + columnValidation[clientId]?.missing + ' de ' + columnValidation[clientId]?.expected + ' registros'"
                      >&#9888; {{ columnValidation[clientId]?.missing }}</span>
                    }
                  </th>
                }
              </tr>
            </thead>
            <tbody>
              @if (rows.length > 0) {
                @for (row of rows; track row.hour) {
                  <tr>
                    <td class="fw-medium measure-sticky-col" [class.measure-cross-row]="isRowActive(row.hour)">{{ row.hour }}</td>
                    <td
                      class="fw-medium measure-sticky-total"
                      [class.measure-cross-row]="isRowActive(row.hour)"
                      [class.measure-total-warning]="hasMissingInRow(row)"
                    >
                      @if (hasMissingInRow(row)) {
                        <span
                          class="measure-total-warning-icon"
                          [attr.title]="'Faltan ' + getMissingCountInRow(row) + ' registros en esta fila'"
                        >&#9888; {{ getMissingCountInRow(row) }}</span>
                      }
                      {{ formatValue(getRowTotal(row)) }}
                    </td>
                    @for (clientId of clientIds; track clientId) {
                      <td
                        [ngClass]="{
                          'measure-missing-cell': getCellValue(row.values, clientId) === null,
                          'measure-cross-row': isRowActive(row.hour),
                          'measure-cross-col': isColumnActive(clientId),
                          'measure-cell-selected': isCellSelected(row.hour, clientId)
                        }"
                        (mouseenter)="onCellHover(row.hour, clientId)"
                        (click)="selectCell(row.hour, clientId)"
                      >
                        @if (getCellValue(row.values, clientId) === null) {
                          <span class="measure-missing-mark" title="Registro faltante">&#9888; -</span>
                        } @else {
                          {{ formatValue(getCellValue(row.values, clientId)) }}
                        }
                      </td>
                    }
                  </tr>
                }

                <tr class="measure-total-row">
                  <td class="fw-semibold measure-sticky-col">Total</td>
                  <td class="fw-semibold measure-sticky-total" [class.measure-total-warning]="getTotalMissingCount() > 0">
                    @if (getTotalMissingCount() > 0) {
                      <span
                        class="measure-total-warning-icon"
                        [attr.title]="'Faltan ' + getTotalMissingCount() + ' registros en total'"
                      >&#9888; {{ getTotalMissingCount() }}</span>
                    }
                    {{ formatValue(getGrandTotal()) }}
                  </td>
                  @for (clientId of clientIds; track clientId) {
                    <td class="fw-semibold" [class.measure-total-warning]="getMissingCountForClient(clientId) > 0">
                      @if (getMissingCountForClient(clientId) > 0) {
                        <span
                          class="measure-total-warning-icon"
                          [attr.title]="'Faltan ' + getMissingCountForClient(clientId) + ' registros para cliente ' + clientId"
                        >&#9888; {{ getMissingCountForClient(clientId) }}</span>
                      }
                      {{ formatValue(getColumnTotal(clientId)) }}
                    </td>
                  }
                </tr>
              } @else {
                <tr>
                  <td [attr.colspan]="clientIds.length + 2" class="text-center text-muted py-4">
                    {{ emptyMessage }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>
    </div>

    @if (clientIds.length > 0 && !loading) {
      <div class="mt-3">
        <button type="button" class="btn btn-link p-0 text-decoration-none" (click)="toggleAuditor()">
          <span class="me-1">{{ showAuditor ? '▼' : '▶' }}</span>
          <span class="fw-semibold">Auditor</span>
        </button>

        @if (showAuditor) {
          <div class="measure-validation-summary mt-2">
            @for (clientId of clientIds; track clientId) {
              <div class="measure-validation-item" [class.is-warning]="(columnValidation[clientId]?.missing ?? 0) > 0">
                <div class="measure-validation-title">Cliente {{ clientId }}</div>
                <div class="measure-validation-detail">
                  {{ columnValidation[clientId]?.present ?? 0 }}/{{ columnValidation[clientId]?.expected ?? expectedFallback }}
                </div>
                @if ((columnValidation[clientId]?.missing ?? 0) > 0) {
                  <div class="measure-validation-missing">&#9888; Faltan {{ columnValidation[clientId]?.missing }}</div>
                } @else {
                  <div class="measure-validation-ok">Completo</div>
                }
              </div>
            }
          </div>
        }
      </div>
    }
  `
})
export class MeasureMatrixTableComponent {
  @Input() rows: MeasureMatrixRow[] = [];
  @Input() clientIds: number[] = [];
  @Input() loading = false;
  @Input() emptyMessage = 'No hay datos para los filtros seleccionados.';
  @Input() expectedFallback = 24;
  @Input() clientColumnMinWidth = 140;
  @Input() hourColumnWidth = 110;
  @Input() totalColumnWidth = 120;
  @Input() columnValidation: Record<string, MeasureColumnValidation> = {};

  showAuditor = false;
  hoveredHour: string | null = null;
  hoveredClientId: number | null = null;
  selectedHour: string | null = null;
  selectedClientId: number | null = null;

  toggleAuditor(): void {
    this.showAuditor = !this.showAuditor;
  }

  onCellHover(hour: string, clientId: number): void {
    this.hoveredHour = hour;
    this.hoveredClientId = clientId;
  }

  clearCellHover(): void {
    this.hoveredHour = null;
    this.hoveredClientId = null;
  }

  selectCell(hour: string, clientId: number): void {
    if (this.selectedHour === hour && this.selectedClientId === clientId) {
      this.selectedHour = null;
      this.selectedClientId = null;
      return;
    }

    this.selectedHour = hour;
    this.selectedClientId = clientId;
  }

  isRowActive(hour: string): boolean {
    return this.hoveredHour === hour || this.selectedHour === hour;
  }

  isColumnActive(clientId: number): boolean {
    return this.hoveredClientId === clientId || this.selectedClientId === clientId;
  }

  isCellSelected(hour: string, clientId: number): boolean {
    return this.selectedHour === hour && this.selectedClientId === clientId;
  }

  getCellValue(values: Record<string, number | null>, clientId: number): number | null {
    return values[String(clientId)] ?? null;
  }

  formatValue(value: number | null | undefined): string {
    return formatEnergyValue(value);
  }

  getRowTotal(row: MeasureMatrixRow): number | null {
    const values = this.clientIds
      .map((clientId) => row.values[String(clientId)])
      .filter((value): value is number => value !== null && value !== undefined)
      .map((value) => Number(value))
      .filter((value) => Number.isFinite(value));

    if (values.length === 0) {
      return null;
    }

    return values.reduce((acc, value) => acc + value, 0);
  }

  hasMissingInRow(row: MeasureMatrixRow): boolean {
    return this.clientIds.some((clientId) => row.values[String(clientId)] === null || row.values[String(clientId)] === undefined);
  }

  getMissingCountInRow(row: MeasureMatrixRow): number {
    return this.clientIds.filter((clientId) => row.values[String(clientId)] === null || row.values[String(clientId)] === undefined).length;
  }

  getMissingCountForClient(clientId: number): number {
    return this.columnValidation[String(clientId)]?.missing ?? 0;
  }

  getColumnTotal(clientId: number): number | null {
    const values = this.rows
      .map((row) => row.values[String(clientId)])
      .filter((value): value is number => value !== null && value !== undefined)
      .map((value) => Number(value))
      .filter((value) => Number.isFinite(value));

    if (values.length === 0) {
      return null;
    }

    return values.reduce((acc, value) => acc + value, 0);
  }

  getGrandTotal(): number | null {
    const values = this.clientIds
      .map((clientId) => this.getColumnTotal(clientId))
      .filter((value): value is number => value !== null && value !== undefined)
      .map((value) => Number(value))
      .filter((value) => Number.isFinite(value));

    if (values.length === 0) {
      return null;
    }

    return values.reduce((acc, value) => acc + value, 0);
  }

  getTotalMissingCount(): number {
    return this.clientIds.reduce((acc, clientId) => acc + this.getMissingCountForClient(clientId), 0);
  }
}

