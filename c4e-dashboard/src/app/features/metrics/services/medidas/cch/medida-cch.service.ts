import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

interface MedidaCchApiItem {
  fecha?: string;
  acten?: number | string;
  actent?: number | string;
  actEnt?: number | string;
}

interface MedidaCchApiPageResponse {
  content?: MedidaCchApiItem[];
  data?: MedidaCchApiItem[];
  totalPages?: number;
  last?: boolean;
}

interface MedidaCchMatrixRow {
  hour: string;
  values: Record<string, number | null>;
}

interface MedidaColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

@Injectable({ providedIn: 'root' })
export class MedidaCchService {
  private readonly http = inject(HttpClient);
  private readonly API_MEDIDA_CCH_PATH = '/api/v1/medidacch';

  private readonly rowsSignal = signal<MedidaCchMatrixRow[]>([]);
  private readonly clientIdsSignal = signal<number[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly columnValidationSignal = signal<Record<string, MedidaColumnValidation>>({});

  readonly rows = this.rowsSignal.asReadonly();
  readonly clientIds = this.clientIdsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly columnValidation = this.columnValidationSignal.asReadonly();

  async fetchMatrix(dayIso: string, clientIds: number[]): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    this.clientIdsSignal.set(clientIds);

    try {
      const perClient = await Promise.all(clientIds.map((clientId) => this.fetchClientData(clientId, dayIso)));

      const rows = this.buildHourRows(clientIds, perClient);
      this.rowsSignal.set(rows);
    } catch (err: any) {
      this.rowsSignal.set(this.buildHourRows(clientIds, []));
      this.columnValidationSignal.set({});
      this.errorSignal.set(err?.error?.message || err?.message || 'No se pudo cargar Medida CCH.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private async fetchClientData(clientId: number, dayIso: string): Promise<MedidaCchApiItem[]> {
    const pageSize = 200;
    let page = 0;
    const acc: MedidaCchApiItem[] = [];

    while (true) {
      const params = new URLSearchParams({
        idCliente: String(clientId),
        // Keep date-only format so backend builds start/end of day without timezone shifts.
        startDate: dayIso,
        endDate: dayIso,
        page: String(page),
        size: String(pageSize)
      });

      const url = `${this.API_MEDIDA_CCH_PATH}?${params.toString()}`;
      const response = await firstValueFrom(this.http.get<MedidaCchApiPageResponse>(url));
      acc.push(...this.extractItems(response));

      if (typeof response.totalPages === 'number') {
        page += 1;
        if (page >= response.totalPages) break;
        continue;
      }

      if (typeof response.last === 'boolean') {
        if (response.last) break;
        page += 1;
        continue;
      }

      break;
    }

    return acc;
  }

  private extractItems(response: MedidaCchApiPageResponse): MedidaCchApiItem[] {
    return response.content ?? response.data ?? [];
  }

  private buildHourRows(clientIds: number[], clientData: MedidaCchApiItem[][]): MedidaCchMatrixRow[] {
    const hours = this.getHourSlots();

    const mapsByClient = clientData.map((items) => {
      const map = new Map<string, number>();
      for (const item of items) {
        const hour = this.toHour(item.fecha);
        const value = this.toActen(item);
        if (!hour || value === null) {
          continue;
        }
        map.set(hour, value);
      }
      return map;
    });

    this.columnValidationSignal.set(this.buildColumnValidation(clientIds, mapsByClient, hours.length));

    return hours.map((hour) => {
      const values: Record<string, number | null> = {};
      clientIds.forEach((clientId, idx) => {
        values[String(clientId)] = mapsByClient[idx]?.get(hour) ?? null;
      });
      return { hour, values };
    });
  }

  private buildColumnValidation(
    clientIds: number[],
    mapsByClient: Map<string, number>[],
    expected: number
  ): Record<string, MedidaColumnValidation> {
    const validation: Record<string, MedidaColumnValidation> = {};
    clientIds.forEach((clientId, idx) => {
      const present = mapsByClient[idx]?.size ?? 0;
      const missing = Math.max(expected - present, 0);
      validation[String(clientId)] = {
        expected,
        present,
        missing,
        complete: missing === 0
      };
    });
    return validation;
  }

  private toActen(item: MedidaCchApiItem): number | null {
    const raw = item.acten ?? item.actent ?? item.actEnt;
    if (raw === null || raw === undefined || raw === '') {
      return null;
    }
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private toHour(value?: string): string | null {
    if (!value) return null;
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return null;
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    return `${hh}:${mm}`;
  }

  private getHourSlots(): string[] {
    return Array.from({ length: 24 }, (_, hour) => `${String(hour).padStart(2, '0')}:00`);
  }
}


