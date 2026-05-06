import { Component, ElementRef, HostListener, OnDestroy, ViewChild, effect, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { EnergyMeasurementService } from './features/metrics/services/energy-measurement.service';
import { FileProcessingNotificationsService } from './features/metrics/services/file-processing-notifications.service';
import { FileRecordEventsService } from './features/metrics/services/file-record-events.service';
import { FileRecordEventItem } from './features/metrics/models/file-record-event.types';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnDestroy {
  protected readonly title = signal('c4e-dashboard');
  protected readonly measurementsService = inject(EnergyMeasurementService);
  protected readonly notificationsService = inject(FileProcessingNotificationsService);
  protected readonly fileRecordEventsService = inject(FileRecordEventsService);

  // Estados para los dropdowns
  showVistasDropdown = signal(false);
  showArchivosDropdown = signal(false);
  showNotificationsPanel = signal(false);
  unreadNotificationsCount = signal(0);
  notificationsPanelTop = signal(84);
  notificationsPanelLeft = signal(16);
  private eventsDirty = signal(true);

  private readonly knownRealtimeIds = new Set<number>();
  private refreshDebounceTimer: ReturnType<typeof setTimeout> | null = null;
  private refreshingEvents = false;
  private refreshQueued = false;

  @ViewChild('notificationsButton')
  private notificationsButtonRef?: ElementRef<HTMLButtonElement>;

  @ViewChild('notificationsPanel')
  private notificationsPanelRef?: ElementRef<HTMLElement>;

  constructor() {
    // Cargar por defecto el cliente 3215 al iniciar la aplicación
    this.selectClient('3215');
    this.notificationsService.connect();

    effect(() => {
      const realtimeItems = this.notificationsService.notifications();
      let newItems = 0;

      for (const item of realtimeItems) {
        if (!this.knownRealtimeIds.has(item.id)) {
          this.knownRealtimeIds.add(item.id);
          newItems++;
        }
      }

      if (newItems > 0) {
        this.unreadNotificationsCount.update((current) => Math.min(current + newItems, 99));
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

  formatEventDate(value: string | null): string {
    if (!value) return 'Ahora';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return 'Ahora';
    return date.toLocaleString('es-MX', {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  eventSnippet(item: FileRecordEventItem): string {
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
    return 'Evento registrado correctamente.';
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
    if (!button || typeof window === 'undefined') {
      return;
    }

    const rect = button.getBoundingClientRect();
    const panelWidth = this.notificationsPanelRef?.nativeElement.offsetWidth ?? Math.min(430, window.innerWidth - 16);
    const horizontalMargin = 8;
    const topOffset = 8;

    const idealLeft = rect.right - panelWidth;
    const maxLeft = Math.max(horizontalMargin, window.innerWidth - panelWidth - horizontalMargin);
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
