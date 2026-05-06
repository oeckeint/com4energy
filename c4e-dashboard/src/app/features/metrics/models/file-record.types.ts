export interface FileRecordItem {
  id: number;
  originalFilename: string | null;
  finalFilename: string;
  type: string | null;
  status: string | null;
  qualityStatus: string | null;
  origin: string | null;
  retryCount: number;
  processedRecords: number | null;
  defectedRecords: number | null;
  processingDurationMs: number | null;
  uploadedAt: string | null;
  processedAt: string | null;
  createdAt: string | null;
}

export interface FileRecordPageResponse {
  data: FileRecordItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface FileRecordFilters {
  filename?: string;
  status?: string;
  origin?: string;
  fileType?: string;
  startDate?: string;
  endDate?: string;
}

