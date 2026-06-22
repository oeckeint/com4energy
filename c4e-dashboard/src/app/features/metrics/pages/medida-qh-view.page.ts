import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MedidaQHService } from '../services/medidas/qh/MedidaQHService';
import { MeasureActiveFilterPill, MeasureMatrixTableComponent } from '../components/measure-matrix-table.component';
import { MeasureFiltersComponent } from '../components/measure-filters.component';
import { MeasureHourlyMiniChartComponent } from '../components/measure-hourly-mini-chart.component';
import { MeasureCompletenessGaugeComponent } from '../components/measure-completeness-gauge.component';
import { MeasureAuditorPanelComponent } from '../components/measure-auditor-panel.component';
import { ClienteInfoCardComponent } from '../components/cliente-info-card.component';

const SLOT_LABEL_REGEX = /^(\d{2}):(\d{2})$/;
const VALID_MINUTES = [0, 15, 30, 45];

@Component({
  selector: 'app-medida-qh-page',
  standalone: true,
  imports: [CommonModule, MeasureFiltersComponent, MeasureCompletenessGaugeComponent, MeasureHourlyMiniChartComponent, MeasureAuditorPanelComponent, MeasureMatrixTableComponent, ClienteInfoCardComponent],
  templateUrl: './medida-qh.page.html',
  styleUrls: ['./measure-view.css']
})
export class MedidaQHPage implements OnInit, OnDestroy {
  private readonly scrollbarHideDelayMs = 900;
  private stackScrollbarTimer: ReturnType<typeof setTimeout> | null = null;
  calendarSyncToken = 0;
  selectedClienteId: number | null = null;
  private appliedFilterSnapshot = {
    clientIdsKey: '',
    clientIdsDisplay: '',
    tarifa: '',
    tarifaDisplay: ''
  };
  readonly service = inject(MedidaQHService);
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

  readonly cellOriginFetcher = (slot: string, clientId: number): Promise<string | null> =>
    this.fetchCellOriginTooltip(slot, clientId);

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

  selectClienteInfo(clienteId: number): void {
    this.selectedClienteId = clienteId;
    this.activeTool = 'auditor';
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

  private async fetchCellOriginTooltip(slotLabel: string, clientId: number): Promise<string | null> {
    const selectedDay = this.filters.day || this.todayIso();
    const slot = this.parseSlotLabel(slotLabel);
    if (slot === null) {
      return 'Origen no disponible';
    }

    try {
      const response = await this.service.fetchCellOrigins(selectedDay, clientId, slot.hour, slot.minute);
      const sourceCount = response.origenesDistintos ?? 0;
      const sources = response.origenes ?? [];

      if (sourceCount <= 0 || sources.length === 0) {
        return 'Origen no informado';
      }

      if (sourceCount === 1) {
        const [only] = sources;
        const created = this.formatOriginDate(only.fechaCreacion);
        return created
          ? `Origen: ${only.nombre}\nCreado: ${created}`
          : `Origen: ${only.nombre}`;
      }

      const preview = sources
        .slice(0, 3)
        .map(source => {
          const created = this.formatOriginDate(source.fechaCreacion);
          return created ? `• ${source.nombre} — ${created}` : `• ${source.nombre}`;
        })
        .join('\n');
      const remaining = Math.max(sourceCount - 3, 0);
      const extra = remaining > 0 ? `\n+${remaining} mas` : '';
      return `Origenes (${sourceCount}):\n${preview}${extra}`;
    } catch {
      return 'No se pudo cargar el origen del archivo';
    }
  }

  /** Formatea la fecha de creacion del archivo (ISO del backend) a dd/MM/yyyy HH:mm. */
  private formatOriginDate(raw: string | null): string {
    if (!raw) {
      return '';
    }
    const parsed = new Date(raw);
    if (Number.isNaN(parsed.getTime())) {
      return '';
    }
    const pad = (n: number): string => String(n).padStart(2, '0');
    return `${pad(parsed.getDate())}/${pad(parsed.getMonth() + 1)}/${parsed.getFullYear()} `
      + `${pad(parsed.getHours())}:${pad(parsed.getMinutes())}`;
  }

  /** Parsea la etiqueta de slot "HH:MM" a hora (0-23) y minuto (0/15/30/45). */
  private parseSlotLabel(slotLabel: string): { hour: number; minute: number } | null {
    const match = SLOT_LABEL_REGEX.exec(slotLabel);
    if (!match) {
      return null;
    }

    const hour = Number(match[1]);
    const minute = Number(match[2]);
    const validHour = Number.isInteger(hour) && hour >= 0 && hour <= 23;
    const validMinute = VALID_MINUTES.includes(minute);
    return validHour && validMinute ? { hour, minute } : null;
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

}
