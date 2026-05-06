/**
 * Tipos para las respuestas del endpoint de carga de archivos
 */
export interface FileBatchItemResponse {
  originalFilename?: string;
  filename: string;
  status: string;
  reason?: string;
  message?: string;
}

export interface FileUploadBatchResponse {
  uploaded: FileBatchItemResponse[];
  duplicated: FileBatchItemResponse[];
  rejected: FileBatchItemResponse[];
  errors: FileBatchItemResponse[];
}

export interface FileUploadGroupedResponse {
  success: FileBatchItemResponse[];
  duplicated: FileBatchItemResponse[];
  invalid: FileBatchItemResponse[];
}

export interface ApiResponse<T> {
  status: string;
  message?: string;
  statusCode?: number;
  statusMessage?: string;
  files?: FileUploadGroupedResponse;
  data?: T;
  errors?: string[];
}

export interface FileUploadResult {
  success: boolean;
  successCount: number;
  duplicateCount: number;
  errorCount: number;
  rejectedCount: number;
  message: string;
  timestamp: string;
}

