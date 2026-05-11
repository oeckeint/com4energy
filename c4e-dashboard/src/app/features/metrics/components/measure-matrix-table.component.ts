import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

// Formatea números con formato europeo: miles (.) y redondea hacia arriba (Math.ceil)
const formatEnergyValue = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '-';
  const rounded = Math.ceil(value);
  const sign = rounded < 0 ? '-' : '';
  const digits = Math.abs(rounded).toString();
  const grouped = digits.replaceAll(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${sign}${grouped}`;
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
    <div class="card measure-matrix-card rounded-4 mb-4">
      <div class="card-body measure-matrix-card-body">
        <div class="measure-matrix-toolbar" aria-label="Cabecera de tabla de medidas">
          <div class="measure-matrix-toolbar-row measure-matrix-toolbar-row--title">
            <div class="measure-matrix-toolbar-side measure-matrix-toolbar-left"></div>
            <div class="measure-matrix-toolbar-center">
              @if (showDateNavigator) {
                <div class="measure-matrix-date-nav" aria-label="Navegacion por fecha">
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-secondary measure-matrix-date-nav-btn"
                    aria-label="Dia anterior"
                    title="Dia anterior"
                    [disabled]="loading"
                    (click)="requestPreviousDay()"
                  >
                    &#9664;
                  </button>
                  <div class="measure-matrix-date-label" [attr.aria-label]="'Fecha seleccionada: ' + headerDateDisplay">
                    {{ headerDateDisplay }}
                  </div>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-secondary measure-matrix-date-nav-btn"
                    aria-label="Dia siguiente"
                    title="Dia siguiente"
                    [disabled]="loading"
                    (click)="requestNextDay()"
                  >
                    &#9654;
                  </button>
                </div>
              } @else {
                <h2 class="measure-matrix-toolbar-title mb-0">Tabla de medidas</h2>
              }
            </div>
            <div class="measure-matrix-toolbar-side measure-matrix-toolbar-right">
              <div class="measure-matrix-actions-top">
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary measure-matrix-action-btn"
                  aria-label="Actualizar"
                  title="Actualizar"
                  [disabled]="loading"
                  (click)="requestRefresh()"
                >
                  <span aria-hidden="true">&#x21bb;</span>
                </button>
                <button
                  type="button"
                  class="btn btn-sm btn-outline-secondary measure-matrix-action-btn"
                  aria-label="Compartir"
                  title="Compartir"
                  (click)="openOptionsModal()"
                >
                  <span aria-hidden="true">&#x1F4E4;</span>
                </button>
              </div>
            </div>
          </div>

          <div class="measure-matrix-toolbar-row measure-matrix-toolbar-row--actions">
            <div class="measure-matrix-toolbar-side measure-matrix-toolbar-left"></div>
            <div class="measure-matrix-toolbar-side measure-matrix-toolbar-center"></div>
            <div class="measure-matrix-toolbar-side measure-matrix-toolbar-right">
              @if (loading || statusText || !hasData) {
                <div class="measure-matrix-actions-status" aria-live="polite">
                  @if (loading) {
                    <span class="measure-matrix-status" aria-busy="true">
                      <span class="c4e-inline-spinner" role="status" aria-hidden="true"></span>
                      <span class="text-muted small">Cargando...</span>
                    </span>
                  } @else if (statusText) {
                    <span class="measure-matrix-status text-muted small">
                      @if (statusWarning) {
                        <span aria-hidden="true">⚠️</span>
                      } @else {
                        <span class="text-success" aria-hidden="true">✓</span>
                      }
                      <span>{{ statusText }}</span>
                    </span>
                  } @else {
                    <span class="measure-matrix-status text-muted small">
                      <span aria-hidden="true">❌</span>
                      <span>Sin datos</span>
                    </span>
                  }
                </div>
              }
            </div>
          </div>
        </div>


        <div class="measure-table-shell">
          @if (hasData && rows.length > 0) {
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
                @for (row of rows; track row) {
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
                        [attr.title]="getCellTitle(row.hour, clientId, getCellValue(row.values, clientId))"
                        (mouseenter)="onCellHover(row.hour, clientId, getCellValue(row.values, clientId))"
                        (click)="selectCell(row.hour, clientId, getCellValue(row.values, clientId))"
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
            </tbody>
          </table>
          </div>
          } @else {
            <div class="py-4 px-3">
              @if (emptyStateTitle || emptyStateDescription) {
                <div class="measure-table-empty-state" role="status" aria-live="polite">
                  @if (emptyStateTitle) {
                    <div class="measure-table-empty-title">{{ emptyStateTitle }}</div>
                  }
                  @if (emptyStateDescription) {
                    <div class="measure-table-empty-description">{{ emptyStateDescription }}</div>
                  } @else {
                    <div class="measure-table-empty-description">{{ emptyMessage }}</div>
                  }
                </div>
              } @else {
                <div class="text-center text-muted">{{ emptyMessage }}</div>
              }
            </div>
          }
        </div>

      </div>
    </div>

    @if (isOptionsModalOpen) {
      <div class="measure-options-modal-backdrop" (click)="closeOptionsModal()">
        <div
          class="measure-options-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="measure-options-modal-title"
          (click)="$event.stopPropagation()"
        >
          <div class="measure-options-modal-header">
            <div class="measure-options-modal-heading">
              <h3 id="measure-options-modal-title" class="measure-options-modal-title mb-0">Compartir datos</h3>
              <div class="measure-options-modal-subtitle">Elige cómo compartir o preparar esta vista para otros usuarios.</div>
            </div>
            <button
              type="button"
              class="btn btn-sm btn-outline-secondary"
              aria-label="Cerrar"
              title="Cerrar"
              (click)="closeOptionsModal()"
            >
              &times;
            </button>
          </div>

          <div class="measure-options-modal-body">
            <div class="measure-options-group">
              <div class="measure-options-group-title">Compartir ahora</div>

              <button
                type="button"
                class="measure-share-option measure-share-option--primary"
                title="Exportar CSV rapido"
                [disabled]="loading || !hasData || rows.length === 0"
                (click)="exportQuickMatrixToCsv()"
              >
                <span class="measure-share-option-icon" aria-hidden="true">↓</span>
                <span class="measure-share-option-content">
                  <span class="measure-share-option-label">Exportar CSV rapido</span>
                  <span class="measure-share-option-description">Descarga solo la tabla visible: hora, total y clientes.</span>
                </span>
              </button>

              <button type="button" class="measure-share-option" disabled title="Proximamente">
                <span class="measure-share-option-icon" aria-hidden="true">↗</span>
                <span class="measure-share-option-content">
                  <span class="measure-share-option-label">Copiar enlace de esta vista</span>
                  <span class="measure-share-option-description">Comparte exactamente los filtros y la tabla que estas viendo.</span>
                </span>
                <span class="measure-share-option-badge">Proximamente</span>
              </button>
            </div>

            <div class="measure-options-group measure-options-group-suggestions">
              <div class="measure-options-group-title">Otras formas de compartir</div>
              <button
                type="button"
                class="measure-share-option"
                title="Exportar CSV avanzado"
                [disabled]="loading || !hasData || rows.length === 0"
                (click)="exportAdvancedMatrixToCsv()"
              >
                <span class="measure-share-option-icon" aria-hidden="true">#</span>
                <span class="measure-share-option-content">
                  <span class="measure-share-option-label">Exportar CSV avanzado</span>
                  <span class="measure-share-option-description">Incluye metadata, matriz, resumen por cliente, hora y global.</span>
                </span>
              </button>

              <button type="button" class="measure-share-option" disabled title="Proximamente">
                <span class="measure-share-option-icon" aria-hidden="true">✉</span>
                <span class="measure-share-option-content">
                  <span class="measure-share-option-label">Enviar por correo</span>
                  <span class="measure-share-option-description">Prepara el envio del reporte a uno o varios destinatarios.</span>
                </span>
                <span class="measure-share-option-badge">Proximamente</span>
              </button>

              <button type="button" class="measure-share-option" disabled title="Proximamente">
                <span class="measure-share-option-icon" aria-hidden="true">≡</span>
                <span class="measure-share-option-content">
                  <span class="measure-share-option-label">Compartir resumen diario</span>
                  <span class="measure-share-option-description">Resume lo mas importante para usuarios que no necesitan toda la matriz.</span>
                </span>
                <span class="measure-share-option-badge">Proximamente</span>
              </button>
            </div>

            <div class="measure-options-group measure-options-group-suggestions">
              <div class="measure-options-group-title">Sugerencias</div>
              <div class="measure-options-hint">Comparte segun lo que quieras comunicar:</div>
              <div class="measure-options-tags">
                <span class="measure-options-tag">Resumen diario</span>
                <span class="measure-options-tag">Clientes con faltantes</span>
                <span class="measure-options-tag">Vista filtrada actual</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    }

  `
})
export class MeasureMatrixTableComponent implements OnChanges {
  private readonly weekdayNames = ['Domingo', 'Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado'];
  private readonly monthNames = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

