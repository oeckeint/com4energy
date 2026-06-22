import { computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

import { ColumnValidation, MatrixRow, MeasureCellOriginsResponse } from './measure-matrix.types';
import { normalizeClientIds } from './measure-client-ids';

/**
 * Base compartida para las vistas de medida (H, QH, …). Centraliza el estado (signals),
 * la carga de `/matrix` (una sola petición ya agregada en BD), tarifas y cell-origins.
 *
 * Cada subtipo aporta solo lo que difiere: el path del API, las etiquetas de slot
 * (24 para H, 96 para QH) y cómo mapear un punto del backend a su slot / cliente / valor.
 */
export abstract class MeasureMatrixService<TPoint> {
  protected readonly http = inject(HttpClient);

  /** Base del API, p. ej. '/api/v1/medidah'. */
  protected abstract readonly apiBasePath: string;
  /** Mensaje de error de carga específico del tipo. */
  protected abstract readonly loadErrorMessage: string;
  /** Etiquetas de slot en orden (24 para H, 96 para QH). */
  protected abstract slots(): string[];
  /** Índice del slot (posición en `slots()`) que corresponde a un punto. */
  protected abstract pointToSlotIndex(point: TPoint): number;
  protected abstract pointToClientId(point: TPoint): number;
  protected abstract pointToValue(point: TPoint): number | null;

  private readonly rowsSignal = signal<MatrixRow[]>([]);
  private readonly clientIdsSignal = signal<number[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly columnValidationSignal = signal<Record<string, ColumnValidation>>({});
  private readonly totalRecordsSignal = signal(0);
  private readonly uniqueClientsSignal = signal(0);
  private readonly tarifasSignal = signal<string[]>([]);

  readonly rows = this.rowsSignal.asReadonly();
  readonly clientIds = this.clientIdsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly columnValidation = this.columnValidationSignal.asReadonly();
  readonly totalRecords = this.totalRecordsSignal.asReadonly();
  readonly uniqueClients = this.uniqueClientsSignal.asReadonly();
  readonly tarifas = this.tarifasSignal.asReadonly();

  /** Suma de registros presentes en todos los clientes visibles. */
  readonly totalPresent = computed(() =>
    Object.values(this.columnValidationSignal()).reduce((sum, v) => sum + v.present, 0)
  );

  /** Suma de registros esperados en todos los clientes visibles (slots × nClientes). */
  readonly totalExpected = computed(() =>
    Object.values(this.columnValidationSignal()).reduce((sum, v) => sum + v.expected, 0)
  );

  /**
   * Carga la matriz para el día dado vía `/matrix` (GROUP BY en BD → una sola petición).
   * Si se pasan clientIds, filtra esas columnas; si se pasa tarifa, filtra en backend;
   * sin filtros, muestra todos los clientes del día.
   */
  async fetchMatrix(dayIso: string, clientIds: number[], tarifa?: string): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    const normalizedClientIds = normalizeClientIds(clientIds);

    try {
      let url = `${this.apiBasePath}/matrix?date=${dayIso}`;
      if (tarifa?.trim()) {
        url += `&tarifa=${encodeURIComponent(tarifa.trim())}`;
      }
      for (const clientId of normalizedClientIds) {
        url += `&clientIds=${clientId}`;
      }

      const points = await firstValueFrom(this.http.get<TPoint[]>(url));

      const finalClientIds = normalizedClientIds.length > 0
        ? normalizedClientIds
        : this.extractClientIds(points);

      this.totalRecordsSignal.set(points.length);
      this.uniqueClientsSignal.set(finalClientIds.length);
      this.clientIdsSignal.set(finalClientIds);

      const rows = this.buildRows(finalClientIds, points);
      this.rowsSignal.set(rows);
      this.columnValidationSignal.set(this.buildColumnValidation(finalClientIds, rows));
    } catch (err: any) {
      this.rowsSignal.set([]);
      this.columnValidationSignal.set({});
      this.clientIdsSignal.set([]);
      this.totalRecordsSignal.set(0);
      this.uniqueClientsSignal.set(0);
      this.errorSignal.set(err?.error?.message || err?.message || this.loadErrorMessage);
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async fetchTarifas(): Promise<void> {
    try {
      const result = await firstValueFrom(this.http.get<string[]>(`/api/v1/tarifas`));
      const normalized = Array.from(
        new Set((result ?? []).map(item => item?.trim()).filter((item): item is string => !!item))
      ).sort((a, b) => a.localeCompare(b, 'es'));
      this.tarifasSignal.set(normalized);
    } catch {
      this.tarifasSignal.set([]);
    }
  }

  /** Requester común de cell-origins; cada subtipo arma su querystring (H: hora; QH: hora+minuto). */
  protected requestCellOrigins(query: string): Promise<MeasureCellOriginsResponse> {
    return firstValueFrom(
      this.http.get<MeasureCellOriginsResponse>(`${this.apiBasePath}/cell-origins?${query}`)
    );
  }

  private extractClientIds(points: TPoint[]): number[] {
    const ids = new Set(points.map(p => Number(this.pointToClientId(p))).filter(id => id > 0));
    return Array.from(ids).sort((a, b) => a - b);
  }

  private buildRows(clientIds: number[], points: TPoint[]): MatrixRow[] {
    const slots = this.slots();

    // Índice: clienteId → slotIndex → valor
    const index = new Map<number, Map<number, number>>();
    for (const point of points) {
      const clientId = Number(this.pointToClientId(point));
      const raw = this.pointToValue(point);
      const val = raw !== null && raw !== undefined ? Number(raw) : null;
      if (val === null || !Number.isFinite(val)) {
        continue;
      }
      if (!index.has(clientId)) {
        index.set(clientId, new Map());
      }
      index.get(clientId)!.set(this.pointToSlotIndex(point), val);
    }

    return slots.map((label, slotIdx) => {
      const values: Record<string, number | null> = {};
      clientIds.forEach(clientId => {
        values[String(clientId)] = index.get(clientId)?.get(slotIdx) ?? null;
      });
      return { hour: label, values };
    });
  }

  private buildColumnValidation(clientIds: number[], rows: MatrixRow[]): Record<string, ColumnValidation> {
    const validation: Record<string, ColumnValidation> = {};
    const expected = rows.length;
    clientIds.forEach(clientId => {
      const present = rows.filter(r => r.values[String(clientId)] !== null).length;
      const missing = Math.max(expected - present, 0);
      validation[String(clientId)] = { expected, present, missing, complete: missing === 0 };
    });
    return validation;
  }
}
