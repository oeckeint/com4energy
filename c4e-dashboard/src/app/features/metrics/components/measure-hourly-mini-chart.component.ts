import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, HostListener, Input, NgZone, OnChanges, OnDestroy, Output, ViewChild } from '@angular/core';

interface MeasureMatrixRow {
  hour: string;
  values: Record<string, number | null>;
}

interface MiniChartPoint {
  x: number;
  y: number;
  value: number;
  formattedValue: string;
  label: string;
}

interface MiniChartTick {
  x: number;
  label: string;
}

interface MiniChartYTick {
  y: number;
  value: number;
  formattedLabel: string;
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
    @if (hasData && rows.length > 0 && miniChartPoints.length > 0) {
      <div
        class="measure-mini-chart-card measure-mini-chart-card--interactive"
        role="button"
        tabindex="0"
        aria-label="Abrir gráfica en grande"
        (click)="openModal($event)"
        (keydown.enter)="openModal($event)"
        (keydown.space)="openModal($event)"
      >
        <div class="measure-mini-chart-title">
          Totales por hora <span class="status-emoji" [attr.title]="statusTooltip">{{ statusEmoji }}</span>
        </div>
        <svg viewBox="0 0 300 150" class="measure-mini-chart" role="img" aria-label="Totales por hora">
          <line x1="28" y1="118" x2="292" y2="118" class="measure-mini-axis" />
          <line x1="28" y1="12" x2="28" y2="118" class="measure-mini-axis" />

          @for (tick of miniChartYTicks; track tick.value) {
            <line x1="24" [attr.y1]="tick.y" x2="292" [attr.y2]="tick.y" class="measure-mini-grid" />
            <text x="22" [attr.y]="tick.y + 3" text-anchor="end" class="measure-mini-tick-label">{{ tick.formattedLabel }}</text>
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
              [attr.title]="point.label + ': ' + point.formattedValue"
            />
          }

          @for (tick of miniChartXTicks; track tick.label) {
            <text [attr.x]="tick.x" y="133" text-anchor="middle" class="measure-mini-tick-label">{{ tick.label }}</text>
          }
        </svg>
        @if (weekdayLabel) {
          <div class="measure-mini-weekday">{{ weekdayLabel }}</div>
        }
        <div class="measure-mini-hover-overlay">Ampliar</div>
      </div>

      @if (isModalOpen) {
        <div class="measure-mini-modal-backdrop" (click)="closeModal($event)">
          <div class="measure-mini-modal" (click)="clearModalSelection($event)">
            <div class="measure-mini-modal-header" (click)="$event.stopPropagation()">
              <div class="measure-mini-modal-nav">
                <button type="button" class="measure-mini-nav-btn" (click)="navigateDay(-1, $event)" aria-label="Día anterior" title="Día anterior">&#8249;</button>
                <div class="measure-mini-modal-title">
                  @if (currentDateLabel) { {{ currentDateLabel }} }
                </div>
                <button type="button" class="measure-mini-nav-btn" (click)="navigateDay(1, $event)" aria-label="Día siguiente" title="Día siguiente">&#8250;</button>
              </div>
              <button type="button" class="measure-mini-modal-close" (click)="closeModal($event)" aria-label="Cerrar">&times;</button>
            </div>

            <div class="measure-mini-modal-controls" (click)="$event.stopPropagation()">
              <label class="measure-mini-control-item">
                <input type="checkbox" [checked]="showMaxLine" (change)="toggleLinePref('max')" />
                <span class="measure-mini-control-text measure-mini-control-text--max">Máximo</span>
              </label>
              <label class="measure-mini-control-item">
                <input type="checkbox" [checked]="showMinLine" (change)="toggleLinePref('min')" />
                <span class="measure-mini-control-text measure-mini-control-text--min">Mínimo</span>
              </label>
              <label class="measure-mini-control-item">
                <input type="checkbox" [checked]="showAvgLine" (change)="toggleLinePref('avg')" />
                <span class="measure-mini-control-text measure-mini-control-text--avg">Promedio</span>
              </label>
              <label class="measure-mini-control-item">
                <input type="checkbox" [checked]="showTrendLine" (change)="toggleLinePref('trend')" />
                <span class="measure-mini-control-text measure-mini-control-text--trend">Tendencia</span>
              </label>
              <label class="measure-mini-control-item">
                <input type="checkbox" [checked]="showAccumulatedLine" (change)="toggleLinePref('accum')" />
                <span class="measure-mini-control-text measure-mini-control-text--accum">Acumulada</span>
              </label>
            </div>

            <svg
              viewBox="0 0 920 420"
              class="measure-mini-chart measure-mini-chart--modal"
              role="img"
              aria-label="Totales por hora en grande"
              tabindex="0"
              (keydown)="onModalChartKeydown($event)"
              (click)="selectModalPoint($event)"
            >
              <line x1="64" y1="334" x2="860" y2="334" class="measure-mini-axis" />
              <line x1="64" y1="22" x2="64" y2="334" class="measure-mini-axis" />

              @if (showAccumulatedLine) {
                <line x1="860" y1="22" x2="860" y2="334" class="measure-mini-axis measure-mini-axis--accum" />
              }

              @for (tick of modalChartYTicks; track tick.value) {
                <line x1="64" [attr.y1]="tick.y" x2="860" [attr.y2]="tick.y" class="measure-mini-grid measure-mini-grid--modal" />
                <text x="52" [attr.y]="tick.y + 4" text-anchor="end" class="measure-mini-tick-label measure-mini-tick-label--modal">{{ tick.formattedLabel }}</text>
              }

              @for (tick of modalChartXTicks; track tick.label) {
                <line [attr.x1]="tick.x" y1="22" [attr.x2]="tick.x" y2="334" class="measure-mini-grid measure-mini-grid-vertical--modal" />
              }

              @if (showMaxLine && modalMaxValue > 0) {
                <line x1="64" [attr.y1]="valueToModalY(modalMaxValue)" x2="860" [attr.y2]="valueToModalY(modalMaxValue)" class="measure-mini-ref-line measure-mini-ref-line--max" />
              }

              @if (showMinLine && modalMinValue > 0) {
                <line x1="64" [attr.y1]="valueToModalY(modalMinValue)" x2="860" [attr.y2]="valueToModalY(modalMinValue)" class="measure-mini-ref-line measure-mini-ref-line--min" />
              }

              @if (showAvgLine && modalAvgValue > 0) {
                <line x1="64" [attr.y1]="valueToModalY(modalAvgValue)" x2="860" [attr.y2]="valueToModalY(modalAvgValue)" class="measure-mini-ref-line measure-mini-ref-line--avg" />
              }

              @if (showTrendLine && modalTrendPath) {
                <path [attr.d]="modalTrendPath" class="measure-mini-ref-line measure-mini-ref-line--trend" />
              }

              @if (showAccumulatedLine && modalAccumulatedPath) {
                <path [attr.d]="modalAccumulatedPath" class="measure-mini-ref-line measure-mini-ref-line--accum" />
              }

              @if (modalChartPath) {
                <path [attr.d]="modalChartPath" class="measure-mini-line measure-mini-line--modal" />
              }

              @for (point of modalChartPoints; track point.label) {
                <circle
                  [attr.cx]="point.x"
                  [attr.cy]="point.y"
                  r="3.1"
                  class="measure-mini-dot measure-mini-dot--modal"
                  [attr.title]="point.label + ': ' + point.formattedValue"
                />
              }

              @for (tick of modalChartXTicks; track tick.label) {
                <text [attr.x]="tick.x" y="352" text-anchor="middle" class="measure-mini-tick-label measure-mini-tick-label--modal">{{ tick.label }}</text>
              }

               @if (showAccumulatedLine) {
                 @for (tick of modalAccumulatedYTicks; track tick.value) {
                   <text x="868" [attr.y]="tick.y + 4" text-anchor="start" class="measure-mini-tick-label measure-mini-tick-label--modal measure-mini-tick-label--accum">{{ tick.formattedLabel }}</text>
                 }
               }

               <!-- Leyenda eje X (Horas) -->
               <text x="462" y="396" text-anchor="middle" class="measure-mini-axis-label">Horas</text>

               <!-- Leyenda eje Y (Totales) - Rotada verticalmente -->
               <text x="14" y="178" text-anchor="middle" transform="rotate(-90 14 178)" class="measure-mini-axis-label">Totales</text>

               @if (isPointSelectionActive && hoveredModalPoint) {
                <line [attr.x1]="hoveredModalPoint.x" y1="22" [attr.x2]="hoveredModalPoint.x" y2="334" class="measure-mini-crosshair" />
                <circle [attr.cx]="hoveredModalPoint.x" [attr.cy]="hoveredModalPoint.y" r="5" class="measure-mini-active-dot" />

                <g [attr.transform]="'translate(' + hoveredTooltipX + ',' + hoveredTooltipY + ')'">
                  <rect width="126" height="36" rx="6" ry="6" class="measure-mini-tooltip-box" />
                  <text x="8" y="14" class="measure-mini-tooltip-text">{{ hoveredModalPoint.label }}</text>
                  <text x="8" y="29" class="measure-mini-tooltip-text measure-mini-tooltip-text--strong">{{ hoveredModalPoint.formattedValue }}</text>
                </g>
              }
            </svg>
          </div>
        </div>
      }
    } @else if (showEmptyStateWhenNoData) {
      <div class="measure-mini-chart-card measure-mini-chart-card--empty" role="status" aria-live="polite">
        <div class="measure-mini-empty-icon" aria-hidden="true">{{ emptyStateIcon }}</div>
        <div class="measure-mini-empty-title">{{ emptyStateTitle }}</div>
        <div class="measure-mini-empty-description">{{ emptyStateDescription }}</div>
      </div>
    }
  `,
  styles: [`
    .measure-mini-chart-card {
      position: relative;
      cursor: default;
      transition: filter 0.18s ease;
    }

