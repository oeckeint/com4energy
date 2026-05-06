// Data for graphic representation of H Consumption

export interface MedidaHConsumptionPoint {
  hour: string;        // Ej: "04:30"
  consumption: number; // Ej: 23.5 (del campo actent)
  date: Date;          // Convertido a tipo Date
  quality: number;     // Calidad de la medida
}

