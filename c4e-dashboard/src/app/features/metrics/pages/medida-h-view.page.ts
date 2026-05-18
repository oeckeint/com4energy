import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MedidaHService } from '../services/medidas/h/MedidaHService';
import { MeasureActiveFilterPill, MeasureMatrixTableComponent } from '../components/measure-matrix-table.component';
import { MeasureFiltersComponent } from '../components/measure-filters.component';
import { MeasureHourlyMiniChartComponent } from '../components/measure-hourly-mini-chart.component';
import { MeasureCompletenessGaugeComponent } from '../components/measure-completeness-gauge.component';
import { MeasureAuditorPanelComponent } from '../components/measure-auditor-panel.component';

const HOUR_LABEL_REGEX = /^(\d{2}):\d{2}$/;

@Component({
  selector: 'app-medida-h-page',
  standalone: true,
  imports: [CommonModule, MeasureFiltersComponent, MeasureCompletenessGaugeComponent, MeasureHourlyMiniChartComponent, MeasureAuditorPanelComponent, MeasureMatrixTableComponent],
  templateUrl: './medida-h.page.html',
  styles: [`
    .measure-h-top-layout {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      align-items: stretch;
      gap: 1rem;
      margin-bottom: 0.75rem;
    }

    app-measure-matrix-table.measure-h-matrix-block {
      display: block;
      margin-top: 0.75rem;
    }

    .measure-h-top-left,
    .measure-h-top-right {
      min-width: 0;
      height: 100%;
    }

    .measure-h-top-filters {
      display: block;
      min-width: 0;
      height: 100%;
    }


    .measure-h-widgets-grid {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 2fr);
      gap: 1rem;
      align-items: stretch;
      height: 100%;
    }

    .measure-h-top-widget {
      display: flex;
      align-items: stretch;
      justify-content: flex-start;
      height: 100%;
    }

    .measure-h-top-filters app-measure-filters,
    .measure-h-top-widget app-measure-completeness-gauge,
    .measure-h-top-widget app-measure-hourly-mini-chart {
      display: block;
      width: 100%;
      height: 100%;
    }

    .measure-h-tools-rail {
      position: fixed;
      right: 0;
      top: 0;
      bottom: 0;
      z-index: 1402;
      width: 44px;
      border-left: 1px solid #e2e8f0;
      border-top: 1px solid #e2e8f0;
      border-bottom: 1px solid #e2e8f0;
      border-radius: 0;
      background: #f8fafc;
      padding: 72px 5px 12px;
      display: flex;
      flex-direction: column;
      gap: 6px;
      align-items: center;
    }

    .measure-h-tools-button {
      width: 32px;
      height: 32px;
      border: 1px solid transparent;
      border-radius: 6px;
      background: transparent;
      color: #6b7280;
      font-size: 16px;
      line-height: 1;
      display: grid;
      place-items: center;
      transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
    }

    .measure-h-tools-button:hover {
      background: #e2e8f0;
      color: #334155;
    }

    .measure-h-tools-button:focus-visible {
      outline: 2px solid #93c5fd;
      outline-offset: 1px;
    }

    .measure-h-tools-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.26);
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.2s ease;
      z-index: 1398;
    }

    .measure-h-tools-drawer {
      position: fixed;
      top: 0;
      right: 44px;
      width: min(440px, 88vw);
      height: 100vh;
      background: #ffffff;
      border-left: 1px solid #e5e7eb;
      box-shadow: -14px 0 28px rgba(15, 23, 42, 0.2);
      transform: translateX(calc(100% + 44px));
      transition: transform 0.24s ease;
      z-index: 1401;
      display: flex;
      flex-direction: column;
    }

    .measure-h-tools-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 10px 12px;
      border-bottom: 1px solid #e5e7eb;
      background: #f8fafc;
    }

    .measure-h-tools-close {
      border: 1px solid #d1d5db;
      border-radius: 6px;
      background: #ffffff;
      color: #374151;
      width: 28px;
      height: 28px;
      line-height: 1;
    }

    .measure-h-tools-body {
      flex: 1;
      min-height: 0;
      padding: 12px;
      overflow: hidden;
      box-sizing: border-box;
    }


    .measure-h-tools-stack {
      display: flex;
      flex-direction: column;
      gap: 10px;
      width: 100%;
      height: 100%;
      overflow-y: auto;
      padding-right: 8px;
      scrollbar-width: none;
      -ms-overflow-style: none;
      box-sizing: border-box;
    }

    .measure-h-tools-stack::-webkit-scrollbar {
      width: 8px;
    }

    .measure-h-tools-stack::-webkit-scrollbar-track {
      background: transparent;
    }

    .measure-h-tools-stack::-webkit-scrollbar-thumb {
      background: transparent;
      border-radius: 999px;
    }

    .measure-h-tools-body app-measure-auditor-panel {
      display: block;
      height: auto;
      width: 100%;
    }

    .measure-h-tools-placeholder-card {
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      background: #ffffff;
      padding: 12px;
      box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08), 0 2px 6px rgba(15, 23, 42, 0.05);
    }

    .measure-h-tools-placeholder-card-title {
      font-size: 13px;
      font-weight: 600;
      color: #0f172a;
      margin-bottom: 4px;
    }

    .measure-h-tools-placeholder-card-text {
      font-size: 12px;
      color: #64748b;
      line-height: 1.35;
    }

    .measure-h-tools-placeholder {
      border: 1px dashed #cbd5e1;
      border-radius: 8px;
      background: #f8fafc;
      color: #64748b;
      padding: 14px;
      font-size: 13px;
    }

    @media (max-width: 1200px) {
      .measure-h-top-layout {
        grid-template-columns: 1fr;
      }

      .measure-h-top-left,
      .measure-h-top-right {
        height: auto;
      }
    }

    @media (max-width: 900px) {
      .measure-h-widgets-grid {
        grid-template-columns: 1fr;
        gap: 0.75rem;
      }

      .measure-h-top-widget {
        justify-content: flex-start;
      }
    }

    @media (max-width: 768px) {
      .measure-h-top-layout {
        gap: 0.75rem;
      }

      .measure-h-tools-rail {
        top: 0;
        bottom: 0;
        padding-top: 72px;
      }
    }
  `]
})
export class MedidaHPage implements OnInit, OnDestroy {
  private readonly scrollbarHideDelayMs = 900;
  private stackScrollbarTimer: ReturnType<typeof setTimeout> | null = null;
  calendarSyncToken = 0;
  private appliedFilterSnapshot = {
    clientIdsKey: '',
    clientIdsDisplay: '',
    tarifa: '',
    tarifaDisplay: ''
  };
  readonly service = inject(MedidaHService);
  activeTool: 'auditor' | 'utilidades' | null = null;
  auditorFocusTrigger = 0;
  showStackScrollbar = false;

