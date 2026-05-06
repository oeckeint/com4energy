import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MedidaHService } from '../services/medidas/h/MedidaHService';
import { MeasureMatrixTableComponent } from '../components/measure-matrix-table.component';
import { MeasureFiltersComponent } from '../components/measure-filters.component';
import { MeasureHourlyMiniChartComponent } from '../components/measure-hourly-mini-chart.component';
import { MeasureCompletenessGaugeComponent } from '../components/measure-completeness-gauge.component';
import { MeasureAuditorPanelComponent } from '../components/measure-auditor-panel.component';

@Component({
  selector: 'app-medida-h-page',
  standalone: true,
  imports: [CommonModule, MeasureFiltersComponent, MeasureCompletenessGaugeComponent, MeasureHourlyMiniChartComponent, MeasureAuditorPanelComponent, MeasureMatrixTableComponent],
  templateUrl: './medida-h.page.html',
  styles: [`
    .measure-h-top-layout {
      grid-template-columns: 1fr 1.5fr;
    }

    .measure-h-tools-rail {
      position: fixed;
      right: 0;
      top: 0;
      bottom: 0;
      z-index: 1402;
      width: 44px;
      border-left: 1px solid #e2e8f0;
      border-top: 1px solid #e2e8f0;
      border-bottom: 1px solid #e2e8f0;
      border-radius: 0;
      background: #f8fafc;
      padding: 72px 5px 12px;
      display: flex;
      flex-direction: column;
      gap: 6px;
      align-items: center;
    }

    .measure-h-tools-button {
      width: 32px;
      height: 32px;
      border: none;
      border-radius: 6px;
      background: transparent;
      color: #6b7280;
      font-size: 16px;
      line-height: 1;
      display: grid;
      place-items: center;
      transition: background-color 0.2s ease, color 0.2s ease;
    }

    .measure-h-tools-button:hover {
      background: #e2e8f0;
      color: #334155;
    }

    .measure-h-tools-button--active {
      background: #dbeafe;
      color: #2563eb;
    }

    .measure-h-tools-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.26);
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.2s ease;
      z-index: 1398;
    }

    .measure-h-tools-backdrop--open {
      opacity: 1;
      pointer-events: auto;
    }

    .measure-h-tools-drawer {
      position: fixed;
      top: 0;
      right: 44px;
      width: min(440px, 88vw);
      height: 100vh;
      background: #ffffff;
      border-left: 1px solid #e5e7eb;
      box-shadow: -14px 0 28px rgba(15, 23, 42, 0.2);
      transform: translateX(calc(100% + 44px));
      transition: transform 0.24s ease;
      z-index: 1401;
      display: flex;
      flex-direction: column;
    }

    .measure-h-tools-drawer--open {
      transform: translateX(0);
    }

    .measure-h-tools-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 10px 12px;
      border-bottom: 1px solid #e5e7eb;
      background: #f8fafc;
    }

    .measure-h-tools-close {
      border: 1px solid #d1d5db;
      border-radius: 6px;
      background: #ffffff;
      color: #374151;
      width: 28px;
      height: 28px;
      line-height: 1;
    }

    .measure-h-tools-body {
      flex: 1;
      min-height: 0;
      padding: 10px;
    }

    .measure-h-tools-body app-measure-auditor-panel {
      display: block;
      height: 100%;
    }

    .measure-h-tools-placeholder {
      border: 1px dashed #cbd5e1;
      border-radius: 8px;
      background: #f8fafc;
      color: #64748b;
      padding: 14px;
      font-size: 13px;
    }

    @media (max-width: 768px) {
      .measure-h-top-layout {
        grid-template-columns: 1fr;
      }

      .measure-h-tools-rail {
        top: 0;
        bottom: 0;
        padding-top: 72px;
      }
    }
  `]
})
export class MedidaHPage implements OnInit {
  readonly service = inject(MedidaHService);
  activeTool: 'auditor' | 'utilidades' | null = null;

  get toolsPanelOpen(): boolean {
    return this.activeTool !== null;
  }

  get activeToolTitle(): string {
    if (this.activeTool === 'auditor') return 'Auditor';
    if (this.activeTool === 'utilidades') return 'Utilidades';
    return 'Herramientas';
  }

  readonly filters = {
    day: this.todayIso(),
    clientIdsText: ''
  };

  ngOnInit(): void {
    this.applyFilters();
  }

  async onDayChange(newDay: string): Promise<void> {
    this.filters.day = newDay;
    await this.applyFilters();
  }

  async applyFilters(): Promise<void> {
    const clientIds = this.parseClientIds(this.filters.clientIdsText);
    await this.service.fetchMatrix(this.filters.day || this.todayIso(), clientIds);
  }

  async clearFilters(): Promise<void> {
    this.filters.day = this.todayIso();
    this.filters.clientIdsText = '';
    await this.service.fetchMatrix(this.filters.day, []);
  }

  toggleTool(tool: 'auditor' | 'utilidades'): void {
    this.activeTool = this.activeTool === tool ? null : tool;
  }

  closeToolsPanel(): void {
    this.activeTool = null;
  }


  private parseClientIds(raw: string): number[] {
    if (!raw) return [];

    const parsed = raw
      .split(',')
      .map((token) => token.trim())
      .filter((token) => token.length > 0)
      .map(Number)
      .filter((id) => Number.isInteger(id) && id > 0);

    return Array.from(new Set(parsed));
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }
}

