import { Injectable, NgZone, inject, signal } from '@angular/core';
import { FileProcessingNotification } from '../models/file-processing-notification.types';

@Injectable({ providedIn: 'root' })
export class FileProcessingNotificationsService {
  private readonly zone = inject(NgZone);
  private eventSource: EventSource | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private reconnectDelayMs = 1000;
  private readonly MAX_RECONNECT_DELAY_MS = 15000;
  private localNotificationId = -1;

  private readonly notificationsSignal = signal<FileProcessingNotification[]>([]);
  private readonly connectedSignal = signal(false);

  readonly notifications = this.notificationsSignal.asReadonly();
  readonly connected = this.connectedSignal.asReadonly();

  connect(): void {
    if (this.eventSource) return;

    const source = new EventSource('/api/v1/notifications/file-processing/stream');
    this.eventSource = source;

    source.addEventListener('connected', () => {
      this.zone.run(() => {
        this.connectedSignal.set(true);
        this.reconnectDelayMs = 1000;
      });
    });

    source.addEventListener('file-record-event', (event: MessageEvent) => {
      this.zone.run(() => {
        try {
          const notification = JSON.parse(event.data) as FileProcessingNotification;
          this.pushNotification(notification);
        } catch {
          // Ignore malformed payloads to keep stream alive
        }
      });
    });

    source.onerror = () => {
      this.zone.run(() => this.connectedSignal.set(false));
      this.cleanupEventSource();
      this.scheduleReconnect();
    };
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.cleanupEventSource();
    this.connectedSignal.set(false);
  }

  notifyProcessingStarted(files: File[]): void {
    if (!files.length) {
      return;
    }

    files.forEach((file) => {
      this.pushNotification({
        id: this.nextLocalNotificationId(),
        eventType: 'FILE_PROCESSING_STARTED',
        status: 'PROCESSING',
        filename: file.name,
        fileType: null,
        origin: 'API',
        failureReason: null,
        failureReasonDescription: null,
        comment: 'Procesamiento iniciado. Te avisaremos cuando termine.',
        occurredAt: new Date().toISOString(),
        receivedAt: new Date().toISOString()
      });
    });
  }

  removeNotification(id: number): void {
    this.notificationsSignal.update(list => list.filter(n => n.id !== id));
  }

  private pushNotification(notification: FileProcessingNotification): void {
    this.notificationsSignal.update(current => {
      const exists = current.some(n => n.id === notification.id);
      if (exists) return current;
      return [notification, ...current].slice(0, 8);
    });

    setTimeout(() => {
      this.zone.run(() => this.removeNotification(notification.id));
    }, 10000);
  }

  private nextLocalNotificationId(): number {
    return this.localNotificationId--;
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) return;

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
      this.reconnectDelayMs = Math.min(this.reconnectDelayMs * 2, this.MAX_RECONNECT_DELAY_MS);
    }, this.reconnectDelayMs);
  }

  private cleanupEventSource(): void {
    if (!this.eventSource) return;
    this.eventSource.close();
    this.eventSource = null;
  }
}