  get statusLabel(): string | null {
    const clients = this.service.uniqueClients();
    if (clients === 0 || this.service.totalRecords() === 0) return null;
    const clientLabel = clients === 1 ? 'cliente' : 'clientes';
    const present = this.service.totalPresent();
    const expected = this.service.totalExpected();
    return `${clients} ${clientLabel} · ${present}/${expected} registros`;
  }

  get statusHasDiscrepancy(): boolean {
    if (!this.statusLabel) {
      return false;
    }

    return this.service.totalPresent() < this.service.totalExpected();
  }

  get activeFilterPills(): MeasureActiveFilterPill[] {
    const parts: MeasureActiveFilterPill[] = [];

    const tarifa = this.appliedFilterSnapshot.tarifaDisplay;
    if (tarifa) {
      parts.push({ key: 'tarifa', label: `Tarifa: ${tarifa}` });
    }

    const appliedClientIds = this.appliedFilterSnapshot.clientIdsKey
      ? this.appliedFilterSnapshot.clientIdsKey.split(',').filter((token) => token.length > 0)
      : [];
    if (appliedClientIds.length > 0) {
      const clientLabel = appliedClientIds.length <= 3
        ? this.appliedFilterSnapshot.clientIdsDisplay
        : `${appliedClientIds.length} clientes filtrados`;
      parts.push({ key: 'clientes', label: appliedClientIds.length <= 3 ? `Clientes: ${clientLabel}` : clientLabel });
    }

    return parts;
  }

  get hasPendingSearchChanges(): boolean {
    return this.normalizeClientFilterText(this.filters.clientIdsText) !== this.appliedFilterSnapshot.clientIdsKey
      || this.normalizeTarifa(this.filters.selectedTarifa) !== this.appliedFilterSnapshot.tarifa
      || this.normalizeTarifa(this.filters.selectedTarifa) !== this.appliedFilterSnapshot.tarifaDisplay;
  }

  get hasActiveFilters(): boolean {
    return this.normalizeClientFilterText(this.filters.clientIdsText).length > 0
      || this.normalizeTarifa(this.filters.selectedTarifa).length > 0;
  }

  get toolsPanelOpen(): boolean {
    return this.activeTool !== null;
  }

  get activeToolTitle(): string {
    if (this.activeTool === 'auditor') return 'Estado de clientes';
    if (this.activeTool === 'utilidades') return 'Utilidades';
    return 'Herramientas';
  }

  readonly filters = {
    day: this.todayIso(),
    clientIdsText: '',
    selectedTarifa: ''
  };

  readonly cellOriginFetcher = (hour: string, clientId: number): Promise<string | null> =>
    this.fetchCellOriginTooltip(hour, clientId);

  ngOnInit(): void {
    void this.initializePageData();
  }

  private async initializePageData(): Promise<void> {
    await Promise.all([
      this.service.fetchTarifas(),
      this.applyFilters()
    ]);
  }

  ngOnDestroy(): void {
    if (this.stackScrollbarTimer !== null) {
      clearTimeout(this.stackScrollbarTimer);
      this.stackScrollbarTimer = null;
    }
  }

  async onDayChange(newDay: string): Promise<void> {
    this.filters.day = newDay;
    await this.applyFilters();
  }