  @Input() rows: MeasureMatrixRow[] = [];
  @Input() clientIds: number[] = [];
  @Input() hasData = true;
  @Input() loading = false;
  @Input() statusText: string | null = null;
  @Input() statusWarning = false;
  @Input() emptyMessage = 'No hay datos para los filtros seleccionados.';
  @Input() emptyStateTitle: string | null = null;
  @Input() emptyStateDescription: string | null = null;
  @Input() expectedFallback = 24;
  @Input() clientColumnMinWidth = 140;
  @Input() hourColumnWidth = 110;
  @Input() totalColumnWidth = 120;
  @Input() columnValidation: Record<string, MeasureColumnValidation> = {};
  @Input() exportMeasureType = 'Medidas';
  @Input() exportDate: string | null = null;
  @Input() exportClientFilterText = '';
  @Input() showDateNavigator = false;
  @Input() headerDate: string | null = null;
  @Input() cellOriginFetcher: ((hour: string, clientId: number) => Promise<string | null>) | null = null;

  @Output() headerPreviousDayRequested = new EventEmitter<void>();
  @Output() headerNextDayRequested = new EventEmitter<void>();
  @Output() headerDateSelected = new EventEmitter<string>();
  @Output() refreshRequested = new EventEmitter<void>();

  hoveredHour: string | null = null;
  hoveredClientId: number | null = null;
  selectedHour: string | null = null;
  selectedClientId: number | null = null;
  isOptionsModalOpen = false;
  private readonly cellOriginCache = new Map<string, string | null>();
  private readonly cellOriginLoading = new Set<string>();
  private readonly cellOriginError = new Set<string>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['headerDate'] || changes['rows'] || changes['clientIds'] || changes['cellOriginFetcher']) {
      this.cellOriginCache.clear();
      this.cellOriginLoading.clear();
      this.cellOriginError.clear();
    }
  }

  get headerDateDisplay(): string {
    const parsed = this.parseIsoDate(this.headerDate);
    if (!parsed) {
      return 'Sin fecha';
    }

    const weekday = this.weekdayNames[parsed.getDay()];
    const day = parsed.getDate();
    const month = this.monthNames[parsed.getMonth()];
    const year = parsed.getFullYear();

    return `${weekday}, ${day} de ${month} del ${year}`;
  }


  onCellHover(hour: string, clientId: number, value: number | null = null): void {
    this.hoveredHour = hour;
    this.hoveredClientId = clientId;
  }

  clearCellHover(): void {
    this.hoveredHour = null;
    this.hoveredClientId = null;
  }

  getCellTitle(hour: string, clientId: number, value: number | null): string {
    if (value === null || value === undefined) {
      return 'Sin dato para esta celda';
    }

    if (this.cellOriginFetcher === null) {
      return 'Origen no disponible';
    }

    if (!this.isCellSelected(hour, clientId)) {
      return 'Selecciona la celda para consultar origen del archivo';
    }

    const key = this.buildCellKey(hour, clientId);

    if (this.cellOriginLoading.has(key)) {
      return 'Cargando origen del archivo...';
    }

    if (this.cellOriginError.has(key)) {
      return 'No se pudo cargar el origen del archivo';
    }

    if (!this.cellOriginCache.has(key)) {
      return 'Origen pendiente de consulta';
    }

    const cached = this.cellOriginCache.get(key);
    return cached ?? 'Origen no informado';
  }

  openOptionsModal(): void {
    this.isOptionsModalOpen = true;
  }

  requestPreviousDay(): void {
    this.headerPreviousDayRequested.emit();
  }

  requestNextDay(): void {
    this.headerNextDayRequested.emit();
  }

  onHeaderDateSelected(rawDate: string): void {
    const normalized = rawDate.trim();
    if (!normalized) {
      return;
    }

    this.headerDateSelected.emit(normalized);
  }

  private parseIsoDate(value: string | null): Date | null {
    if (!value) {
      return null;
    }

    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (!match) {
      return null;
    }

    const [, year, month, day] = match;
    const parsed = new Date(Number(year), Number(month) - 1, Number(day));
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  requestRefresh(): void {
    this.refreshRequested.emit();
  }

  closeOptionsModal(): void {
    this.isOptionsModalOpen = false;
  }

  exportQuickMatrixToCsv(): void {
    if (this.loading || !this.hasData || this.rows.length === 0) {
      return;
    }

    this.downloadCsv(this.buildMatrixOnlyCsv(), this.buildCsvFileName('rapido'));
    this.closeOptionsModal();
  }

  exportAdvancedMatrixToCsv(): void {
    if (this.loading || !this.hasData || this.rows.length === 0) {
      return;
    }

    this.downloadCsv(this.buildVisibleMatrixCsv(), this.buildCsvFileName('avanzado'));
    this.closeOptionsModal();
  }

  private downloadCsv(csvContent: string, fileName: string): void {
    if (this.loading || !this.hasData || this.rows.length === 0) {
      return;
    }

    if (globalThis.window === undefined || globalThis.document === undefined) {
      return;
    }

    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const downloadUrl = globalThis.URL.createObjectURL(blob);
    const link = globalThis.document.createElement('a');

    link.href = downloadUrl;
    link.download = fileName;
    link.style.display = 'none';

    globalThis.document.body.appendChild(link);
    link.click();
    link.remove();
    globalThis.URL.revokeObjectURL(downloadUrl);
  }

  selectCell(hour: string, clientId: number, value: number | null): void {
    if (this.selectedHour === hour && this.selectedClientId === clientId) {
      this.selectedHour = null;
      this.selectedClientId = null;
      return;
    }

    this.selectedHour = hour;
    this.selectedClientId = clientId;

    this.loadCellOrigin(hour, clientId, value);
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
      .map(Number)
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
      .map(Number)
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
      .map(Number)
      .filter((value) => Number.isFinite(value));

    if (values.length === 0) {
      return null;
    }

    return values.reduce((acc, value) => acc + value, 0);
  }

  getTotalMissingCount(): number {
    return this.clientIds.reduce((acc, clientId) => acc + this.getMissingCountForClient(clientId), 0);
  }

  private buildVisibleMatrixCsv(): string {
    const delimiter = ';';
    const lines: string[] = [];

    lines.push('#METADATA');
    for (const row of this.buildCsvMetadataRows()) {
      lines.push(row.map((value) => this.escapeCsvValue(value)).join(delimiter));
    }

    lines.push('', '#MATRIZ');
    const header = ['Hora', 'Total', ...this.clientIds.map(String)];

    lines.push(header.map((value) => this.escapeCsvValue(value)).join(delimiter));

    for (const row of this.rows) {
      const rowValues = [
        row.hour,
        this.toCsvCellValue(this.getRowTotal(row)),
        ...this.clientIds.map((clientId) => this.toCsvCellValue(this.getCellValue(row.values, clientId)))
      ];

      lines.push(rowValues.map((value) => this.escapeCsvValue(value)).join(delimiter));
    }

    const totalsRow = [
      'Total',
      this.toCsvCellValue(this.getGrandTotal()),
      ...this.clientIds.map((clientId) => this.toCsvCellValue(this.getColumnTotal(clientId)))
    ];

    lines.push(totalsRow.map((value) => this.escapeCsvValue(value)).join(delimiter));

    lines.push(
      '',
      '#RESUMEN_CLIENTES',
      ['Cliente', 'Esperados', 'Presentes', 'Faltantes', 'Completo', 'Total'].map((value) => this.escapeCsvValue(value)).join(delimiter)
    );

    for (const clientId of this.clientIds) {
      const validation = this.getColumnValidationForClient(clientId);
      const clientTotal = this.toCsvCellValue(this.getColumnTotal(clientId));
      const row = [
        String(clientId),
        String(validation.expected),
        String(validation.present),
        String(validation.missing),
        validation.complete ? 'SI' : 'NO',
        clientTotal
      ];
      lines.push(row.map((value) => this.escapeCsvValue(value)).join(delimiter));
    }

    lines.push('', '#RESUMEN_HORAS', ['Hora', 'Total', 'Faltantes en fila'].map((value) => this.escapeCsvValue(value)).join(delimiter));
    for (const row of this.rows) {
      const hourRow = [row.hour, this.toCsvCellValue(this.getRowTotal(row)), String(this.getMissingCountInRow(row))];
      lines.push(hourRow.map((value) => this.escapeCsvValue(value)).join(delimiter));
    }

    lines.push('', '#RESUMEN_GENERAL');
    const globalSummaryRows = [
      ['Total clientes', String(this.clientIds.length)],
      ['Total filas', String(this.rows.length)],
      ['Registros faltantes totales', String(this.getTotalMissingCount())],
      ['Total energia', this.toCsvCellValue(this.getGrandTotal())]
    ];
    for (const row of globalSummaryRows) {
      lines.push(row.map((value) => this.escapeCsvValue(value)).join(delimiter));
    }

    return lines.join('\r\n');
  }

  private buildMatrixOnlyCsv(): string {
    const delimiter = ';';
    const lines: string[] = [];
    const header = ['Hora', 'Total', ...this.clientIds.map(String)];

    lines.push(header.map((value) => this.escapeCsvValue(value)).join(delimiter));

    for (const row of this.rows) {
      const rowValues = [
        row.hour,
        this.toCsvCellValue(this.getRowTotal(row)),
        ...this.clientIds.map((clientId) => this.toCsvCellValue(this.getCellValue(row.values, clientId)))
      ];

      lines.push(rowValues.map((value) => this.escapeCsvValue(value)).join(delimiter));
    }

    const totalsRow = [
      'Total',
      this.toCsvCellValue(this.getGrandTotal()),
      ...this.clientIds.map((clientId) => this.toCsvCellValue(this.getColumnTotal(clientId)))
    ];

    lines.push(totalsRow.map((value) => this.escapeCsvValue(value)).join(delimiter));

    return lines.join('\r\n');
  }

  private buildCsvFileName(mode: 'rapido' | 'avanzado'): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');

    const typeSlug = this.toCsvFileSlug(this.exportMeasureType || 'tabla-medidas');
    return `${typeSlug}-${mode}-${year}${month}${day}-${hours}${minutes}.csv`;
  }

  private buildCsvMetadataRows(): string[][] {
    const generatedAt = new Date().toLocaleString('es-ES');
    const fallbackRows = this.rows.length > 0 ? this.rows.length : this.expectedFallback;
    const clientScope = this.exportClientFilterText.trim() ? this.exportClientFilterText.trim() : 'Todos';

    return [
      ['Reporte', 'Tabla de medidas'],
      ['Tipo de medida', this.exportMeasureType],
      ['Fecha del reporte', this.exportDate ?? 'No especificada'],
      ['Generado', generatedAt],
      ['Clientes en reporte', String(this.clientIds.length)],
      ['Filas en matriz', String(this.rows.length)],
      ['Filas esperadas por cliente', String(fallbackRows)],
      ['Filtro de clientes', clientScope]
    ];
  }

  private getColumnValidationForClient(clientId: number): MeasureColumnValidation {
    const storedValidation = this.columnValidation[String(clientId)];
    if (storedValidation) {
      return storedValidation;
    }

    const expected = this.rows.length > 0 ? this.rows.length : this.expectedFallback;
    const present = this.rows.filter((row) => row.values[String(clientId)] !== null && row.values[String(clientId)] !== undefined).length;
    const missing = Math.max(expected - present, 0);

    return {
      expected,
      present,
      missing,
      complete: missing === 0
    };
  }

  private toCsvFileSlug(value: string): string {
    const slug = value
      .toLowerCase()
      .normalize('NFD')
      .replaceAll(/[\u0300-\u036f]/g, '')
      .replaceAll(/[^a-z0-9]+/g, '-')
      .replaceAll(/^-+|-+$/g, '');

    return slug || 'tabla-medidas';
  }

  private toCsvCellValue(value: number | string | null | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }

    return String(value);
  }

  private escapeCsvValue(value: string): string {
    if (value.includes(';') || value.includes('"') || value.includes('\n') || value.includes('\r')) {
      return `"${value.replaceAll('"', '""')}"`;
    }

    return value;
  }

  private buildCellKey(hour: string, clientId: number): string {
    return `${hour}|${clientId}`;
  }

  private loadCellOrigin(hour: string, clientId: number, value: number | null): void {
    if (value === null || value === undefined || this.cellOriginFetcher === null) {
      return;
    }

    const key = this.buildCellKey(hour, clientId);
    if (this.cellOriginCache.has(key) || this.cellOriginLoading.has(key)) {
      return;
    }

    this.cellOriginLoading.add(key);
    this.cellOriginFetcher(hour, clientId)
      .then((tooltip) => {
        this.cellOriginCache.set(key, tooltip);
        this.cellOriginError.delete(key);
      })
      .catch(() => {
        this.cellOriginError.add(key);
      })
      .finally(() => {
        this.cellOriginLoading.delete(key);
      });
  }
}

