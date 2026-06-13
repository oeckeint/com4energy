//Data for graphic representation of QH Consumption

export interface MedidaQHConsumptionPoint {
  hour: string;        // Ej: "04:30"
  consumption: number; // Ej: 23 (del campo actEnt)
  date: Date;          // Convertido a tipo Date
  quality: number;     // Calidad de la medida
}
