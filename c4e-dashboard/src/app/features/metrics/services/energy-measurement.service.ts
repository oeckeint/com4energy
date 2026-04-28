import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  EnergyMeasurement,
  EnergyMeasurementResponse,
  ConsumptionDataPoint
} from '../models/energy-measurement.types';

@Injectable({ providedIn: 'root' })
export class EnergyMeasurementService {
  private http = inject(HttpClient);

  // API path for measurements.
  private readonly API_MEASUREMENTS_PATH = '/api/medidaqh'; // proxy to 8082

  private rawMeasurementsSignal = signal<EnergyMeasurement[]>([]);
  private loadingSignal = signal(false);
  private errorSignal = signal<string | null>(null);
  private currentPageSignal = signal(0);
  private currentClientIdSignal = signal<number | null>(null);

  rawMeasurements = this.rawMeasurementsSignal.asReadonly();
  loading = this.loadingSignal.asReadonly();
  error = this.errorSignal.asReadonly();
  currentPage = this.currentPageSignal.asReadonly();
  currentClientId = this.currentClientIdSignal.asReadonly();

  consumptionData = computed<ConsumptionDataPoint[]>(() =>
    this.rawMeasurementsSignal().map(m => this.transformToDataPoint(m))
  );

  totalDailyConsumption = computed(() =>
    this.consumptionData().reduce((s, p) => s + p.consumption, 0)
  );

  maxConsumption = computed(() => {
    const data = this.consumptionData();
    if (data.length === 0) return 0;
    return Math.max(...data.map(p => p.consumption));
  });

  minConsumption = computed(() => {
    const data = this.consumptionData();
    if (data.length === 0) return 0;
    return Math.min(...data.map(p => p.consumption));
  });

  avgConsumption = computed(() => {
    const data = this.consumptionData();
    if (data.length === 0) return 0;
    const total = data.reduce((s, p) => s + p.consumption, 0);
    return total / data.length;
  });

  /**
   * Fetch a single page of measurements. Optionally pass start/end dates (ISO string or Date)
   * If you want to retrieve a full range that spans multiple pages, use `fetchAllMeasurements`.
   */
  fetchMeasurements(
    page = 0,
    size = 20,
    clientId?: number,
    startDate?: string | Date,
    endDate?: string | Date
  ): void {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    if (typeof clientId === 'number') {
      this.currentClientIdSignal.set(clientId);
    }

    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (clientId) params.append('idCliente', String(clientId));
    if (startDate) params.append('startDate', this.toIso(startDate));
    if (endDate) params.append('endDate', this.toIso(endDate));

    this.http.get<EnergyMeasurementResponse>(`${this.API_MEASUREMENTS_PATH}?${params.toString()}`)
      .subscribe({
        next: (response) => {
          this.rawMeasurementsSignal.set(response.content || []);
          this.currentPageSignal.set(response.number || 0);
          this.loadingSignal.set(false);
        },
        error: (err) => {
          this.errorSignal.set(err?.error?.message || err?.message || 'Error loading measurements');
          this.loadingSignal.set(false);
        }
      });
  }

  /**
   * Fetch all pages for the given query (client and optional date range) and set results.
   * This will iterate pages until the backend indicates the last page (via `totalPages` or `last`).
   */
  async fetchAllMeasurements(
    clientId?: number,
    startDate?: string | Date,
    endDate?: string | Date,
    pageSize = 200
  ): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    if (typeof clientId === 'number') {
      this.currentClientIdSignal.set(clientId);
    }

    try {
      const accumulated: EnergyMeasurement[] = [];
      let page = 0;
      while (true) {
        const params = new URLSearchParams({ page: String(page), size: String(pageSize) });
        if (clientId) params.append('idCliente', String(clientId));
        if (startDate) params.append('startDate', this.toIso(startDate));
        if (endDate) params.append('endDate', this.toIso(endDate));

        const url = `${this.API_MEASUREMENTS_PATH}?${params.toString()}`;
        const resp = await firstValueFrom(this.http.get<EnergyMeasurementResponse>(url));
        accumulated.push(...(resp.content || []));

        // Decide whether to continue based on backend metadata
        if (typeof resp.totalPages === 'number') {
          page++;
          if (page >= resp.totalPages) break;
        } else if (typeof (resp as any).last === 'boolean') {
          if ((resp as any).last) break;
          page++;
        } else if (typeof resp.totalElements === 'number') {
          // Fallback: if we have totalElements and pageSize, compute pages
          const total = resp.totalElements;
          const totalPages = Math.ceil(total / pageSize);
          page++;
          if (page >= totalPages) break;
        } else {
          // No pagination metadata; stop after first page to avoid infinite loops
          break;
        }
      }

      this.rawMeasurementsSignal.set(accumulated);
      this.currentPageSignal.set(0);
      this.loadingSignal.set(false);
    } catch (err: any) {
      this.errorSignal.set(err?.error?.message || err?.message || 'Error loading measurements');
      this.loadingSignal.set(false);
    }
  }

  /** Convenience: fetch the last 24 hours and aggregate across pages. */
  fetchLast24Hours(clientId?: number): Promise<void> {
    const end = new Date();
    const start = new Date(Date.now() - 24 * 60 * 60 * 1000);
    return this.fetchAllMeasurements(clientId, start.toISOString(), end.toISOString());
  }

  private transformToDataPoint(m: EnergyMeasurement): ConsumptionDataPoint {
    const date = new Date(m.fecha);
    return {
      hour: this.formatHour(date),
      consumption: m.actent,
      date,
      quality: m.qactent,
      origin: m.origen
    };
  }

  private formatHour(d: Date): string {
    return d.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
  }

  private toIso(d: string | Date): string {
    return typeof d === 'string' ? d : d.toISOString();
  }
}
