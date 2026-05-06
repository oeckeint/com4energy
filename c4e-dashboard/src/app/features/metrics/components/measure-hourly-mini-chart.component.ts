import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges } from '@angular/core';

interface MeasureMatrixRow {
  hour: string;
  values: Record<string, number | null>;
}

interface MiniChartPoint {
  x: number;
  y: number;
  value: number;
  label: string;
}

interface MiniChartTick {
  x: number;
  label: string;
}

interface MiniChartYTick {
  y: number;
  value: number;
}

interface MeasureColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

@Component({
  selector: 'app-measure-hourly-mini-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (rows.length > 0 && miniChartPoints.length > 0) {
      <div class="measure-mini-chart-card">
        <div class="measure-mini-chart-title">
          Totales por hora <span class="status-emoji" [attr.title]="statusTooltip">{{ statusEmoji }}</span>
        </div>
        <svg viewBox="0 0 300 150" class="measure-mini-chart" role="img" aria-label="Totales por hora">
          <line x1="28" y1="118" x2="292" y2="118" class="measure-mini-axis" />
          <line x1="28" y1="12" x2="28" y2="118" class="measure-mini-axis" />

          @for (tick of miniChartYTicks; track tick.value) {
            <line x1="24" [attr.y1]="tick.y" x2="292" [attr.y2]="tick.y" class="measure-mini-grid" />
            <text x="22" [attr.y]="tick.y + 3" text-anchor="end" class="measure-mini-tick-label">{{ formatAxisValue(tick.value) }}</text>
          }

          @if (miniChartPath) {
            <path [attr.d]="miniChartPath" class="measure-mini-line" />
          }

          @for (point of miniChartPoints; track point.label) {
            <circle
              [attr.cx]="point.x"
              [attr.cy]="point.y"
              r="2.1"
              class="measure-mini-dot"
              [attr.title]="point.label + ': ' + formatAxisValue(point.value)"
            />
          }

          @for (tick of miniChartXTicks; track tick.label) {
            <text [attr.x]="tick.x" y="133" text-anchor="middle" class="measure-mini-tick-label">{{ tick.label }}</text>
          }
        </svg>
        @if (weekdayLabel) {
          <div class="measure-mini-weekday">{{ weekdayLabel }}</div>
        }
      </div>
    }
  `,
  styles: [`
    .status-emoji {
      font-size: 1.2em;
      margin-left: 8px;
      cursor: help;
    }

    .measure-mini-weekday {
      margin-top: 6px;
      font-size: 0.8rem;
      color: #6b7280;
      text-align: center;
      text-transform: capitalize;
    }
  `]
})
export class MeasureHourlyMiniChartComponent implements OnChanges {
  private readonly miniChartLeft = 28;
  private readonly miniChartTop = 12;
  private readonly miniChartWidth = 264;
  private readonly miniChartHeight = 106;

  @Input() rows: MeasureMatrixRow[] = [];
  @Input() chartYMax = 0;
  @Input() columnValidation: Record<string, MeasureColumnValidation> = {};
  @Input() clientIds: number[] = [];
  @Input() currentDate: string | Date | null = null;

  miniChartPoints: MiniChartPoint[] = [];
  miniChartXTicks: MiniChartTick[] = [];
  miniChartYTicks: MiniChartYTick[] = [];
  miniChartPath = '';
  statusEmoji = '';
  statusTooltip = '';

  get weekdayLabel(): string {
    if (!this.currentDate) {
      return '';
    }

    const date = this.parseDate(this.currentDate);
    if (!date) {
      return '';
    }

    return date.toLocaleDateString('es-ES', { weekday: 'long' });
  }

  ngOnChanges(): void {
    this.rebuildMiniChart();
    this.updateStatusIndicator();
  }

  private updateStatusIndicator(): void {
    const successRate = this.calculateSuccessRate();
    const hasNoData = this.clientIds.length === 0;

    if (hasNoData) {
      this.statusEmoji = '🤔';
      this.statusTooltip = 'Sin datos - Esperando información';
    } else if (successRate >= 1.0) {
      this.statusEmoji = '😄';
      this.statusTooltip = '100% éxito - ¡Perfecto!';
    } else if (successRate >= 0.7) {
      this.statusEmoji = '😊';
      this.statusTooltip = `${Math.round(successRate * 100)}% éxito - Muy bien`;
    } else if (successRate >= 0.4) {
      this.statusEmoji = '😐';
      this.statusTooltip = `${Math.round(successRate * 100)}% éxito - Revisar`;
    } else {
      this.statusEmoji = '😞';
      this.statusTooltip = `${Math.round(successRate * 100)}% éxito - Atención requerida`;
    }
  }

  private calculateSuccessRate(): number {
    if (this.clientIds.length === 0) {
      return 0; // Sin datos, se considera como fallo
    }

    let totalExpected = 0;
    let totalMissing = 0;

    for (const clientId of this.clientIds) {
      const validation = this.columnValidation[String(clientId)];
      if (validation) {
        totalExpected += validation.expected;
        totalMissing += validation.missing;
      }
    }

    if (totalExpected === 0) {
      return 0; // Sin datos esperados, se considera como fallo
    }

    return Math.max(0, 1 - totalMissing / totalExpected);
  }

  private rebuildMiniChart(): void {
    const hourlyTotals = this.getHourlyTotals();
    if (hourlyTotals.length === 0) {
      this.miniChartPoints = [];
      this.miniChartXTicks = [];
      this.miniChartYTicks = [];
      this.miniChartPath = '';
      return;
    }

    const count = hourlyTotals.length;
    const dataMax = hourlyTotals.reduce((max, item) => Math.max(max, item.total), 0);
    // Usa el mayor entre el tope configurado y el máximo real y agrega margen visual superior.
    const baseMax = Math.max(this.chartYMax || 0, dataMax, 1);
    const safeMax = Math.ceil(baseMax * 1.1);

    this.miniChartPoints = hourlyTotals.map((item, idx) => {
      const x = this.miniChartLeft + (count === 1 ? 0 : (idx / (count - 1)) * this.miniChartWidth);
      const clamped = Math.max(0, Math.min(item.total, safeMax));
      const ratio = clamped / safeMax;
      const y = this.miniChartTop + this.miniChartHeight - ratio * this.miniChartHeight;
      return { x, y, value: item.total, label: item.hour };
    });

    this.miniChartPath = this.miniChartPoints
      .map((p, idx) => `${idx === 0 ? 'M' : 'L'}${p.x.toFixed(1)} ${p.y.toFixed(1)}`)
      .join(' ');

    this.miniChartXTicks = [0, 6, 12, 18]
      .filter((idx) => idx < hourlyTotals.length)
      .map((idx) => ({
        x: this.miniChartPoints[idx]?.x ?? this.miniChartLeft,
        label: hourlyTotals[idx]?.hour.slice(0, 2) ?? '--'
      }));

    this.miniChartYTicks = [0, Math.round(safeMax / 2), safeMax].map((value) => {
      const ratio = value / safeMax;
      const y = this.miniChartTop + this.miniChartHeight - ratio * this.miniChartHeight;
      return { y, value };
    });
  }

  private getRowTotal(row: MeasureMatrixRow): number {
    return Object.values(row.values)
      .filter((value): value is number => value !== null && value !== undefined)
      .map((value) => Number(value))
      .filter((value) => Number.isFinite(value))
      .reduce((acc, value) => acc + value, 0);
  }

  private getHourlyTotals(): Array<{ hour: string; total: number }> {
    const hourly = new Map<string, number>();
    for (let hour = 0; hour < 24; hour++) {
      hourly.set(String(hour).padStart(2, '0'), 0);
    }

    this.rows.forEach((row) => {
      const hourKey = row.hour.slice(0, 2);
      if (!hourly.has(hourKey)) {
        return;
      }

      const rowTotal = this.getRowTotal(row);
      hourly.set(hourKey, (hourly.get(hourKey) ?? 0) + rowTotal);
    });

    return Array.from(hourly.entries()).map(([hour, total]) => ({ hour: `${hour}:00`, total }));
  }

  private parseDate(value: string | Date): Date | null {
    if (value instanceof Date) {
      return Number.isNaN(value.getTime()) ? null : value;
    }

    const normalized = value.trim();
    if (!normalized) {
      return null;
    }

    // Evita desfases por zona horaria cuando llega solo yyyy-mm-dd.
    const plainDateMatch = normalized.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (plainDateMatch) {
      const [, year, month, day] = plainDateMatch;
      const parsed = new Date(Number(year), Number(month) - 1, Number(day));
      return Number.isNaN(parsed.getTime()) ? null : parsed;
    }

    const parsed = new Date(normalized);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  formatAxisValue(value: number): string {
    return Math.ceil(value).toLocaleString('es-ES');
  }
}