    .measure-mini-chart-card--interactive {
      cursor: zoom-in;
    }

    .measure-mini-chart-card--interactive:hover,
    .measure-mini-chart-card--interactive:focus-visible {
      filter: brightness(0.92);
      outline: none;
    }

    .measure-mini-hover-overlay {
      position: absolute;
      right: 10px;
      bottom: 8px;
      font-size: 0.72rem;
      padding: 2px 8px;
      border-radius: 999px;
      background: rgba(15, 23, 42, 0.65);
      color: #f8fafc;
      opacity: 0;
      transform: translateY(3px);
      transition: opacity 0.18s ease, transform 0.18s ease;
      pointer-events: none;
    }

    .measure-mini-chart-card--interactive:hover .measure-mini-hover-overlay,
    .measure-mini-chart-card--interactive:focus-visible .measure-mini-hover-overlay {
      opacity: 1;
      transform: translateY(0);
    }

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

    .measure-mini-chart-card--empty {
      min-height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      gap: 0.35rem;
      padding: 0.75rem;
    }

    .measure-mini-empty-icon {
      font-size: 1.25rem;
      line-height: 1;
    }

    .measure-mini-empty-title {
      font-size: 0.86rem;
      font-weight: 700;
      color: #344054;
    }

    .measure-mini-empty-description {
      font-size: 0.76rem;
      color: #667085;
      line-height: 1.35;
      max-width: 26ch;
    }

