import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadService } from '../services/file-upload.service';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
      <div class="card-header bg-white border-bottom py-3">
        <h3 class="text-lg font-semibold mb-0">📤 Cargar Archivos</h3>
        <p class="text-sm text-muted mt-1 mb-0">
          ⚠️ <strong>RESTRICCIÓN TEMPORAL:</strong> Solo se aceptan archivos con extensión <strong>.0</strong> (máximo {{ service.MAX_FILE_SIZE_MB }}MB por archivo y {{ service.MAX_TOTAL_SIZE_MB }}MB en total por carga)
        </p>
      </div>

      <div class="card-body">
        <!-- Drop Zone -->
        <div
          class="drop-zone"
          [class.dragover]="isDragover()"
          (drop)="onDrop($event)"
          (dragover)="onDragOver($event)"
          (dragleave)="onDragLeave()"
        >
          <div class="text-center">
            <div class="fs-1 mb-3">📁</div>
            <p class="mb-2 fw-semibold">Arrastra archivos aquí o haz clic para seleccionar</p>
            <p class="text-muted small mb-3">⚠️ Solo se aceptan archivos con extensión <strong>.0</strong> | Límite: {{ service.MAX_TOTAL_SIZE_MB }}MB total por carga</p>
            <button
              type="button"
              class="btn btn-primary btn-sm"
              (click)="fileInput.click()"
              [disabled]="service.uploading()"
            >
              <i class="bi bi-folder-open me-2"></i>Seleccionar Archivos
            </button>
            <input
              #fileInput
              type="file"
              multiple
              [accept]="acceptedFileTypes"
              hidden
              (change)="onFileSelected($event)"
            />
          </div>
        </div>

        <!-- Filed list -->
        @if (selectedFiles().length > 0) {
          <div class="mt-4">
            <h5 class="mb-3 fw-semibold">Archivos Seleccionados</h5>
            <div class="list-group">
              @for (file of selectedFiles(); track file.name) {
                <div class="list-group-item d-flex justify-content-between align-items-center">
                  <div class="d-flex align-items-center flex-grow-1">
                    <i class="bi bi-file-earmark me-2 text-primary"></i>
                    <div class="flex-grow-1">
                      <div class="text-sm fw-medium">{{ file.name }}</div>
                      <div class="text-xs text-muted">{{ formatFileSize(file.size) }}</div>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="btn btn-sm btn-link text-danger"
                    (click)="removeFile(file)"
                    [disabled]="service.uploading()"
                  >
                    ✕
                  </button>
                </div>
              }
            </div>

            <!-- Action buttons -->
            <div class="mt-4 d-flex gap-2">
              <button
                type="button"
                class="btn btn-primary"
                (click)="uploadFiles()"
                [disabled]="service.uploading() || selectedFiles().length === 0"
              >
                @if (service.uploading()) {
                  <span class="spinner-border spinner-border-sm me-2" role="status"></span>
                  Cargando...
                } @else {
                  <i class="bi bi-upload me-2"></i>Cargar Archivos
                }
              </button>
              <button
                type="button"
                class="btn btn-outline-secondary"
                (click)="clearFiles()"
                [disabled]="service.uploading()"
              >
                Limpiar
              </button>
            </div>

            <!-- Progress bar -->
            @if (service.uploading()) {
              <div class="progress mt-3" style="height: 25px;">
                <div
                  class="progress-bar progress-bar-striped progress-bar-animated"
                  role="progressbar"
                  [style.width.%]="service.progress()"
                  [attr.aria-valuenow]="service.progress()"
                  aria-valuemin="0"
                  aria-valuemax="100"
                >
                  {{ service.progress() }}%
                </div>
              </div>
            }
          </div>
        }

        <!-- Success Alert -->
        @if (service.uploadResult()) {
          <div
            class="alert mt-4"
            [ngClass]="service.uploadResult()!.success ? 'alert-success border-success-subtle bg-success-subtle text-success-emphasis' : 'alert-warning border-warning-subtle bg-warning-subtle text-warning-emphasis'"
          >
            <div class="d-flex align-items-start">
              <div class="fs-4 me-3">
                {{ service.uploadResult()!.success ? '✅' : '⚠️' }}
              </div>
              <div class="flex-grow-1">
                <h5 class="alert-heading mb-2 fw-bold">
                  {{ service.uploadResult()!.success ? 'Carga exitosa' : 'Carga completada con advertencias' }}
                </h5>
                <p class="mb-2">{{ service.uploadResult()!.message }}</p>
                <div class="d-flex flex-wrap gap-2 mb-2">
                  <span class="badge text-bg-success">Exitosos: {{ service.uploadResult()!.successCount }}</span>
                  <span class="badge text-bg-warning">Duplicados: {{ service.uploadResult()!.duplicateCount }}</span>
                  <span class="badge text-bg-secondary">Rechazados: {{ service.uploadResult()!.rejectedCount }}</span>
                  <span class="badge text-bg-danger">Errores: {{ service.uploadResult()!.errorCount }}</span>
                </div>
                <small class="text-muted">{{ service.uploadResult()!.timestamp }}</small>
              </div>
              <button
                type="button"
                class="btn-close"
                (click)="service.clearStatus()"
                aria-label="Cerrar"
              ></button>
            </div>
          </div>
        }

        <!-- Error Alert -->
        @if (service.uploadError()) {
          <div class="alert alert-danger border-danger-subtle bg-danger-subtle text-danger-emphasis mt-4">
            <div class="d-flex align-items-start">
              <div class="fs-4 me-3">❌</div>
              <div class="flex-grow-1">
                <h5 class="alert-heading mb-2 fw-bold">Error en la carga</h5>
                <p class="mb-0">{{ service.uploadError() }}</p>
              </div>
              <button
                type="button"
                class="btn-close"
                (click)="service.clearStatus()"
                aria-label="Cerrar"
              ></button>
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: `
    .drop-zone {
      border: 2px dashed #dee2e6;
      border-radius: 0.5rem;
      padding: 3rem 2rem;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s ease;
      background-color: #f8f9fa;

      &:hover {
        border-color: #0d6efd;
        background-color: #e7f1ff;
      }

      &.dragover {
        border-color: #0d6efd;
        background-color: #cfe2ff;
        box-shadow: 0 0 0 0.25rem rgba(13, 110, 253, 0.25);
      }
    }

    .list-group-item {
      border-color: #dee2e6;
      padding: 0.75rem 1rem;

      &:hover {
        background-color: #f8f9fa;
      }
    }

    .text-xs {
      font-size: 0.75rem;
    }
  `
})
export class FileUploadComponent implements OnInit, OnDestroy {
  service = inject(FileUploadService);

