
import { computed, inject, Injectable, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";

import { MedidaQH } from "../../../models/medidas/qh/MedidaQH";
import { MedidaQHConsumptionPoint } from "../../../models/medidas/qh/MedidaQHConsumptionPoint";

@Injectable({providedIn: 'root'})
export class MedidaQHService {
    private http = inject(HttpClient);

    // ========== SIGNALS (Estado) ==========
    private rawMeasurementsSignal = signal<MedidaQH[]>([]);
    private loadingSignal = signal(false);
    private errorSignal = signal<string | null>(null);
    private currentPageSignal = signal(0);

    // ========== SEÑALES PÚBLICAS (solo lectura) ==========
    rawMeasurements = this.rawMeasurementsSignal.asReadonly();
    loading = this.loadingSignal.asReadonly();
    error = this.errorSignal.asReadonly();
    currentPage = this.currentPageSignal.asReadonly();

    /**
     * Datos transformados para gráficos
     * Este computed() automáticamente se actualiza cuando rawMeasurements cambia
     */
    consumptionData = computed(() =>
        this.rawMeasurements().map(m => this.transformMedidaToDataPoint(m))
    );

    /**
     * Transforma una medición raw a formato para gráficos
     * Aquí convertimos los nombres del backend a nombres legibles
     */
    private transformMedidaToDataPoint(medida: MedidaQH): MedidaQHConsumptionPoint {
        const fecha = new Date(medida.fecha);

        return {
            hour: this.formatHour(fecha),
            consumption: medida.actEnt,
            date: fecha,
            quality: medida.qActEnt
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
