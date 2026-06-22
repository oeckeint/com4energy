/** Tipos compartidos por las vistas de medida (H, QH, …). */

/** Una fila de la matriz: un slot temporal con una columna por cliente. */
export interface MatrixRow {
  hour: string;
  values: Record<string, number | null>;
}

/** Completitud de una columna (cliente) frente a los slots esperados. */
export interface ColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

/** Un archivo de carga que aporta filas a una celda, con su fecha de creación. */
export interface MeasureCellOrigin {
  nombre: string;
  fechaCreacion: string | null;
}

/**
 * Detalle de orígenes de una celda. `minuto` es null para Medida H (granularidad
 * horaria) y se informa para QH (cuarto de hora).
 */
export interface MeasureCellOriginsResponse {
  clienteId: number;
  hora: number;
  minuto: number | null;
  totalRegistros: number;
  origenesDistintos: number;
  origenes: MeasureCellOrigin[];
}
