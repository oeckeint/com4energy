import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MedidaQHService } from '../services/medidas/qh/MedidaQHService';
import { MeasureMatrixTableComponent } from '../components/measure-matrix-table.component';
import { MeasureFiltersComponent } from '../components/measure-filters.component';
import { MeasureHourlyMiniChartComponent } from '../components/measure-hourly-mini-chart.component';
import { MeasureCompletenessGaugeComponent } from '../components/measure-completeness-gauge.component';
import { MeasureAuditorPanelComponent } from '../components/measure-auditor-panel.component';

@Component({
  selector: 'app-medida-qh-page',
  standalone: true,
  imports: [CommonModule, MeasureFiltersComponent, MeasureCompletenessGaugeComponent, MeasureHourlyMiniChartComponent, MeasureAuditorPanelComponent, MeasureMatrixTableComponent],
  templateUrl: './medida-qh.page.html'
})
export class MedidaQHPage implements OnInit {
  readonly service = inject(MedidaQHService);
  auditorFocusTrigger = 0;

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
    this.filters.clientIdsText = '';
    const selectedDay = this.filters.day || this.todayIso();
    this.filters.day = selectedDay;
    await this.service.fetchMatrix(selectedDay, []);
  }

  openAuditorDefectuosos(): void {
    this.auditorFocusTrigger += 1;
  }

  private parseClientIds(raw: string): number[] {
    if (!raw) return [];

    const parsed = raw
      .split(',')
      .map((token) => token.trim())
      .filter((token) => token.length > 0)
      .map((token) => Number(token))
      .filter((id) => Number.isInteger(id) && id > 0);

    return Array.from(new Set(parsed));
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
