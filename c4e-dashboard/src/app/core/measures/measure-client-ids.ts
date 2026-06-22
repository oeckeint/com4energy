/** Normaliza una lista de IDs de cliente: enteros > 0, sin duplicados, ordenados. */
export function normalizeClientIds(clientIds: number[]): number[] {
  return Array.from(new Set(clientIds.filter(id => Number.isInteger(id) && id > 0))).sort((a, b) => a - b);
}

/** Parsea texto "3366, 3367" a IDs de cliente normalizados (enteros > 0, dedup, ordenados). */
export function parseClientIds(raw: string | null | undefined): number[] {
  if (!raw) {
    return [];
  }
  const parsed = raw
    .split(',')
    .map(token => token.trim())
    .filter(token => token.length > 0)
    .map(token => Number(token));
  return normalizeClientIds(parsed);
}