  readonly acceptedFileTypes = this.service.ALLOWED_EXTENSIONS.map(ext => `.${ext}`).join(',');
  readonly allowedExtensionsForDisplay = this.acceptedFileTypes;

  selectedFiles = signal<File[]>([]);
  isDragover = signal(false);

  ngOnInit(): void {
    this.service.clearStatus();
  }

  ngOnDestroy(): void {
    this.clearFiles();
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragover.set(true);
  }

  onDragLeave(): void {
    this.isDragover.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragover.set(false);

    const files = event.dataTransfer?.files;
    if (files) {
      this.addFiles(Array.from(files));
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.addFiles(Array.from(input.files));
    }
  }

  private addFiles(newFiles: File[]): void {
    const current = this.selectedFiles();
    const combined = [...current, ...newFiles];
    // Remove duplicates by name
    const unique = Array.from(new Map(combined.map(f => [f.name, f])).values());
    this.selectedFiles.set(unique);
  }

  removeFile(file: File): void {
    const current = this.selectedFiles();
    this.selectedFiles.set(current.filter(f => f.name !== file.name));
  }

  clearFiles(): void {
    this.selectedFiles.set([]);
    this.service.clearStatus();
  }

  async uploadFiles(): Promise<void> {
    const files = this.selectedFiles();
    if (files.length === 0) return;

    const result = await this.service.uploadFiles(files);
    if (result?.success) {
      this.selectedFiles.set([]);
    }
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}
