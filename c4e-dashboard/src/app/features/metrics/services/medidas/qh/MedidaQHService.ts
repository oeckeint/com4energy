import { computed, inject, Injectable, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { firstValueFrom } from "rxjs";

import { MedidaQH } from "../../../models/medidas/qh/MedidaQH";
import { MedidaQHConsumptionPoint } from "../../../models/medidas/qh/MedidaQHConsumptionPoint";

interface MedidaQHMatrixRow {
  hour: string;
  values: Record<string, number | null>;
}

interface MedidaQHApiPageResponse {
  content?: MedidaQH[];
  data?: MedidaQH[];
  totalPages?: number;
  last?: boolean;
}

interface MedidaColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

@Injectable({providedIn: 'root'})
export class MedidaQHService {
    private readonly http = inject(HttpClient);
    private readonly API_MEDIDA_QH_PATH = '/api/v1/medidaqh';

    private readonly rowsSignal = signal<MedidaQHMatrixRow[]>([]);
    private readonly clientIdsSignal = signal<number[]>([]);
    private readonly loadingSignal = signal(false);
    private readonly errorSignal = signal<string | null>(null);
    private readonly columnValidationSignal = signal<Record<string, MedidaColumnValidation>>({});

    readonly rows = this.rowsSignal.asReadonly();
    readonly clientIds = this.clientIdsSignal.asReadonly();
    readonly loading = this.loadingSignal.asReadonly();
    readonly error = this.errorSignal.asReadonly();
    readonly columnValidation = this.columnValidationSignal.asReadonly();

    // ========== SIGNALS (Estado) ==========
    private rawMeasurementsSignal = signal<MedidaQH[]>([]);
    private currentPageSignal = signal(0);

    // ========== SEÑALES PÚBLICAS (solo lectura) ==========
    rawMeasurements = this.rawMeasurementsSignal.asReadonly();
    currentPage = this.currentPageSignal.asReadonly();

    /**
     * Datos transformados para gráficos
     */
    consumptionData = computed(() =>
        this.rawMeasurements().map(m => this.transformMedidaToDataPoint(m))
    );

    async fetchMatrix(dayIso: string, clientIds: number[]): Promise<void> {
        this.loadingSignal.set(true);
        this.errorSignal.set(null);
        const normalizedClientIds = this.normalizeClientIds(clientIds);
        this.clientIdsSignal.set(normalizedClientIds);

        try {
            const allItems = await this.fetchData(dayIso);
            const finalClientIds = normalizedClientIds.length > 0
                ? normalizedClientIds
                : this.extractClientIds(allItems);

            this.clientIdsSignal.set(finalClientIds);
            const rows = this.buildRows(finalClientIds, allItems);
            this.rowsSignal.set(rows);
        } catch (err: any) {
            this.rowsSignal.set(this.buildRows(normalizedClientIds, []));
            this.columnValidationSignal.set({});
            this.errorSignal.set(err?.error?.message || err?.message || 'No se pudo cargar Medida QH.');
        } finally {
            this.loadingSignal.set(false);
        }
    }

    private async fetchData(dayIso: string, clientId?: number): Promise<MedidaQH[]> {
        const pageSize = 200;
        let page = 0;
        const acc: MedidaQH[] = [];
        const previousDayIso = this.getPreviousDayIso(dayIso);

        while (true) {
            const params = new URLSearchParams({
                startDate: previousDayIso,
                endDate: dayIso,
                page: String(page),
                size: String(pageSize)
            });

            if (typeof clientId === 'number') {
                params.append('idCliente', String(clientId));
            }

            const url = `${this.API_MEDIDA_QH_PATH}?${params.toString()}`;
            const response = await firstValueFrom(this.http.get<MedidaQHApiPageResponse>(url));
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

    private extractItems(response: MedidaQHApiPageResponse): MedidaQH[] {
        return response.content ?? response.data ?? [];
    }

    private buildRows(clientIds: number[], items: MedidaQH[]): MedidaQHMatrixRow[] {
        const slots = this.getQuarterHourSlots();
        const byClient = new Map<number, Map<string, number>>();

        clientIds.forEach((clientId) => byClient.set(clientId, new Map<string, number>()));

        for (const item of items) {
            const clientId = this.resolveClientId(item);
            if (clientId === null || !byClient.has(clientId)) {
                continue;
            }

            const slot = this.toQuarterHourSlot(item.fecha);
            const value = this.toActEnt(item);
            if (!slot || value === null) {
                continue;
            }

            byClient.get(clientId)?.set(slot, value);
        }

        this.columnValidationSignal.set(this.buildColumnValidation(clientIds, byClient, slots.length));

        return slots.map((slot) => {
            const values: Record<string, number | null> = {};
            clientIds.forEach((clientId) => {
                values[String(clientId)] = byClient.get(clientId)?.get(slot) ?? null;
            });
            return { hour: slot, values };
        });
    }

    private buildColumnValidation(
        clientIds: number[],
        byClient: Map<number, Map<string, number>>,
        expected: number
    ): Record<string, MedidaColumnValidation> {
        const validation: Record<string, MedidaColumnValidation> = {};
        clientIds.forEach((clientId) => {
            const present = byClient.get(clientId)?.size ?? 0;
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

    private normalizeClientIds(clientIds: number[]): number[] {
        return Array.from(new Set(clientIds.filter((id) => Number.isInteger(id) && id > 0))).sort((a, b) => a - b);
    }

    private extractClientIds(items: MedidaQH[]): number[] {
        const ids = items
            .map((item) => this.resolveClientId(item))
            .filter((id): id is number => id !== null);

        return this.normalizeClientIds(ids);
    }

    private resolveClientId(item: MedidaQH): number | null {
        const raw = (item as any).clienteId ?? (item as any).idCliente;
        const parsed = Number(raw);
        return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
    }

    private toActEnt(item: MedidaQH): number | null {
        const raw = item.actent;
        if (raw === null || raw === undefined) {
            return null;
        }
        const parsed = Number(raw);
        return Number.isFinite(parsed) ? parsed : null;
    }

    private toQuarterHourSlot(value: string): string | null {
        if (!value) return null;

        // Expected formats: 2025-03-05T14:15:00 or 2025-03-05T14:15:00Z
        const timePart = value.includes('T') ? value.split('T')[1] : value;
        const match = timePart.match(/^(\d{2}):(\d{2})/);
        if (!match) return null;
        const hh = match[1];
        const mm = match[2];

        // Validar que los minutos sean 00, 15, 30 o 45
        const minutes = parseInt(mm, 10);
        if (![0, 15, 30, 45].includes(minutes)) {
            return null;
        }

        return `${hh}:${mm}`;
    }

    private getQuarterHourSlots(): string[] {
        const slots: string[] = [];
        for (let hour = 0; hour < 24; hour++) {
            for (const minutes of [0, 15, 30, 45]) {
                const hh = String(hour).padStart(2, '0');
                const mm = String(minutes).padStart(2, '0');
                slots.push(`${hh}:${mm}`);
            }
        }
        return slots;
    }

    private getPreviousDayIso(dayIso: string): string {
        const day = new Date(`${dayIso}T00:00:00`);
        day.setDate(day.getDate() - 1);
        return day.toISOString().slice(0, 10);
    }


    /**
     * Transforma una medición raw a formato para gráficos
     */
    private transformMedidaToDataPoint(medida: MedidaQH): MedidaQHConsumptionPoint {
        const fecha = new Date(medida.fecha);

        return {
            hour: this.formatHour(fecha),
            consumption: medida.actent,
            date: fecha,
            quality: medida.qactent
        };
    }

    /**
     * Formatea la hora a "HH:MM"
     */
    private formatHour(date: Date): string {
        return date.toLocaleTimeString('es-ES', {
        hour: '2-digit',
        minute: '2-digit'
        });
    }

}
