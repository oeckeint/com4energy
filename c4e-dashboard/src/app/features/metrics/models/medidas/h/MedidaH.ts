export interface MedidaH {
  idMedidaH: number;
  clienteId: number;
  tipoMedida: number;
  fecha: string; // ISO 8601 format: "2025-01-19T04:30:00"
  banderaInvVer: number;

  // Energía activa (en float para MedidaH)
  actent: number;      // Energía activa entrante (consumo)
  qactent: number;     // Calidad de energía activa entrante
  actsal: number;      // Energía activa saliente
  qactsal: number;     // Calidad de energía activa saliente

  // Reactivos por cuadrante
  r_q1: number;
  qr_q1: number;
  r_q2: number;
  qr_q2: number;
  r_q3: number;
  qr_q3: number;
  r_q4: number;
  qr_q4: number;

  // Medidas residuales
  medres1: number;
  qmedres1: number;
  medres2: number;
  qmedres2: number;

  // Metadata
  metodObt: number;
  origen: string;
  createdOn: string;
  createdBy: string;
  updatedOn: string | null;
  updatedBy: string | null;
  temporal: number;
}

