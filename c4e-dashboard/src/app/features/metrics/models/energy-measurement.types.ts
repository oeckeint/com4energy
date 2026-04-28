/**
 * Tipos que reflejan el JSON que viene del backend (nombres coinciden con el JSON)
 */
export interface EnergyMeasurement {
  idMedidaQH: number;
  id_cliente: number;
  tipomed: number;
  fecha: string; // ISO 8601
  bandera_inv_ver: number;

  actent: number;
  qactent: number;
  qactsal: number;

  r_q1: number;
  qr_q1: number;
  r_q2: number;
  qr_q2: number;
  r_q3: number;
  qr_q3: number;
  r_q4: number;
  qr_q4: number;

  medres1: number;
  qmedres1: number;
  medres2: number;
  qmedres2: number;

  metod_obt: number;
  origen: string;
  created_on: string;
  created_by: string;
  updated_on: string | null;
  updated_by: string | null;
  temporal: number;
}

export interface PageSort {
  empty: boolean;
  sorted: boolean;
  unsorted: boolean;
}

export interface PageInfo {
  pageNumber: number;
  pageSize: number;
  sort: PageSort;
  offset: number;
  paged: boolean;
  unpaged: boolean;
}

export interface EnergyMeasurementResponse {
  content: EnergyMeasurement[];
  pageable: PageInfo;
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  sort: PageSort;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

/**
 * Formato simplificado para la UI / gráficos
 */
export interface ConsumptionDataPoint {
  hour: string; // "HH:MM"
  consumption: number; // actent
  date: Date;
  quality: number; // qactent
  origin: string; // origen (filename)
}
