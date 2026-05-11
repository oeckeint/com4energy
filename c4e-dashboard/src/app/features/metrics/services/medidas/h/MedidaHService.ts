import { computed, inject, Injectable, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { firstValueFrom } from "rxjs";

import { MedidaH } from "../../../models/medidas/h/MedidaH";
import { MedidaHConsumptionPoint } from "../../../models/medidas/h/MedidaHConsumptionPoint";

/** Respuesta del endpoint /matrix: una fila por (cliente, hora) ya agregada en BD */
interface MedidaHHourlyPoint {
  clienteId: number;
  hora: number;          // 0-23
  totalActent: number | null;
}

interface MedidaHMatrixRow {
  hour: string;
  values: Record<string, number | null>;
}

export interface MedidaHCellOriginsResponse {
  clienteId: number;
  hora: number;
  totalRegistros: number;
  origenesDistintos: number;
  origenes: string[];
}

interface MedidaColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

@Injectable({providedIn: 'root'})
export class MedidaHService {
    private readonly http = inject(HttpClient);
    private readonly API_MEDIDA_H_PATH = '/api/v1/medidah';

    private readonly rowsSignal = signal<MedidaHMatrixRow[]>([]);
    private readonly clientIdsSignal = signal<number[]>([]);
    private readonly loadingSignal = signal(false);
    private readonly errorSignal = signal<string | null>(null);
    private readonly columnValidationSignal = signal<Record<string, MedidaColumnValidation>>({});
    private readonly totalRecordsSignal = signal(0);
    private readonly uniqueClientsSignal = signal(0);

    readonly rows = this.rowsSignal.asReadonly();
    readonly clientIds = this.clientIdsSignal.asReadonly();
    readonly loading = this.loadingSignal.asReadonly();
    readonly error = this.errorSignal.asReadonly();
    readonly columnValidation = this.columnValidationSignal.asReadonly();
    readonly totalRecords = this.totalRecordsSignal.asReadonly();
    readonly uniqueClients = this.uniqueClientsSignal.asReadonly();

    /** Suma de registros presentes en todos los clientes visibles */
    readonly totalPresent = computed(() =>
        Object.values(this.columnValidationSignal()).reduce((sum, v) => sum + v.present, 0)
    );

    /** Suma de registros esperados en todos los clientes visibles (24 × nClientes para H) */
    readonly totalExpected = computed(() =>
        Object.values(this.columnValidationSignal()).reduce((sum, v) => sum + v.expected, 0)
    );

    private rawMeasurementsSignal = signal<MedidaH[]>([]);
    private currentPageSignal = signal(0);

    rawMeasurements = this.rawMeasurementsSignal.asReadonly();
    currentPage = this.currentPageSignal.asReadonly();

    consumptionData = computed(() =>
        this.rawMeasurements().map(m => this.transformMedidaToDataPoint(m))
    );

    /**
     * Carga la matriz de medidas H para el día dado usando el endpoint /matrix
     * que hace GROUP BY en base de datos → una sola petición, sin paginación.
     *
     * Si se pasan clientIds, filtra para mostrar solo esas columnas.
     * Si no, muestra todos los clientes del día.
     */
    async fetchMatrix(dayIso: string, clientIds: number[]): Promise<void> {
        this.loadingSignal.set(true);
        this.errorSignal.set(null);

        const normalizedClientIds = this.normalizeClientIds(clientIds);

        try {
            const points = await firstValueFrom(
                this.http.get<MedidaHHourlyPoint[]>(`${this.API_MEDIDA_H_PATH}/matrix?date=${dayIso}`)
            );

            const allClientIds = this.extractClientIdsFromPoints(points);

            // Si hay filtro de clientes, usar solo esos; si no, mostrar todos
            const finalClientIds = normalizedClientIds.length > 0 ? normalizedClientIds : allClientIds;

            // Filtrar los puntos al conjunto visible para que los contadores coincidan con la vista
            const visiblePoints = normalizedClientIds.length > 0
                ? points.filter(p => normalizedClientIds.includes(Number(p.clienteId)))
                : points;

            this.totalRecordsSignal.set(visiblePoints.length);
            this.uniqueClientsSignal.set(finalClientIds.length);

            this.clientIdsSignal.set(finalClientIds);

            const rows = this.buildRowsFromPoints(finalClientIds, points);
            this.rowsSignal.set(rows);
            this.columnValidationSignal.set(this.buildColumnValidation(finalClientIds, rows));
        } catch (err: any) {
            this.rowsSignal.set([]);
            this.columnValidationSignal.set({});
            this.clientIdsSignal.set([]);
            this.totalRecordsSignal.set(0);
            this.uniqueClientsSignal.set(0);
            this.errorSignal.set(err?.error?.message || err?.message || 'No se pudo cargar Medida H.');
        } finally {
            this.loadingSignal.set(false);
        }
    }

    async fetchCellOrigins(dayIso: string, clientId: number, hour: number): Promise<MedidaHCellOriginsResponse> {
        return await firstValueFrom(
            this.http.get<MedidaHCellOriginsResponse>(
                `${this.API_MEDIDA_H_PATH}/cell-origins?date=${dayIso}&clientId=${clientId}&hour=${hour}`
            )
        );
    }

    private extractClientIdsFromPoints(points: MedidaHHourlyPoint[]): number[] {
        const ids = new Set(points.map(p => Number(p.clienteId)).filter(id => id > 0));
        return Array.from(ids).sort((a, b) => a - b);
    }

    /**
     * Construye las 24 filas (una por hora) con una columna por cliente.
     * El valor de cada celda es totalActent del punto correspondiente.
     */
    private buildRowsFromPoints(clientIds: number[], points: MedidaHHourlyPoint[]): MedidaHMatrixRow[] {
        const hours = this.getHourSlots();

        // Índice: clienteId → hora (0-23) → valor
        const index = new Map<number, Map<number, number>>();
        for (const p of points) {
            const clientId = Number(p.clienteId);
            const val = p.totalActent !== null && p.totalActent !== undefined ? Number(p.totalActent) : null;
            if (!Number.isFinite(val) || val === null) continue;
            if (!index.has(clientId)) index.set(clientId, new Map());
            index.get(clientId)!.set(Number(p.hora), val as number);
        }

        return hours.map((hourLabel, hourIdx) => {
            const values: Record<string, number | null> = {};
            clientIds.forEach(clientId => {
                values[String(clientId)] = index.get(clientId)?.get(hourIdx) ?? null;
            });
            return { hour: hourLabel, values };
        });
    }

    private buildColumnValidation(
        clientIds: number[],
        rows: MedidaHMatrixRow[]
    ): Record<string, MedidaColumnValidation> {
        const validation: Record<string, MedidaColumnValidation> = {};
        const expected = rows.length;
        clientIds.forEach(clientId => {
            const present = rows.filter(r => r.values[String(clientId)] !== null).length;
            const missing = Math.max(expected - present, 0);
            validation[String(clientId)] = { expected, present, missing, complete: missing === 0 };
        });
        return validation;
    }

    private normalizeClientIds(clientIds: number[]): number[] {
        return Array.from(new Set(clientIds.filter(id => Number.isInteger(id) && id > 0))).sort((a, b) => a - b);
    }

    private getHourSlots(): string[] {
        return Array.from({ length: 24 }, (_, hour) => `${String(hour).padStart(2, '0')}:00`);
    }

    private transformMedidaToDataPoint(medida: MedidaH): MedidaHConsumptionPoint {
        const fecha = new Date(medida.fecha);
        return {
            hour: this.formatHour(fecha),
            consumption: medida.actent,
            date: fecha,
            quality: medida.qactent
        };
    }

    private formatHour(date: Date): string {
        return date.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' });
    }
}
