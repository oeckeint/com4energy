/**
 * Formato europeo de energía: agrupa miles con '.' y redondea al entero más cercano.
 *
 * Las magnitudes llegan del backend ya como enteros (columnas SMALLINT, redondeo
 * HALF_EVEN en la ingesta), así que aquí Math.round es no-op para ellas y solo actúa
 * sobre agregados calculados en el front (p. ej. el promedio).
 *
 * Sustituye al formateador que estaba duplicado en varios componentes y que usaba
 * Math.ceil: redondeaba enteros innecesariamente y, donde sí había decimal (el
 * promedio), lo hacía hacia arriba, de forma inconsistente con el HALF_EVEN del backend.
 */
export const formatEnergyValue = (value: number | null | undefined): string => {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '-';
  }
  return Math.round(value).toLocaleString('es-ES');
};
