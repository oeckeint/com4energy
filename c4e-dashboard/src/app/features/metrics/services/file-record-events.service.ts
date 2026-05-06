import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { FileRecordEventFilters, FileRecordEventPageResponse } from '../models/file-record-event.types';

@Injectable({ providedIn: 'root' })
export class FileRecordEventsService {
  private readonly http = inject(HttpClient);
  private readonly API_FILE_RECORD_EVENTS_PATH = '/api/v1/file-record-events';

  private readonly eventsSignal = signal<FileRecordEventPageResponse | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly events = this.eventsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  async fetchEvents(
    page = 0,
    size = 20,
    filters: FileRecordEventFilters = {},
    sortBy = 'receivedAt',
    sortDir: 'asc' | 'desc' = 'desc'
  ): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const params = new URLSearchParams({
        page: String(page),
        size: String(size),
        sortBy,
        sortDir
      });

      if (filters.filename) params.append('filename', filters.filename);
      if (filters.eventType) params.append('eventType', filters.eventType);
      if (filters.status) params.append('status', filters.status);
      if (filters.origin) params.append('origin', filters.origin);
      if (filters.fileType) params.append('fileType', filters.fileType);
      if (filters.startDate) params.append('startDate', filters.startDate);
      if (filters.endDate) params.append('endDate', filters.endDate);

      const response = await firstValueFrom(
        this.http.get<FileRecordEventPageResponse>(`${this.API_FILE_RECORD_EVENTS_PATH}?${params.toString()}`)
      );

      this.eventsSignal.set(response);
    } catch (err: any) {
      this.errorSignal.set(err?.error?.message || err?.message || 'No se pudo cargar el historial de eventos.');
    } finally {
      this.loadingSignal.set(false);
    }
  }
}