  async shiftTableDay(offsetDays: number): Promise<void> {
    const baseDate = new Date(this.filters.day || this.todayIso());
    baseDate.setDate(baseDate.getDate() + offsetDays);
    const nextDay = baseDate.toISOString().slice(0, 10);
    await this.onDayChange(nextDay);
  }

  async onTableDateSelected(newDay: string): Promise<void> {
    await this.onDayChange(newDay);
  }

  focusCalendarOnCurrentTableDay(): void {
    this.calendarSyncToken += 1;
  }

  async onTarifaChange(tarifa: string): Promise<void> {
    this.filters.selectedTarifa = tarifa;
    await this.applyFilters();
  }

  async onActiveFilterRemoved(filterKey: 'tarifa' | 'clientes'): Promise<void> {
    if (filterKey === 'tarifa') {
      if (!this.filters.selectedTarifa) {
        return;
      }
      this.filters.selectedTarifa = '';
      await this.applyFilters();
      return;
    }

    if (!this.filters.clientIdsText.trim()) {
      return;
    }

    this.filters.clientIdsText = '';
    await this.applyFilters();
  }

  async applyFilters(): Promise<void> {
    const clientIds = this.parseClientIds(this.filters.clientIdsText);
    await this.service.fetchMatrix(
      this.filters.day || this.todayIso(),
      clientIds,
      this.filters.selectedTarifa
    );

    if (!this.service.error()) {
      this.captureAppliedFilterSnapshot();
    }
  }

  async clearFilters(): Promise<void> {
    this.filters.clientIdsText = '';
    this.filters.selectedTarifa = '';
    const selectedDay = this.filters.day || this.todayIso();
    this.filters.day = selectedDay;
    await this.service.fetchMatrix(selectedDay, []);

    if (!this.service.error()) {
      this.captureAppliedFilterSnapshot();
    }
  }

  toggleTool(tool: 'auditor' | 'utilidades'): void {
    this.activeTool = this.activeTool === tool ? null : tool;
  }

  closeToolsPanel(): void {
    this.activeTool = null;
  }

  openAuditorDefectuosos(): void {
    this.activeTool = 'auditor';
    this.auditorFocusTrigger += 1;
  }

  onToolsStackScroll(): void {
    this.showStackScrollbar = true;
    if (this.stackScrollbarTimer !== null) {
      clearTimeout(this.stackScrollbarTimer);
    }
    this.stackScrollbarTimer = setTimeout(() => {
      this.showStackScrollbar = false;
      this.stackScrollbarTimer = null;
    }, this.scrollbarHideDelayMs);
  }

  private parseClientIds(raw: string): number[] {
    if (!raw) return [];

    const parsed = raw
      .split(',')
      .map((token) => token.trim())
      .filter((token) => token.length > 0)
      .map(Number)
      .filter((id) => Number.isInteger(id) && id > 0);

    return Array.from(new Set(parsed));
  }

  private captureAppliedFilterSnapshot(): void {
    this.appliedFilterSnapshot = {
      clientIdsKey: this.normalizeClientFilterText(this.filters.clientIdsText),
      clientIdsDisplay: this.normalizeClientFilterDisplay(this.filters.clientIdsText),
      tarifa: this.normalizeTarifa(this.filters.selectedTarifa),
      tarifaDisplay: this.normalizeTarifa(this.filters.selectedTarifa)
    };
  }

  private normalizeClientFilterText(raw: string): string {
    return this.parseClientIds(raw).sort((a, b) => a - b).join(',');
  }

  private normalizeClientFilterDisplay(raw: string): string {
    return this.parseClientIds(raw).join(', ');
  }

  private normalizeTarifa(tarifa: string): string {
    return tarifa?.trim() ?? '';
  }

  private async fetchCellOriginTooltip(hourLabel: string, clientId: number): Promise<string | null> {
    const selectedDay = this.filters.day || this.todayIso();
    const parsedHour = this.parseHourLabel(hourLabel);
    if (parsedHour === null) {
      return 'Origen no disponible';
    }

    try {
      const response = await this.service.fetchCellOrigins(selectedDay, clientId, parsedHour);
      const sourceCount = response.origenesDistintos ?? 0;
      const sources = response.origenes ?? [];

      if (sourceCount <= 0 || sources.length === 0) {
        return 'Origen no informado';
      }

      if (sourceCount === 1) {
        return `Origen: ${sources[0]}`;
      }

      const preview = sources.slice(0, 3).join(', ');
      const remaining = Math.max(sourceCount - 3, 0);
      const extra = remaining > 0 ? ` +${remaining} mas` : '';
      return `Origenes (${sourceCount}): ${preview}${extra}`;
    } catch {
      return 'No se pudo cargar el origen del archivo';
    }
  }

  private parseHourLabel(hourLabel: string): number | null {
    const match = HOUR_LABEL_REGEX.exec(hourLabel);
    if (!match) {
      return null;
    }

    const hour = Number(match[1]);
    return Number.isInteger(hour) && hour >= 0 && hour <= 23 ? hour : null;
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

}
