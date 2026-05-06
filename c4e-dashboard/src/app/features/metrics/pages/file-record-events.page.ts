import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  FILE_RECORD_EVENT_STATUS_OPTIONS,
  FILE_RECORD_EVENT_TYPE_OPTIONS,
  FILE_RECORD_ORIGIN_OPTIONS
} from '../models/file-record-filter-options';
import { FileRecordEventFilters } from '../models/file-record-event.types';
import { FileRecordEventsService } from '../services/file-record-events.service';

@Component({
  selector: 'app-file-record-events-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-4">Eventos críticos de archivos</h1>

      <div class="card border-0 shadow-sm rounded-4 mb-4">
        <div class="card-body">
          <div class="row g-3">
            <div class="col-md-3">
              <label class="form-label">Nombre de archivo</label>
              <input class="form-control" [(ngModel)]="filters.filename" placeholder="archivo" />
            </div>
            <div class="col-md-2">
              <label class="form-label">Evento</label>
              <select class="form-select" [(ngModel)]="filters.eventType">
                <option value="">Todos</option>
                @for (eventType of eventTypeOptions; track eventType) {
                  <option [value]="eventType">{{ eventType }}</option>
                }
              </select>
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
          <span>Cargando eventos...</span>
        </div>
      }

      <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
        <div class="table-responsive">
          <table class="table table-sm align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th>ID</th>
                <th>Evento</th>
                <th>Archivo</th>
                <th>Estado</th>
                <th>Tipo</th>
                <th>Origen</th>
                <th>Descripción</th>
                <th>Recibido</th>
              </tr>
            </thead>
            <tbody>
              @if (service.events()?.data?.length) {
                @for (item of service.events()!.data; track item.id) {
                  <tr>
                    <td>{{ item.id }}</td>
                    <td>{{ item.eventType }}</td>
                    <td>
                      <div class="fw-medium">{{ item.filename }}</div>
                    </td>
                    <td>{{ item.status }}</td>
                    <td>{{ item.fileType || '-' }}</td>
                    <td>{{ item.origin || '-' }}</td>
                    <td>{{ item.failureReasonDescription || formatMetadata(item.metadataJson) || '-' }}</td>
                    <td>{{ item.receivedAt ? formatDate(item.receivedAt) : '-' }}</td>
                  </tr>
                }
              } @else {
                <tr>
                  <td colspan="8" class="text-center text-muted py-4">No hay eventos para los filtros seleccionados.</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </div>

      <div class="d-flex justify-content-between align-items-center mt-3" *ngIf="service.events()">
        <small class="text-muted">Total: {{ service.events()!.totalElements }} eventos</small>
        <div class="d-flex gap-2">
          <button class="btn btn-sm btn-outline-secondary" (click)="prevPage()" [disabled]="service.events()!.first || service.loading()">Anterior</button>
          <span class="align-self-center">Página {{ service.events()!.page + 1 }} de {{ service.events()!.totalPages || 1 }}</span>
          <button class="btn btn-sm btn-outline-secondary" (click)="nextPage()" [disabled]="service.events()!.last || service.loading()">Siguiente</button>
        </div>
      </div>
    </div>
  `
})
export class FileRecordEventsPage implements OnInit {
  readonly service = inject(FileRecordEventsService);
  readonly eventTypeOptions = FILE_RECORD_EVENT_TYPE_OPTIONS;
  readonly statusOptions = FILE_RECORD_EVENT_STATUS_OPTIONS;
  readonly originOptions = FILE_RECORD_ORIGIN_OPTIONS;

  readonly pageSize = 20;
  readonly currentPage = signal(0);
  readonly filters: FileRecordEventFilters = {
    filename: '',
    eventType: '',
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
    this.filters.eventType = '';
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
    const page = this.service.events()?.page ?? this.currentPage();
    this.loadPage(page + 1);
  }

  formatDate(value: string): string {
    return new Date(value).toLocaleString('es-MX');
  }

  formatMetadata(json: string | null): string | null {
    if (!json) return null;
    try {
      const obj = JSON.parse(json);
      return Object.entries(obj)
        .map(([k, v]) => `${k}=${v}`)
        .join('  ');
    } catch {
      return json;
    }
  }

  private loadPage(page: number): void {
    this.currentPage.set(page);
    this.service.fetchEvents(page, this.pageSize, {
      filename: this.filters.filename || undefined,
      eventType: this.filters.eventType || undefined,
      status: this.filters.status || undefined,
      origin: this.filters.origin || undefined,
      fileType: this.filters.fileType || undefined
    });
  }
}

