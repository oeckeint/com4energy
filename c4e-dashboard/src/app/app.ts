import { Component, ElementRef, HostListener, OnDestroy, ViewChild, effect, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { EnergyMeasurementService } from './features/metrics/services/energy-measurement.service';
import { FileProcessingNotificationsService } from './features/metrics/services/file-processing-notifications.service';
import { FileRecordEventsService } from './features/metrics/services/file-record-events.service';
import { FileRecordEventItem } from './features/metrics/models/file-record-event.types';
import { formatDashboardDateTime } from './core/date-time-format';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnDestroy {
  protected readonly title = signal('c4e-dashboard');
  protected readonly measurementsService = inject(EnergyMeasurementService);
  protected readonly notificationsService = inject(FileProcessingNotificationsService);
  protected readonly fileRecordEventsService = inject(FileRecordEventsService);

  // Estados para los dropdowns
  readonly showVistasDropdown = signal(false);
  readonly showArchivosDropdown = signal(false);
  readonly showNotificationsPanel = signal(false);
  readonly unreadNotificationsCount = signal(0);
  readonly notificationsPanelTop = signal(84);
  readonly notificationsPanelLeft = signal(16);
  private readonly eventsDirty = signal(true);

  private readonly knownRealtimeIds = new Set<number>();
  private refreshDebounceTimer: ReturnType<typeof setTimeout> | null = null;
  private refreshingEvents = false;
  private refreshQueued = false;

  @ViewChild('notificationsButton')
  private readonly notificationsButtonRef?: ElementRef<HTMLButtonElement>;

  @ViewChild('notificationsPanel')
  private readonly notificationsPanelRef?: ElementRef<HTMLElement>;

  @ViewChild('vistasDropdownRoot')
  private readonly vistasDropdownRootRef?: ElementRef<HTMLElement>;

  @ViewChild('archivosDropdownRoot')
  private readonly archivosDropdownRootRef?: ElementRef<HTMLElement>;

  constructor() {
    // Cargar por defecto el cliente 3215 al iniciar la aplicación
    this.selectClient('3215');
    this.notificationsService.connect();

    effect(() => {
      const realtimeItems = this.notificationsService.notifications();
      let newTerminalItems = 0;

      for (const item of realtimeItems) {
        if (!this.knownRealtimeIds.has(item.id)) {
          this.knownRealtimeIds.add(item.id);
          if (this.isTerminalStatus(item.status)) {
            newTerminalItems++;
          }
        }
      }

      if (newTerminalItems > 0) {
        this.unreadNotificationsCount.update((current) => Math.min(current + newTerminalItems, 99));
        this.eventsDirty.set(true);
        this.scheduleEventsRefresh();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.refreshDebounceTimer !== null) {
      clearTimeout(this.refreshDebounceTimer);
      this.refreshDebounceTimer = null;
    }
  }

  selectClient(clientId: string) {
    const id = Number(clientId);
    if (!Number.isNaN(id)) {
      this.measurementsService.fetchMeasurements(0, 20, id);
    }
  }

  async toggleNotificationsPanel(): Promise<void> {
    const next = !this.showNotificationsPanel();
    this.showNotificationsPanel.set(next);

    if (next) {
      this.positionNotificationsPanel();
      requestAnimationFrame(() => this.positionNotificationsPanel());
      this.unreadNotificationsCount.set(0);
      await this.refreshEventsList(true);
    }
  }

  closeNotificationsPanel(): void {
    this.showNotificationsPanel.set(false);
  }

  async reloadNotifications(): Promise<void> {
    this.positionNotificationsPanel();
    await this.refreshEventsList(true);
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    if (this.showNotificationsPanel()) {
      this.positionNotificationsPanel();
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as Node | null;
    if (!target) {
      return;
    }

    const clickedInsideVistas = this.vistasDropdownRootRef?.nativeElement.contains(target) ?? false;
    const clickedInsideArchivos = this.archivosDropdownRootRef?.nativeElement.contains(target) ?? false;

    if (!clickedInsideVistas) {
      this.showVistasDropdown.set(false);
    }

    if (!clickedInsideArchivos) {
      this.showArchivosDropdown.set(false);
    }
  }

  formatEventDate(value: string | null): string {
    return formatDashboardDateTime(value, {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }

  notificationPanelItems(): FileRecordEventItem[] {
    const events = this.fileRecordEventsService.events()?.data ?? [];
    return events.filter(item => this.isTerminalStatus(item.status));
  }

  toastItems() {
    return this.notificationsService.notifications().filter(item => {
      const status = (item.status || '').toUpperCase();
      const isRegistered = item.eventType === 'FILE_REGISTERED';
      const isMeasureFinished =
        (item.eventType === 'FILE_PROCESSING_PROCESSED' || item.eventType === 'FILE_MEASURE_PROCESSED') &&
        this.isTerminalStatus(status);
      return isRegistered || isMeasureFinished;
    });
  }

  isCriticalEvent(item: FileRecordEventItem): boolean {
    const status = (item.status || '').toUpperCase();
    if (status === 'SUCCEEDED') {
      return false;
    }
    return !!item.failureReason || status === 'FAILED' || status === 'REJECTED' || status === 'ERROR';
  }

  eventSnippet(item: FileRecordEventItem): string {
    if (item.eventType === 'FILE_MEASURE_PROCESSED' && (item.status || '').toUpperCase() === 'SUCCEEDED') {
      const summary = this.buildMeasureProcessedSummary(item.metadataJson);
      if (summary) {
        return summary;
      }
      return 'Procesamiento completado correctamente.';
    }

    if (item.failureReasonDescription) return item.failureReasonDescription;
    if (item.failureReason) return item.failureReason;
    if (item.metadataJson) {
      try {
        const obj = JSON.parse(item.metadataJson) as Record<string, unknown>;
        const firstEntry = Object.entries(obj)[0];
        if (firstEntry) return `${firstEntry[0]}: ${String(firstEntry[1])}`;
      } catch {
        return item.metadataJson;
      }
    }

    if ((item.status || '').toUpperCase() === 'SUCCEEDED') {
      return 'Procesamiento finalizado correctamente.';
    }

    return 'Evento registrado correctamente.';
  }

  private buildMeasureProcessedSummary(metadataJson: string | null): string | null {
    if (!metadataJson) {
      return null;
    }

    try {
      const metadata = JSON.parse(metadataJson) as {
        persisted?: number;
        total?: number;
        defects?: number;
        totalMs?: number;
      };

      const persisted = typeof metadata.persisted === 'number' ? metadata.persisted : null;
      const total = typeof metadata.total === 'number' ? metadata.total : null;
      const defects = typeof metadata.defects === 'number' ? metadata.defects : null;
      const totalMs = typeof metadata.totalMs === 'number' ? metadata.totalMs : null;

      if (persisted === null && total === null) {
        return null;
      }

      const counts = total !== null ? `${persisted ?? 0}/${total}` : String(persisted ?? 0);
      const defectsPart = defects !== null ? `, defectos: ${defects}` : '';
      const durationPart = totalMs !== null ? `, ${totalMs} ms` : '';
      return `Procesado: ${counts} registros${defectsPart}${durationPart}.`;
    } catch {
      return null;
    }
  }

  private isTerminalStatus(status: string | null | undefined): boolean {
    const value = (status || '').toUpperCase();
    return value === 'SUCCEEDED' || value === 'FAILED' || value === 'REJECTED' || value === 'ERROR';
  }

  private scheduleEventsRefresh(): void {
    if (this.refreshDebounceTimer !== null) {
      clearTimeout(this.refreshDebounceTimer);
    }

    this.refreshDebounceTimer = setTimeout(() => {
      this.refreshDebounceTimer = null;
      void this.refreshEventsList();
    }, 1200);
  }

  private positionNotificationsPanel(): void {
    const button = this.notificationsButtonRef?.nativeElement;
    if (!button || typeof globalThis.window === 'undefined') {
      return;
    }

    const rect = button.getBoundingClientRect();
    const panelWidth = this.notificationsPanelRef?.nativeElement.offsetWidth ?? Math.min(430, globalThis.window.innerWidth - 16);
    const horizontalMargin = 8;
    const topOffset = 8;

    const idealLeft = rect.right - panelWidth;
    const maxLeft = Math.max(horizontalMargin, globalThis.window.innerWidth - panelWidth - horizontalMargin);
    const left = Math.min(Math.max(idealLeft, horizontalMargin), maxLeft);
    const top = rect.bottom + topOffset;

    this.notificationsPanelLeft.set(left);
    this.notificationsPanelTop.set(top);
  }

  private async refreshEventsList(force = false): Promise<void> {
    if (!force && !this.eventsDirty()) {
      return;
    }

    if (this.refreshingEvents) {
      this.refreshQueued = true;
      return;
    }

    this.refreshingEvents = true;
    try {
      await this.fileRecordEventsService.fetchEvents(0, 20, {});
      this.eventsDirty.set(false);
    } finally {
      this.refreshingEvents = false;
      if (this.refreshQueued) {
        this.refreshQueued = false;
        this.eventsDirty.set(true);
        void this.refreshEventsList();
      }
    }
  }
}
