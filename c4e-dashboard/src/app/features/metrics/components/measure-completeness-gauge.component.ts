import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

interface MeasureColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

@Component({
  selector: 'app-measure-completeness-gauge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="measure-gauge-card"
      role="button"
      tabindex="0"
      aria-label="Abrir estado de clientes en defectuosos"
      (click)="requestOpenAuditor()"
      (keydown.enter)="requestOpenAuditor()"
      (keydown.space)="requestOpenAuditor(); $event.preventDefault()"
    >
      <div class="measure-gauge-title">
        Estado clientes <span class="status-emoji" [attr.title]="statusTooltip">{{ statusEmoji }}</span>
      </div>

      <svg viewBox="0 0 220 130" class="measure-gauge-svg" role="img" aria-label="Clientes completos vs incompletos">
        <path [attr.d]="fullArcPath" class="measure-gauge-track" />
        <path [attr.d]="detailArcPath" class="measure-gauge-detail" />
        <path [attr.d]="okArcPath" class="measure-gauge-ok" />

        <text x="110" y="78" text-anchor="middle" class="measure-gauge-center">{{ defectiveClients }}</text>
        <text x="110" y="95" text-anchor="middle" class="measure-gauge-sub">Defectos</text>
      </svg>

      <div class="measure-gauge-metric" [attr.title]="'Total clientes / con detalle'">
        <span class="metric-value" [attr.data-tooltip]="'Total clientes / clientes con detalle'">
          {{ totalClients }} / {{ detailedClients }}
        </span>
      </div>
    </div>
  `,
  styles: [`
    .measure-gauge-card {
      cursor: pointer;
      transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
      position: relative;
    }

    .measure-gauge-card:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(15, 23, 42, 0.12);
      border-color: #bfdbfe;
    }

    .measure-gauge-card:hover::after {
      content: 'Haz clic para ver defectuosos';
      position: absolute;
      bottom: -28px;
      left: 50%;
      transform: translateX(-50%);
      background-color: rgba(15, 23, 42, 0.9);
      color: white;
      padding: 6px 10px;
      border-radius: 4px;
      font-size: 11px;
      white-space: nowrap;
      z-index: 1000;
      pointer-events: none;
    }

    .measure-gauge-card:focus-visible {
      outline: 2px solid #60a5fa;
      outline-offset: 2px;
      box-shadow: 0 0 0 3px rgba(147, 197, 253, 0.35);
    }

    .measure-gauge-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .status-emoji {
      font-size: 1.2em;
      cursor: help;
    }

    .metric-value {
      position: relative;
      cursor: help;
      border-bottom: 1px dotted rgba(0, 0, 0, 0.2);
    }

    .metric-value:hover::after {
      content: attr(data-tooltip);
      position: absolute;
      bottom: 125%;
      left: 50%;
      transform: translateX(-50%);
      background-color: rgba(0, 0, 0, 0.8);
      color: white;
      padding: 8px 12px;
      border-radius: 4px;
      font-size: 12px;
      white-space: nowrap;
      z-index: 1000;
      pointer-events: none;
      font-weight: 500;
    }

    .metric-value:hover::before {
      content: '';
      position: absolute;
      bottom: 115%;
      left: 50%;
      transform: translateX(-50%);
      border: 5px solid transparent;
      border-top-color: rgba(0, 0, 0, 0.8);
      z-index: 1000;
    }
  `]
})
export class MeasureCompletenessGaugeComponent {
  @Input() clientIds: number[] = [];
  @Input() columnValidation: Record<string, MeasureColumnValidation> = {};
  @Output() openAuditorRequested = new EventEmitter<void>();

  requestOpenAuditor(): void {
    this.openAuditorRequested.emit();
  }

  get totalClients(): number {
    return this.clientIds.length;
  }

  get detailedClients(): number {
    return this.clientIds.filter((clientId) => (this.columnValidation[String(clientId)]?.missing ?? 0) > 0).length;
  }

  get defectiveClients(): number {
    return this.detailedClients;
  }

  get okClients(): number {
    return Math.max(this.totalClients - this.detailedClients, 0);
  }

  get okRatio(): number {
    if (this.totalClients <= 0) return 0;
    return this.okClients / this.totalClients;
  }

  get statusEmoji(): string {
    if (this.totalClients === 0) return '🤔';
    if (this.okRatio >= 1.0) return '😄';
    if (this.okRatio >= 0.7) return '😊';
    if (this.okRatio >= 0.4) return '😐';
    return '😞';
  }

  get statusTooltip(): string {
    if (this.totalClients === 0) return 'Sin datos - Esperando información';
    const percentage = Math.round(this.okRatio * 100);
    if (this.okRatio >= 1.0) return `${percentage}% OK - ¡Perfecto!`;
    if (this.okRatio >= 0.7) return `${percentage}% OK - Muy bien`;
    if (this.okRatio >= 0.4) return `${percentage}% OK - Revisar`;
    return `${percentage}% OK - Atención requerida`;
  }

  get fullArcPath(): string {
    return this.describeArc(110, 108, 72, -180, 0);
  }

  get detailArcPath(): string {
    return this.describeArc(110, 108, 72, -180, 0);
  }

  get okArcPath(): string {
    const end = -180 + (180 * this.okRatio);
    return this.describeArc(110, 108, 72, -180, end);
  }

  private describeArc(cx: number, cy: number, radius: number, startAngle: number, endAngle: number): string {
    if (endAngle <= startAngle) {
      return '';
    }

    const start = this.polarToCartesian(cx, cy, radius, endAngle);
    const end = this.polarToCartesian(cx, cy, radius, startAngle);
    const largeArcFlag = endAngle - startAngle > 180 ? 1 : 0;

    return `M ${start.x} ${start.y} A ${radius} ${radius} 0 ${largeArcFlag} 0 ${end.x} ${end.y}`;
  }

  private polarToCartesian(cx: number, cy: number, radius: number, angleInDegrees: number): { x: number; y: number } {
    const angleInRadians = (angleInDegrees * Math.PI) / 180;
    return {
      x: cx + radius * Math.cos(angleInRadians),
      y: cy + radius * Math.sin(angleInRadians)
    };
  }
}
