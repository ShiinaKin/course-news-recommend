export type MediaJobType = 'STT' | 'OCR' | string;

export type MediaJobStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED' | string;

export interface MediaJob {
  id: number;
  type: MediaJobType;
  status: MediaJobStatus;
  resultText?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

