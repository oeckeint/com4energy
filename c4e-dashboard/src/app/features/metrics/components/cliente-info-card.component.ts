import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ClienteInfo, ClienteInfoService } from '../services/cliente-info.service';

@Component({
  selector: 'app-cliente-info-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="cliente-info-header">
      <button
        type="button"
        class="cliente-info-toggle"
        (click)="toggleCollapsed()"
        [attr.aria-expanded]="!collapsed"
        [attr.aria-label]="collapsed ? 'Expandir información del cliente' : 'Retraer información del cliente'"
        [attr.title]="collapsed ? 'Expandir información del cliente' : 'Retraer información del cliente'"
      >
        <span class="cliente-info-toggle-icon" aria-hidden="true">{{ collapsed ? '▶' : '▼' }}</span>
        <span class="cliente-info-title">{{ tituloCliente }}</span>
      </button>
    </div>

    <div class="cliente-info-collapsible" [class.cliente-info-collapsible--collapsed]="collapsed" [attr.aria-hidden]="collapsed">
      <div class="cliente-info-collapsible-inner">
        <div class="cliente-info-content">
          @if (clienteInfoService.error()) {
            <div class="cliente-info-error">
              <span class="error-icon">⚠</span>
              <p>{{ clienteInfoService.error() }}</p>
            </div>
          } @else {
            <div class="cliente-info-details" [class.cliente-info-details--loading]="clienteInfoService.loading()" [attr.aria-busy]="clienteInfoService.loading()">
              <div class="cliente-info-row">
                <label class="cliente-info-label">Nombre:</label>
                <span class="cliente-info-value">{{ clienteInfoDisplay.nombreCliente }}</span>
              </div>
              <div class="cliente-info-row">
                <label class="cliente-info-label">CUPS:</label>
                <span class="cliente-info-value">{{ clienteInfoDisplay.cups }}</span>
              </div>
              <div class="cliente-info-row">
                <label class="cliente-info-label">Tarifa:</label>
                <span class="cliente-info-value">{{ clienteInfoDisplay.tarifa }}</span>
              </div>
              <div class="cliente-info-row">
                <label class="cliente-info-label">Status:</label>
                <span class="cliente-info-value">{{ estadoClienteLabel(clienteInfoDisplay.isDeleted) }}</span>
              </div>
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .cliente-info-header {
      background: #f5f5f5;
      padding: 12px 16px;
      border: 1px solid #e0e0e0;
      border-bottom: 0;
      border-radius: 8px 8px 0 0;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .cliente-info-toggle {
      border: 0;
      background: transparent;
      padding: 0;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: inherit;
      cursor: pointer;
    }

    .cliente-info-toggle:hover .cliente-info-title,
    .cliente-info-toggle:focus-visible .cliente-info-title {
      color: #0f172a;
      text-decoration: underline;
      text-underline-offset: 2px;
    }

    .cliente-info-toggle:focus-visible {
      outline: 2px solid #93c5fd;
      outline-offset: 2px;
      border-radius: 4px;
    }

    .cliente-info-toggle-icon {
      font-size: 15px;
      line-height: 1;
      color: #374151;
    }

    .cliente-info-title {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .cliente-info-collapsible {
      display: grid;
      grid-template-rows: 1fr;
      transition: grid-template-rows 0.26s ease;
      border: 1px solid #e0e0e0;
      border-top: 0;
      border-radius: 0 0 8px 8px;
      background: white;
    }

    .cliente-info-collapsible--collapsed {
      grid-template-rows: 0fr;
    }

    .cliente-info-collapsible-inner {
      overflow: hidden;
      min-height: 0;
      display: flex;
      flex-direction: column;
    }

    .cliente-info-content {
      padding: 12px 16px;
      box-sizing: border-box;
    }

    .cliente-info-details {
      display: flex;
      flex-direction: column;
      gap: 12px;
      transition: opacity 0.2s ease;
    }

    .cliente-info-details--loading {
      opacity: 0.72;
    }

    .cliente-info-row {
      display: grid;
      grid-template-columns: 96px 1fr;
      gap: 12px;
      align-items: center;
    }

    .cliente-info-label {
      font-size: 12px;
      font-weight: 600;
      color: #666;
    }

    .cliente-info-value {
      font-size: 13px;
      color: #333;
      word-break: break-word;
      min-height: 18px;
    }

    .cliente-info-error {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #dc2626;
      background: #fef2f2;
      border: 1px solid #fca5a5;
      border-radius: 6px;
      padding: 12px;
      font-size: 12px;
    }

    .error-icon {
      font-size: 16px;
    }

    .cliente-info-empty {
      text-align: center;
      color: #999;
      font-size: 13px;
      padding: 24px;
    }
  `]
})
export class ClienteInfoCardComponent implements OnChanges {
  @Input() clienteId: number | null = null;

  readonly clienteInfoService = inject(ClienteInfoService);
  collapsed = true;
  readonly defaultClienteInfo: ClienteInfo = {
    idCliente: 0,
    cups: '-',
    nombreCliente: '-',
    tarifa: '-',
    isDeleted: -1
  };

  get tituloCliente(): string {
    return this.clienteId && this.clienteId > 0
      ? `Informacion del cliente ${this.clienteId}`
      : 'Informacion del cliente';
  }

  get clienteInfoDisplay(): ClienteInfo {
    return this.clienteInfoService.clienteInfo() ?? this.defaultClienteInfo;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['clienteId'] && this.clienteId && this.clienteId > 0) {
      if (this.collapsed) {
        this.collapsed = false;
      }
      void this.clienteInfoService.fetchClienteInfo(this.clienteId);
    }
  }

  toggleCollapsed(): void {
    this.collapsed = !this.collapsed;
  }

  estadoClienteLabel(isDeleted: number | undefined): string {
    if (isDeleted === 1) return 'Eliminado';
    if (isDeleted === 0) return 'Activo';
    return '-';
  }
}
