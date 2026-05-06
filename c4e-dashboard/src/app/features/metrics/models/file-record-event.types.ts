export interface FileRecordEventItem {
  id: number;
  sourceId: string | null;
  eventType: string;
  status: string;
  filename: string;
  fileType: string | null;
  origin: string | null;
  failureReason: string | null;
  failureReasonDescription: string | null;
  failedLineNumber: number | null;
  createdBy: string | null;
  occurredAt: string | null;
  receivedAt: string | null;
  metadataJson: string | null;
}

export interface FileRecordEventPageResponse {
  data: FileRecordEventItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface FileRecordEventFilters {
  filename?: string;
  eventType?: string;
  status?: string;
  origin?: string;
  fileType?: string;
  startDate?: string;
  endDate?: string;
}

