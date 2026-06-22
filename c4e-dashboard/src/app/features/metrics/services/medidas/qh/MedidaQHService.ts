import { Injectable } from '@angular/core';

import { MeasureMatrixService } from '../../../../../core/measures/measure-matrix.service';
import { MeasureCellOriginsResponse } from '../../../../../core/measures/measure-matrix.types';

/** Respuesta del endpoint /matrix de QH: una fila por (cliente, hora, minuto) agregada en BD. */
interface MedidaQHQuarterHourPoint {
  clienteId: number;
  hora: number;     // 0-23
  minuto: number;   // 0, 15, 30, 45
  totalActent: number | null;
}

@Injectable({ providedIn: 'root' })
export class MedidaQHService extends MeasureMatrixService<MedidaQHQuarterHourPoint> {
  protected readonly apiBasePath = '/api/v1/medidaqh';
  protected readonly loadErrorMessage = 'No se pudo cargar Medida QH.';

  /** 96 slots cuarto-horarios en orden: 00:00, 00:15, … 23:45. */
  protected slots(): string[] {
    const slots: string[] = [];
    for (let hour = 0; hour < 24; hour++) {
      for (const minute of [0, 15, 30, 45]) {
        slots.push(`${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`);
      }
    }
    return slots;
  }

  /** Índice 0-95: cada hora son 4 slots; el minuto (0/15/30/45) define el offset. */
  protected pointToSlotIndex(point: MedidaQHQuarterHourPoint): number {
    return Number(point.hora) * 4 + Math.floor(Number(point.minuto) / 15);
  }

  protected pointToClientId(point: MedidaQHQuarterHourPoint): number {
    return Number(point.clienteId);
  }

  protected pointToValue(point: MedidaQHQuarterHourPoint): number | null {
    return point.totalActent;
  }

  /** Orígenes de una celda QH (cliente / hora / minuto / día). */
  fetchCellOrigins(
    dayIso: string,
    clientId: number,
    hour: number,
    minute: number
  ): Promise<MeasureCellOriginsResponse> {
    return this.requestCellOrigins(`date=${dayIso}&clientId=${clientId}&hour=${hour}&minute=${minute}`);
  }
}
