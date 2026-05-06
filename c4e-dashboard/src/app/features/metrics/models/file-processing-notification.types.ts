export interface FileProcessingNotification {
  id: number;
  eventType: string;
  status: string;
  filename: string;
  fileType: string | null;
  origin: string | null;
  failureReason: string | null;
  comment: string | null;
  occurredAt: string | null;
  receivedAt: string | null;
}

