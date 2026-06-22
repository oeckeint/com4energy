import { Injectable } from '@angular/core';

import { MeasureMatrixService } from '../../../../../core/measures/measure-matrix.service';
import { MeasureCellOriginsResponse } from '../../../../../core/measures/measure-matrix.types';

/** Respuesta del endpoint /matrix de H: una fila por (cliente, hora) ya agregada en BD. */
interface MedidaHHourlyPoint {
  clienteId: number;
  hora: number;          // 0-23
  totalActent: number | null;
}

@Injectable({ providedIn: 'root' })
export class MedidaHService extends MeasureMatrixService<MedidaHHourlyPoint> {
  protected readonly apiBasePath = '/api/v1/medidah';
  protected readonly loadErrorMessage = 'No se pudo cargar Medida H.';

  /** 24 slots horarios: 00:00 … 23:00. El índice de slot es la propia hora. */
  protected slots(): string[] {
    return Array.from({ length: 24 }, (_, hour) => `${String(hour).padStart(2, '0')}:00`);
  }

  protected pointToSlotIndex(point: MedidaHHourlyPoint): number {
    return Number(point.hora);
  }

  protected pointToClientId(point: MedidaHHourlyPoint): number {
    return Number(point.clienteId);
  }

  protected pointToValue(point: MedidaHHourlyPoint): number | null {
    return point.totalActent;
  }

  /** Orígenes de una celda H (cliente / hora / día). */
  fetchCellOrigins(dayIso: string, clientId: number, hour: number): Promise<MeasureCellOriginsResponse> {
    return this.requestCellOrigins(`date=${dayIso}&clientId=${clientId}&hour=${hour}`);
  }
}
