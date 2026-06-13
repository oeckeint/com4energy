import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { FileRecordFilters, FileRecordPageResponse } from '../models/file-record.types';

@Injectable({ providedIn: 'root' })
export class FileRecordsService {
  private readonly http = inject(HttpClient);
  private readonly API_FILE_RECORDS_PATH = '/api/v1/file-records';

  private readonly recordsSignal = signal<FileRecordPageResponse | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly records = this.recordsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  async fetchFileRecords(
    page = 0,
    size = 20,
    filters: FileRecordFilters = {},
    sortBy = 'createdAt',
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
      if (filters.status) params.append('status', filters.status);
      if (filters.origin) params.append('origin', filters.origin);
      if (filters.fileType) params.append('fileType', filters.fileType);
      if (filters.startDate) params.append('startDate', filters.startDate);
      if (filters.endDate) params.append('endDate', filters.endDate);

      const response = await firstValueFrom(
        this.http.get<FileRecordPageResponse>(`${this.API_FILE_RECORDS_PATH}?${params.toString()}`)
      );

      this.recordsSignal.set(response);
    } catch (err: any) {
      this.errorSignal.set(err?.error?.message || err?.message || 'No se pudo cargar el historial de archivos.');
    } finally {
      this.loadingSignal.set(false);
    }
  }
}