    .measure-mini-modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.58);
      z-index: 2500;
      display: grid;
      place-items: center;
      padding: 24px;
      animation: measureMiniBackdropIn 0.18s ease;
    }

    .measure-mini-modal {
      width: min(96vw, 980px);
      max-height: 92vh;
      background: #ffffff;
      border-radius: 14px;
      box-shadow: 0 24px 60px rgba(15, 23, 42, 0.35);
      padding: 20px 14px 14px;
      display: flex;
      flex-direction: column;
      gap: 16px;
      animation: measureMiniModalIn 0.18s ease;
      align-items: center;
    }

    @keyframes measureMiniBackdropIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes measureMiniModalIn {
      from {
        opacity: 0;
        transform: scale(0.97) translateY(8px);
      }
      to {
        opacity: 1;
        transform: scale(1) translateY(0);
      }
    }

    .measure-mini-modal-header {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      width: 100%;
      position: relative;
      padding-top: 12px;
    }

    .measure-mini-modal-nav {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }

    .measure-mini-nav-btn {
      width: 30px;
      height: 30px;
      border-radius: 999px;
      border: 1px solid #cbd5e1;
      background: #ffffff;
      color: #334155;
      font-size: 1.15rem;
      line-height: 1;
      display: grid;
      place-items: center;
      transition: background-color 0.15s ease, border-color 0.15s ease;
    }

    .measure-mini-nav-btn:hover {
      background: #f1f5f9;
      border-color: #94a3b8;
    }

    .measure-mini-modal-controls {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      padding: 2px 2px 6px;
      flex-wrap: wrap;
      margin-top: 8px;
    }

    .measure-mini-control-item {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-size: 0.86rem;
      color: #334155;
      user-select: none;
    }

    .measure-mini-control-text--max { color: #b91c1c; }
    .measure-mini-control-text--min { color: #166534; }
    .measure-mini-control-text--avg { color: #b45309; }
    .measure-mini-control-text--trend { color: #7c3aed; }
    .measure-mini-control-text--accum { color: #0f766e; }

    .measure-mini-modal-title {
      font-size: 1rem;
      font-weight: 600;
      color: #0f172a;
      text-transform: capitalize;
    }

    .measure-mini-modal-close {
      border: 1px solid #d1d5db;
      border-radius: 8px;
      width: 32px;
      height: 32px;
      font-size: 1.3rem;
      line-height: 1;
      background: #ffffff;
      color: #334155;
      position: absolute;
      right: 12px;
      top: 12px;
    }

    .measure-mini-chart--modal {
      width: 100%;
      height: min(70vh, 560px);
    }

    .measure-mini-chart--modal:focus {
      outline: none;
    }

    .measure-mini-chart--modal:focus-visible {
      outline: 2px solid #0ea5e9;
      outline-offset: 2px;
    }

    .measure-mini-tick-label--modal {
      font-size: 9.5px;
      fill: #475569;
    }

    .measure-mini-line--modal {
      stroke-width: 2.4;
    }

    .measure-mini-dot--modal {
      stroke-width: 0;
    }

    .measure-mini-ref-line {
      stroke-width: 1.6;
      stroke-dasharray: 6 4;
      opacity: 0.9;
    }

    .measure-mini-grid--modal {
      stroke: #94a3b8;
      stroke-opacity: 0.2;
      stroke-width: 1;
    }

    .measure-mini-grid-vertical--modal {
      stroke: #94a3b8;
      stroke-opacity: 0.14;
      stroke-width: 1;
    }

    .measure-mini-ref-line--max { stroke: #ef4444; }
    .measure-mini-ref-line--min { stroke: #22c55e; }
    .measure-mini-ref-line--avg { stroke: #f59e0b; stroke-dasharray: 3 4; }
    .measure-mini-ref-line--trend { stroke: #7c3aed; stroke-width: 2; stroke-dasharray: 7 5; fill: none; }
    .measure-mini-ref-line--accum { stroke: #0f766e; stroke-width: 2.1; stroke-dasharray: none; fill: none; opacity: 0.9; }

    .measure-mini-axis--accum {
      stroke: #0f766e;
      stroke-opacity: 0.45;
      stroke-width: 1;
    }

    .measure-mini-tick-label--accum {
      fill: #0f766e;
      opacity: 0.75;
    }

    .measure-mini-axis-label {
      font-size: 13px;
      font-weight: 600;
      fill: #334155;
      letter-spacing: 0.3px;
    }

    .measure-mini-crosshair {
      stroke: #475569;
      stroke-width: 1.2;
      stroke-dasharray: 3 4;
      opacity: 0.65;
    }

    .measure-mini-active-dot {
      fill: #0ea5e9;
      stroke: #ffffff;
      stroke-width: 1.5;
    }

    .measure-mini-tooltip-box {
      fill: rgba(15, 23, 42, 0.76);
      stroke: rgba(148, 163, 184, 0.28);
      stroke-width: 1;
    }

    .measure-mini-tooltip-text {
      fill: #e2e8f0;
      font-size: 11px;
      font-weight: 500;
    }

    .measure-mini-tooltip-text--strong {
      fill: #ffffff;
      font-size: 12px;
      font-weight: 700;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MeasureHourlyMiniChartComponent implements OnChanges, OnDestroy {
  private readonly miniChartLeft = 28;
  private readonly miniChartTop = 12;
  private readonly miniChartWidth = 264;
  private readonly miniChartHeight = 106;
  private readonly modalChartLeft = 64;
  private readonly modalChartTop = 22;
  private readonly modalChartWidth = 796;
  private readonly modalChartHeight = 312;
  private readonly numberFormatter = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 0 });
  private readonly linePrefsStorageKey = 'c4e:measure-mini-chart:line-prefs:v1';
  private modalSvgElement: SVGElement | null = null;

  @Input() rows: MeasureMatrixRow[] = [];
  @Input() chartYMax = 0;
  @Input() columnValidation: Record<string, MeasureColumnValidation> = {};
  @Input() clientIds: number[] = [];
  @Input() currentDate: string | Date | null = null;
  @Input() hasData = true;
  @Input() showEmptyStateWhenNoData = false;
  @Input() emptyStateIcon = '📊';
  @Input() emptyStateTitle = 'Sin datos para graficar';
  @Input() emptyStateDescription = 'Selecciona un cliente o rango de fechas con mediciones.';
  @Output() dayChange = new EventEmitter<string>();

  miniChartPoints: MiniChartPoint[] = [];
  miniChartXTicks: MiniChartTick[] = [];
  miniChartYTicks: MiniChartYTick[] = [];
  miniChartPath = '';
  modalChartPoints: MiniChartPoint[] = [];
  modalChartXTicks: MiniChartTick[] = [];
  modalChartYTicks: MiniChartYTick[] = [];
  modalChartPath = '';
  statusEmoji = '';
  statusTooltip = '';
  isModalOpen = false;
  showMaxLine = false;
  showMinLine = false;
  showAvgLine = false;
  showTrendLine = false;
  showAccumulatedLine = false;
  isPointSelectionActive = false;
  hoveredModalPointIndex: number | null = null;
  hoveredModalPoint: MiniChartPoint | null = null;
  hoveredTooltipX = 0;
  hoveredTooltipY = 0;
  modalScaleMax = 1;
  modalMaxValue = 0;
  modalMinValue = 0;
  modalAvgValue = 0;
  modalTrendPath = '';
  modalAccumulatedPath = '';
  modalAccumulatedScaleMax = 1;
  modalAccumulatedYTicks: MiniChartYTick[] = [];
  private hoverFrameId: number | null = null;
  private pendingRelativeX: number | null = null;
  private readonly onModalMouseMoveNative = (event: MouseEvent) => this.handleModalMouseMoveNative(event);
  private readonly onModalMouseLeaveNative = () => this.handleModalMouseLeaveNative();

  constructor(private readonly ngZone: NgZone) {
    this.loadLinePrefs();
  }

  @ViewChild('modalSvg')
  set modalSvgRef(ref: ElementRef<SVGElement> | undefined) {
    const nextEl = ref?.nativeElement ?? null;
    if (this.modalSvgElement === nextEl) {
      return;
    }

    this.detachModalSvgListeners();
    this.modalSvgElement = nextEl;
    this.attachModalSvgListeners();
  }

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

  get currentDateLabel(): string {
    if (!this.currentDate) {
      return '';
    }

    const date = this.parseDate(this.currentDate);
    if (!date) {
      return '';
    }

    const dayName = date.toLocaleDateString('es-ES', { weekday: 'long' });
    const formattedDate = date.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
    return `${dayName} ${formattedDate}`;
  }

  ngOnDestroy(): void {
    if (this.hoverFrameId !== null) {
      cancelAnimationFrame(this.hoverFrameId);
      this.hoverFrameId = null;
    }
    this.detachModalSvgListeners();
  }

  ngOnChanges(): void {
    this.rebuildMiniChart();
    this.updateStatusIndicator();
  }

  openModal(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();

    if (this.miniChartPoints.length === 0) {
      return;
    }

    this.isModalOpen = true;
    this.isPointSelectionActive = false;
    this.setHoveredModalPoint(null);
  }

  toggleLinePref(key: 'max' | 'min' | 'avg' | 'trend' | 'accum'): void {
    if (key === 'max') this.showMaxLine = !this.showMaxLine;
    if (key === 'min') this.showMinLine = !this.showMinLine;
    if (key === 'avg') this.showAvgLine = !this.showAvgLine;
    if (key === 'trend') this.showTrendLine = !this.showTrendLine;
    if (key === 'accum') this.showAccumulatedLine = !this.showAccumulatedLine;
    this.saveLinePrefs();
  }

  closeModal(event?: Event): void {
    event?.stopPropagation();
    this.isModalOpen = false;
    this.isPointSelectionActive = false;
    this.setHoveredModalPoint(null);
  }

  clearModalSelection(event?: Event): void {
    event?.stopPropagation();
    this.isPointSelectionActive = false;
    this.setHoveredModalPoint(null);
  }

  selectModalPoint(event: MouseEvent): void {
    event.stopPropagation();
    if (this.modalChartPoints.length === 0) {
      return;
    }

    const svg = event.currentTarget as SVGElement | null;
    if (!svg) {
      return;
    }

    const rect = svg.getBoundingClientRect();
    if (rect.width <= 0) {
      return;
    }

    const relativeX = ((event.clientX - rect.left) / rect.width) * 920;
    const ratio = (relativeX - this.modalChartLeft) / this.modalChartWidth;
    const index = Math.round(ratio * (this.modalChartPoints.length - 1));
    const clampedIndex = Math.max(0, Math.min(index, this.modalChartPoints.length - 1));

    if (this.isPointSelectionActive && this.hoveredModalPointIndex === clampedIndex) {
      this.isPointSelectionActive = false;
      this.setHoveredModalPoint(null);
      return;
    }

    this.isPointSelectionActive = true;
    this.setHoveredModalPoint(clampedIndex);
  }

  onModalChartKeydown(event: KeyboardEvent): void {
    if (this.modalChartPoints.length === 0) {
      return;
    }

    const current = this.hoveredModalPointIndex ?? 0;
    const lastIndex = this.modalChartPoints.length - 1;

    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      event.stopPropagation();
      this.isPointSelectionActive = true;
      this.setHoveredModalPoint(Math.max(0, current - 1));
      return;
    }

    if (event.key === 'ArrowRight') {
      event.preventDefault();
      event.stopPropagation();
      this.isPointSelectionActive = true;
      this.setHoveredModalPoint(Math.min(lastIndex, current + 1));
      return;
    }

    if (event.key === 'Home') {
      event.preventDefault();
      event.stopPropagation();
      this.isPointSelectionActive = true;
      this.setHoveredModalPoint(0);
      return;
    }

    if (event.key === 'End') {
      event.preventDefault();
      event.stopPropagation();
      this.isPointSelectionActive = true;
      this.setHoveredModalPoint(lastIndex);
      return;
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      event.stopPropagation();
      if (this.isPointSelectionActive) {
        this.isPointSelectionActive = false;
        this.setHoveredModalPoint(null);
        return;
      }
      this.isPointSelectionActive = true;
      this.setHoveredModalPoint(this.hoveredModalPointIndex ?? 0);
      return;
    }

    if (event.key === 'Escape' && this.isPointSelectionActive) {
      event.preventDefault();
      event.stopPropagation();
      this.isPointSelectionActive = false;
      this.setHoveredModalPoint(null);
    }
  }

  navigateDay(step: number, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();

    const baseDate = this.parseDate(this.currentDate ?? new Date());
    if (!baseDate) {
      return;
    }

    const nextDate = new Date(baseDate.getFullYear(), baseDate.getMonth(), baseDate.getDate());
    nextDate.setDate(nextDate.getDate() + step);
    this.dayChange.emit(this.formatDateIso(nextDate));
  }

  @HostListener('document:keydown.escape')
  onEscapePressed(): void {
    if (this.isModalOpen) {
      this.closeModal();
    }
  }

  private handleModalMouseMoveNative(event: MouseEvent): void {
    if (this.modalChartPoints.length === 0) {
      this.ngZone.run(() => this.setHoveredModalPoint(null));
      return;
    }

    const svg = event.currentTarget as SVGElement | null;
    if (!svg) {
      return;
    }

    const rect = svg.getBoundingClientRect();
    if (rect.width <= 0) {
      return;
    }

    this.pendingRelativeX = ((event.clientX - rect.left) / rect.width) * 920;
    if (this.hoverFrameId !== null) {
      return;
    }

    this.hoverFrameId = requestAnimationFrame(() => {
      this.hoverFrameId = null;
      if (this.pendingRelativeX === null) {
        return;
      }

      const ratio = (this.pendingRelativeX - this.modalChartLeft) / this.modalChartWidth;
      const index = Math.round(ratio * (this.modalChartPoints.length - 1));
      const clampedIndex = Math.max(0, Math.min(index, this.modalChartPoints.length - 1));

      if (clampedIndex !== this.hoveredModalPointIndex) {
        this.ngZone.run(() => this.setHoveredModalPoint(clampedIndex));
      }
    });
  }

  private handleModalMouseLeaveNative(): void {
    this.pendingRelativeX = null;
    if (this.hoverFrameId !== null) {
      cancelAnimationFrame(this.hoverFrameId);
      this.hoverFrameId = null;
    }
    this.ngZone.run(() => this.setHoveredModalPoint(null));
  }

  private attachModalSvgListeners(): void {
    if (!this.modalSvgElement) {
      return;
    }

    this.ngZone.runOutsideAngular(() => {
      this.modalSvgElement?.addEventListener('mousemove', this.onModalMouseMoveNative, { passive: true });
      this.modalSvgElement?.addEventListener('mouseleave', this.onModalMouseLeaveNative);
    });
  }

  private loadLinePrefs(): void {
    if (typeof window === 'undefined') {
      return;
    }

    try {
      const raw = window.localStorage.getItem(this.linePrefsStorageKey);
      if (!raw) {
        return;
      }

      const parsed = JSON.parse(raw) as Partial<Record<'max' | 'min' | 'avg' | 'trend' | 'accum', boolean>>;
      this.showMaxLine = parsed.max ?? this.showMaxLine;
      this.showMinLine = parsed.min ?? this.showMinLine;
      this.showAvgLine = parsed.avg ?? this.showAvgLine;
      this.showTrendLine = parsed.trend ?? this.showTrendLine;
      this.showAccumulatedLine = parsed.accum ?? this.showAccumulatedLine;
    } catch {
      // Ignorar prefs corruptas.
    }
  }

  private saveLinePrefs(): void {
    if (typeof window === 'undefined') {
      return;
    }

    const payload = {
      max: this.showMaxLine,
      min: this.showMinLine,
      avg: this.showAvgLine,
      trend: this.showTrendLine,
      accum: this.showAccumulatedLine
    };

    try {
      window.localStorage.setItem(this.linePrefsStorageKey, JSON.stringify(payload));
    } catch {
      // Si storage está bloqueado, no interrumpimos UX.
    }
  }

  private detachModalSvgListeners(): void {
    if (!this.modalSvgElement) {
      return;
    }

    this.modalSvgElement.removeEventListener('mousemove', this.onModalMouseMoveNative);
    this.modalSvgElement.removeEventListener('mouseleave', this.onModalMouseLeaveNative);
  }

  valueToModalY(value: number): number {
    const clamped = Math.max(0, Math.min(value, this.modalScaleMax));
    const ratio = clamped / this.modalScaleMax;
    return this.modalChartTop + this.modalChartHeight - ratio * this.modalChartHeight;
  }

  private setHoveredModalPoint(index: number | null): void {
    this.hoveredModalPointIndex = index;
    if (index === null) {
      this.hoveredModalPoint = null;
      this.hoveredTooltipX = 0;
      this.hoveredTooltipY = 0;
      return;
    }

    const point = this.modalChartPoints[index] ?? null;
    this.hoveredModalPoint = point;
    if (!point) {
      this.hoveredTooltipX = 0;
      this.hoveredTooltipY = 0;
      return;
    }

    this.hoveredTooltipX = point.x > 728 ? point.x - 132 : point.x + 10;
    const preferredY = point.y - 42;
    this.hoveredTooltipY = Math.max(this.modalChartTop + 4, preferredY);
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
      this.modalChartPoints = [];
      this.modalChartXTicks = [];
      this.modalChartYTicks = [];
      this.modalChartPath = '';
      this.modalMaxValue = 0;
      this.modalMinValue = 0;
      this.modalAvgValue = 0;
      this.modalTrendPath = '';
      this.modalAccumulatedPath = '';
      this.modalAccumulatedScaleMax = 1;
      this.modalAccumulatedYTicks = [];
      this.setHoveredModalPoint(null);
      return;
    }

    const count = hourlyTotals.length;
    const dataMax = hourlyTotals.reduce((max, item) => Math.max(max, item.total), 0);
    // Escala completamente dinámica basada en el máximo real del dataset filtrado.
    // Esto evita pisos artificiales (por ejemplo 2500/2800) que distorsionan la posición de puntos.
    const baseMax = Math.max(dataMax, 1);
    const safeMax = this.computeDynamicScaleMax(baseMax);
    this.modalScaleMax = safeMax;
    this.modalMaxValue = dataMax;
    this.modalMinValue = hourlyTotals.reduce((min, item) => Math.min(min, item.total), Number.POSITIVE_INFINITY);
    this.modalAvgValue = hourlyTotals.reduce((acc, item) => acc + item.total, 0) / hourlyTotals.length;

    const mini = this.buildChartGeometry(hourlyTotals, safeMax, this.miniChartLeft, this.miniChartTop, this.miniChartWidth, this.miniChartHeight);
    this.miniChartPoints = mini.points;
    this.miniChartXTicks = mini.xTicks;
    this.miniChartYTicks = mini.yTicks;
    this.miniChartPath = mini.path;

    const modal = this.buildChartGeometry(hourlyTotals, safeMax, this.modalChartLeft, this.modalChartTop, this.modalChartWidth, this.modalChartHeight);
    this.modalChartPoints = modal.points;
    this.modalChartXTicks = this.buildModalXTicks(hourlyTotals, this.modalChartPoints);
    this.modalChartYTicks = this.buildModalYTicks(safeMax);
    this.modalChartPath = this.buildSmoothPath(this.modalChartPoints);
    this.modalTrendPath = this.buildTrendPath(this.modalChartPoints);
    const accumulatedValues = this.buildAccumulatedValues(hourlyTotals);
    this.modalAccumulatedScaleMax = Math.max(accumulatedValues[accumulatedValues.length - 1] ?? 0, 1);
    this.modalAccumulatedYTicks = this.buildAccumulatedYTicks(this.modalAccumulatedScaleMax);
    this.modalAccumulatedPath = this.buildAccumulatedPath(this.modalChartPoints, accumulatedValues);

    if (this.isModalOpen) {
      this.setHoveredModalPoint(this.modalChartPoints.length > 0 ? 0 : null);
    }
  }

  private buildChartGeometry(
    hourlyTotals: Array<{ hour: string; total: number }>,
    safeMax: number,
    left: number,
    top: number,
    width: number,
    height: number
  ): { points: MiniChartPoint[]; xTicks: MiniChartTick[]; yTicks: MiniChartYTick[]; path: string } {
    const count = hourlyTotals.length;
    const points = hourlyTotals.map((item, idx) => {
      const x = left + (count === 1 ? 0 : (idx / (count - 1)) * width);
      const clamped = Math.max(0, Math.min(item.total, safeMax));
      const ratio = clamped / safeMax;
      const y = top + height - ratio * height;
      return { x, y, value: item.total, formattedValue: this.formatAxisValue(item.total), label: item.hour };
    });

    const path = points.map((p, idx) => `${idx === 0 ? 'M' : 'L'}${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');

    const xTicks = [0, 6, 12, 18]
      .filter((idx) => idx < hourlyTotals.length)
      .map((idx) => ({
        x: points[idx]?.x ?? left,
        label: hourlyTotals[idx]?.hour.slice(0, 2) ?? '--'
      }));

    const yTicks = [0, Math.round(safeMax / 2), safeMax].map((value) => {
      const ratio = value / safeMax;
      const y = top + height - ratio * height;
      return { y, value, formattedLabel: this.formatAxisValue(value) };
    });

    return { points, xTicks, yTicks, path };
  }

  private computeDynamicScaleMax(baseMax: number): number {
    const rawMax = Math.max(baseMax, 1) * 1.1;

    if (rawMax <= 100) {
      return Math.ceil(rawMax / 10) * 10;
    }
    if (rawMax <= 500) {
      return Math.ceil(rawMax / 25) * 25;
    }
    if (rawMax <= 2000) {
      return Math.ceil(rawMax / 50) * 50;
    }
    return Math.ceil(rawMax / 100) * 100;
  }

  private buildModalXTicks(hourlyTotals: Array<{ hour: string; total: number }>, points: MiniChartPoint[]): MiniChartTick[] {
    return hourlyTotals.map((item, idx) => ({
      x: points[idx]?.x ?? this.modalChartLeft,
      label: item.hour.slice(0, 2)
    }));
  }

  private buildModalYTicks(safeMax: number): MiniChartYTick[] {
    const tickCount = 10;
    if (tickCount <= 1) {
      return [{ y: this.modalChartTop + this.modalChartHeight, value: 0, formattedLabel: this.formatAxisValue(0) }];
    }

    return Array.from({ length: tickCount }, (_, idx) => {
      const ratio = idx / (tickCount - 1);
      const value = Math.round(safeMax * ratio);
      const y = this.modalChartTop + this.modalChartHeight - ratio * this.modalChartHeight;
      return {
        y,
        value,
        formattedLabel: this.formatAxisValue(value)
      };
    });
  }

  private buildAccumulatedValues(hourlyTotals: Array<{ hour: string; total: number }>): number[] {
    const accumulated: number[] = [];
    let running = 0;
    for (const item of hourlyTotals) {
      running += item.total;
      accumulated.push(running);
    }
    return accumulated;
  }

  private buildAccumulatedYTicks(scaleMax: number): MiniChartYTick[] {
    const tickCount = 5;
    return Array.from({ length: tickCount }, (_, idx) => {
      const ratio = idx / (tickCount - 1);
      const value = Math.round(scaleMax * ratio);
      const y = this.modalChartTop + this.modalChartHeight - ratio * this.modalChartHeight;
      return { y, value, formattedLabel: this.formatAxisValue(value) };
    });
  }

  private accumulatedValueToModalY(value: number): number {
    const clamped = Math.max(0, Math.min(value, this.modalAccumulatedScaleMax));
    const ratio = clamped / this.modalAccumulatedScaleMax;
    return this.modalChartTop + this.modalChartHeight - ratio * this.modalChartHeight;
  }

  private buildAccumulatedPath(points: MiniChartPoint[], accumulatedValues: number[]): string {
    if (points.length === 0 || accumulatedValues.length === 0) {
      return '';
    }

    const segments = points.map((point, idx) => {
      const y = this.accumulatedValueToModalY(accumulatedValues[idx] ?? 0);
      return `${idx === 0 ? 'M' : 'L'}${point.x.toFixed(1)} ${y.toFixed(1)}`;
    });

    return segments.join(' ');
  }

  private buildTrendPath(points: MiniChartPoint[]): string {
    const count = points.length;
    if (count < 2) {
      return '';
    }

    let sumX = 0;
    let sumY = 0;
    let sumXY = 0;
    let sumX2 = 0;

    for (let i = 0; i < count; i++) {
      const x = i;
      const y = points[i].value;
      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumX2 += x * x;
    }

    const denominator = count * sumX2 - sumX * sumX;
    if (denominator === 0) {
      return '';
    }

    const slope = (count * sumXY - sumX * sumY) / denominator;
    const intercept = (sumY - slope * sumX) / count;

    const startX = points[0].x;
    const endX = points[count - 1].x;
    const startY = this.valueToModalY(intercept);
    const endY = this.valueToModalY(intercept + slope * (count - 1));

    return `M${startX.toFixed(1)} ${startY.toFixed(1)} L${endX.toFixed(1)} ${endY.toFixed(1)}`;
  }

  private buildSmoothPath(points: MiniChartPoint[]): string {
    if (points.length === 0) {
      return '';
    }
    if (points.length < 3) {
      return points.map((p, idx) => `${idx === 0 ? 'M' : 'L'}${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
    }

    let path = `M${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
    for (let i = 0; i < points.length - 1; i++) {
      const p0 = points[i - 1] ?? points[i];
      const p1 = points[i];
      const p2 = points[i + 1];
      const p3 = points[i + 2] ?? p2;

      // Catmull-Rom -> Bezier para curva suave sin alterar puntos originales.
      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;

      path += ` C${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${p2.x.toFixed(1)} ${p2.y.toFixed(1)}`;
    }

    return path;
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

  private formatDateIso(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  formatAxisValue(value: number): string {
    const rounded = Math.ceil(value);
    const sign = rounded < 0 ? '-' : '';
    const digits = Math.abs(rounded).toString();
    const grouped = digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    return `${sign}${grouped}`;
  }

}
