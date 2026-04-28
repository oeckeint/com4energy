
export interface MedidaQH {
  idMedidaQH: number;
  idCliente: number;
  tipoMed: number;
  fecha: string; // ISO 8601 format: "2025-01-19T04:30:00"
  banderaInvVer: number;
  
  // Energía activa
  actEnt: number;      // Energía activa entrante (consumo)
  qActEnt: number;     // Calidad de energía activa entrante
  qActSal: number;     // Calidad de energía activa saliente
  
  // Reactivos por cuadrante
  rQ1: number;
  qrQ1: number;
  rQ2: number;
  qrQ2: number;
  rQ3: number;
  qrQ3: number;
  rQ4: number;
  qrQ4: number;
  
  // Medidas residuales
  medRes1: number;
  qMedRes1: number;
  medRes2: number;
  qMedRes2: number;
  
  // Metadata
  metodObt: number;
  origen: string;
  createdOn: string;
  createdBy: string;
  updatedOn: string | null;
  updatedBy: string | null;
  temporal: number;
}
