import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { formatDashboardDateTime } from '../../../core/date-time-format';
import { FileRecordsService } from '../services/file-records.service';
import { FILE_RECORD_ORIGIN_OPTIONS, FILE_RECORD_STATUS_OPTIONS, FILE_RECORD_TYPE_OPTIONS } from '../models/file-record-filter-options';
import { FileRecordFilters } from '../models/file-record.types';

@Component({
  selector: 'app-file-records-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Historial de archivos procesados</h1>

      <div class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-body">
          <div class="row g-3">
            <div class="col-md-3">
              <label class="form-label">Nombre de archivo</label>
              <input class="form-control" [(ngModel)]="filters.filename" placeholder="original o final" />
            </div>
            <div class="col-md-2">
              <label class="form-label">Estado</label>
              <select class="form-select" [(ngModel)]="filters.status">
                <option value="">Todos</option>
                @for (status of statusOptions; track status) {
                  <option [value]="status">{{ status }}</option>
                }
              </select>
            </div>
            <div class="col-md-2">
              <label class="form-label">Origen</label>
              <select class="form-select" [(ngModel)]="filters.origin">
                <option value="">Todos</option>
                @for (origin of originOptions; track origin) {
                  <option [value]="origin">{{ origin }}</option>
                }
              </select>
            </div>
            <div class="col-md-2">
              <label class="form-label">Tipo</label>
              <select class="form-select" [(ngModel)]="filters.fileType">
                <option value="">Todos</option>
                @for (fileType of fileTypeOptions; track fileType) {
                  <option [value]="fileType">{{ fileType }}</option>
                }
              </select>
            </div>
            <div class="col-md-3 d-flex align-items-end gap-2">
              <button class="btn btn-primary" (click)="applyFilters()" [disabled]="service.loading()">Buscar</button>
              <button class="btn btn-outline-secondary" (click)="clearFilters()" [disabled]="service.loading()">Limpiar</button>
            </div>
          </div>
        </div>
      </div>

      @if (service.error()) {
        <div class="alert alert-danger">{{ service.error() }}</div>
      }

      @if (service.loading()) {
        <div class="d-flex align-items-center gap-2 mb-3">
          <div class="spinner-border spinner-border-sm" role="status"></div>
          <span>Cargando historial...</span>
        </div>
      }

      <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
        <div class="table-responsive">
          <table class="table table-sm align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>ID</th>
                <th>Archivo final</th>
                <th>Tipo</th>
                <th>Estado</th>
                <th>Origen</th>
                <th>Proc./Def.</th>
                <th>Duracion (ms)</th>
                <th>Creado</th>
              </tr>
            </thead>
            <tbody>
              @if (service.records()?.data?.length) {
                @for (item of service.records()!.data; track item.id) {
                  <tr>
                    <td>{{ item.id }}</td>
                    <td>
                      <div class="fw-medium">{{ item.finalFilename }}</div>
                      @if (item.originalFilename && item.originalFilename !== item.finalFilename) {
                        <small class="text-muted">Original: {{ item.originalFilename }}</small>
                      }
                    </td>
                    <td>{{ item.type || '-' }}</td>
                    <td>{{ item.status || '-' }}</td>
                    <td>{{ item.origin || '-' }}</td>
                    <td>{{ item.processedRecords ?? 0 }} / {{ item.defectedRecords ?? 0 }}</td>
                    <td>{{ item.processingDurationMs ?? '-' }}</td>
                    <td>{{ item.createdAt ? formatDate(item.createdAt) : '-' }}</td>
                  </tr>
                }
              } @else {
                <tr>
                  <td colspan="8" class="text-center text-muted py-4">No hay registros para los filtros seleccionados.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex justify-content-between align-items-center mt-3" *ngIf="service.records()">
        <small class="text-muted">Total: {{ service.records()!.totalElements }} archivos</small>
        <div class="d-flex gap-2">
          <button class="btn btn-sm btn-outline-secondary" (click)="prevPage()" [disabled]="service.records()!.first || service.loading()">Anterior</button>
          <span class="align-self-center">Página {{ service.records()!.page + 1 }} de {{ service.records()!.totalPages || 1 }}</span>
          <button class="btn btn-sm btn-outline-secondary" (click)="nextPage()" [disabled]="service.records()!.last || service.loading()">Siguiente</button>
        </div>
      </div>
    </div>
  `
})
export class FileRecordsPage implements OnInit {
  readonly service = inject(FileRecordsService);
  readonly statusOptions = FILE_RECORD_STATUS_OPTIONS;
  readonly originOptions = FILE_RECORD_ORIGIN_OPTIONS;
  readonly fileTypeOptions = FILE_RECORD_TYPE_OPTIONS;

  readonly pageSize = 20;
  readonly currentPage = signal(0);
  readonly filters: FileRecordFilters = {
    filename: '',
    status: '',
    origin: '',
    fileType: ''
  };

  ngOnInit(): void {
    this.loadPage(0);
  }

  applyFilters(): void {
    this.loadPage(0);
  }

  clearFilters(): void {
    this.filters.filename = '';
    this.filters.status = '';
    this.filters.origin = '';
    this.filters.fileType = '';
    this.loadPage(0);
  }

  prevPage(): void {
    const next = Math.max(this.currentPage() - 1, 0);
    this.loadPage(next);
  }

  nextPage(): void {
    const page = this.service.records()?.page ?? this.currentPage();
    this.loadPage(page + 1);
  }

  formatDate(value: string): string {
    return formatDashboardDateTime(value);
  }

  private loadPage(page: number): void {
    this.currentPage.set(page);
    this.service.fetchFileRecords(page, this.pageSize, {
      filename: this.filters.filename || undefined,
      status: this.filters.status || undefined,
      origin: this.filters.origin || undefined,
      fileType: this.filters.fileType || undefined
    });
  }
}

