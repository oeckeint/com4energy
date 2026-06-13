import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiResponse, FileUploadBatchResponse, FileUploadResult } from '../models/file-upload.types';

@Injectable({ providedIn: 'root' })
export class FileUploadService {
  private readonly http = inject(HttpClient);

  // API path for file uploads
  private readonly API_FILES_PATH = '/api/v1/files'; // proxy to ingestion-service:8083

  private readonly uploadingSignal = signal(false);
  private readonly uploadErrorSignal = signal<string | null>(null);
  private readonly uploadResultSignal = signal<FileUploadResult | null>(null);
  private readonly progressSignal = signal(0);

  readonly uploading = this.uploadingSignal.asReadonly();
  readonly uploadError = this.uploadErrorSignal.asReadonly();
  readonly uploadResult = this.uploadResultSignal.asReadonly();
  readonly progress = this.progressSignal.asReadonly();

  /**
   * Allowed file extensions
   * TEMPORARY: Only .0 revisions are accepted while deduplication strategy is being refined.
   */
  readonly ALLOWED_EXTENSIONS = ['0'];
  readonly MAX_FILE_SIZE_MB = 50;
  readonly MAX_TOTAL_SIZE_MB = 500;

  /**
   * Upload files to the ingestion service
   */
  async uploadFiles(files: File[]): Promise<FileUploadResult | null> {
    if (!files.length) {
      this.uploadErrorSignal.set('No files selected');
      return null;
    }

    // Validate files
    const validationError = this.validateFiles(files);
    if (validationError) {
      this.uploadErrorSignal.set(validationError);
      return null;
    }

    this.uploadingSignal.set(true);
    this.uploadErrorSignal.set(null);
    this.progressSignal.set(0);

    try {
      const formData = new FormData();
      files.forEach((file) => {
        formData.append('files', file);
      });

      const response = await firstValueFrom(
        this.http.post<ApiResponse<FileUploadBatchResponse>>(this.API_FILES_PATH, formData)
      );

      const result = this.buildUploadResult(response);
      this.uploadResultSignal.set(result);
      this.progressSignal.set(100);
      return result;
    } catch (error: any) {
      const errorMessage = error?.error?.message || error?.error?.statusMessage || error?.message || 'Error during file upload';
      this.uploadErrorSignal.set(errorMessage);
      return null;
    } finally {
      this.uploadingSignal.set(false);
    }
  }

  /**
   * Validate files before upload
   */
  private validateFiles(files: File[]): string | null {

    const totalSizeBytes = files.reduce((sum, file) => sum + file.size, 0);
    if (totalSizeBytes > this.MAX_TOTAL_SIZE_MB * 1024 * 1024) {
      return `El tamaño total excede el máximo de ${this.MAX_TOTAL_SIZE_MB}MB por carga`;
    }

    for (const file of files) {
      // Check file size
      if (file.size > this.MAX_FILE_SIZE_MB * 1024 * 1024) {
        return `El archivo ${file.name} excede el tamaño máximo de ${this.MAX_FILE_SIZE_MB}MB`;
      }

      // TEMPORARY: Only .0 revisions are accepted for now
      if (!file.name.endsWith('.0')) {
        return `El archivo ${file.name} debe tener la extensión .0. Solo se aceptan archivos con revisión inicial (.0) de forma temporal.`;
      }

      // Check file extension (validate against allowed extensions)
      const extension = file.name.split('.').pop()?.toLowerCase();
      if (!extension || !this.ALLOWED_EXTENSIONS.includes(extension)) {
        const allowedExtensions = this.ALLOWED_EXTENSIONS.map(ext => `.${ext}`).join(', ');
        return `El archivo ${file.name} tiene una extensión no permitida. Solo se aceptan: ${allowedExtensions}`;
      }

      // Check file is not empty
      if (file.size === 0) {
        return `El archivo ${file.name} está vacío`;
      }
    }

    return null;
  }

  /**
   * Build upload result from API response
   */
  private buildUploadResult(
    response: ApiResponse<FileUploadBatchResponse>
  ): FileUploadResult {
    const oldData = response.data || { uploaded: [], duplicated: [], rejected: [], errors: [] };
    const groupedFiles = response.files || { success: [], duplicated: [], invalid: [] };

    const parsedFromMessage = this.extractCountsFromMessage(response.message);

    const successCount = groupedFiles.success?.length || oldData.uploaded?.length || parsedFromMessage.successCount || 0;
    const duplicateCount = groupedFiles.duplicated?.length || oldData.duplicated?.length || parsedFromMessage.duplicateCount || 0;
    const rejectedCount = groupedFiles.invalid?.length || oldData.rejected?.length || 0;
    const errorCount = oldData.errors?.length || parsedFromMessage.errorCount || 0;

    let message = response.message || response.statusMessage || '';
    if (!message) {
      if (successCount > 0) {
        message += `${successCount} archivo(s) cargado(s) correctamente. `;
      }
      if (duplicateCount > 0) {
        message += `${duplicateCount} duplicado(s) omitido(s). `;
      }
      if (rejectedCount > 0) {
        message += `${rejectedCount} rechazado(s). `;
      }
      if (errorCount > 0) {
        message += `${errorCount} error(es). `;
      }
    }

    return {
      success: errorCount === 0 && rejectedCount === 0,
      successCount,
      duplicateCount,
      errorCount,
      rejectedCount,
      message: message.trim() || 'Carga completada',
      timestamp: new Date().toISOString()
    };
  }

  private extractCountsFromMessage(message?: string): {
    successCount: number;
    duplicateCount: number;
    errorCount: number;
  } {
    if (!message) {
      return { successCount: 0, duplicateCount: 0, errorCount: 0 };
    }

    const pattern = /(\d+)\s+archivo\(s\).*?(\d+)\s+duplicado\(s\).*?(\d+)\s+error\(es\)/i;
    const match = pattern.exec(message);
    if (!match) {
      return { successCount: 0, duplicateCount: 0, errorCount: 0 };
    }

    return {
      successCount: Number(match[1]) || 0,
      duplicateCount: Number(match[2]) || 0,
      errorCount: Number(match[3]) || 0
    };
  }

  /**
   * Clear upload result and error
   */
  clearStatus(): void {
    this.uploadResultSignal.set(null);
    this.uploadErrorSignal.set(null);
    this.progressSignal.set(0);
  }
}
