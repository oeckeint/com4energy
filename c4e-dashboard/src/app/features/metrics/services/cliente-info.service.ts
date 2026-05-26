import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface ClienteInfo {
  idCliente: number;
  cups: string;
  nombreCliente: string;
  tarifa: string;
  isDeleted: number;
}

@Injectable({ providedIn: 'root' })
export class ClienteInfoService {
  private readonly http = inject(HttpClient);
  private readonly API_CLIENTE_PATH = '/api/v1/cliente';

  private readonly clienteInfoSignal = signal<ClienteInfo | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly clienteInfo = this.clienteInfoSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  /**
   * Obtiene la información de un cliente de manera lazy (on-demand).
   * A través del patrón HTTP con el endpoint GET /api/v1/cliente/{idCliente}
   */
  async fetchClienteInfo(idCliente: number): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const response = await firstValueFrom(
        this.http.get<ClienteInfo>(`${this.API_CLIENTE_PATH}/${idCliente}`)
      );
      this.clienteInfoSignal.set(response);
    } catch (err: any) {
      const errorMessage = err?.error?.message || err?.message || `No se pudo cargar la información del cliente ${idCliente}`;
      this.errorSignal.set(errorMessage);
      this.clienteInfoSignal.set(null);
    } finally {
      this.loadingSignal.set(false);
    }
  }

  /**
   * Limpia los datos del cliente en memoria (útil cuando se cierra el card)
   */
  clearClienteInfo(): void {
    this.clienteInfoSignal.set(null);
    this.errorSignal.set(null);
  }
}
