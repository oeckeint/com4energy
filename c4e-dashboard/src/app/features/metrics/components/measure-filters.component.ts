import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

const ISO_DATE_REGEX = /^(\d{4})-(\d{2})-(\d{2})$/;

@Component({
  selector: 'app-measure-filters',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [class]="containerClass">
      <div class="card-body">
        @if (layoutMode === 'top-split') {
          <div class="measure-filters-top-split">
            <section class="card rounded-3 measure-filters-split-card">
              <div class="card-body measure-filters-split-card-body">
                <div class="measure-calendar-title-row">
                  <label class="form-label mb-0">Calendario</label>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-secondary measure-calendar-today-btn"
                    [class.is-active]="isCurrentDaySelected()"
                    title="Ir a hoy"
                    aria-label="Ir a hoy"
                    [disabled]="loading"
                    (click)="goToToday()"
                  >
                    Hoy
                  </button>
                </div>

                <div class="measure-calendar-toolbar">
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-secondary"
                    title="Mes anterior"
                    aria-label="Mes anterior"
                    [disabled]="loading"
                    (click)="goToPreviousMonth()"
                  >
                    ◀
                  </button>

                  <select
                    class="form-select form-select-sm measure-calendar-month-select"
                    [value]="viewMonth"
                    (change)="onMonthChanged($any($event.target).value)"
                    [disabled]="loading"
                    aria-label="Mes"
                  >
                    @for (month of monthNames; track month.value) {
                      <option [value]="month.value">{{ month.label }}</option>
                    }
                  </select>

                  <select
                    class="form-select form-select-sm measure-calendar-year-select"
                    [value]="viewYear"
                    (change)="onYearChanged($any($event.target).value)"
                    [disabled]="loading"
                    aria-label="Ano"
                  >
                    @for (year of yearOptions; track year) {
                      <option [value]="year">{{ year }}</option>
                    }
                  </select>

                  <button
                    type="button"
                    class="btn btn-sm btn-outline-secondary"
                    title="Mes siguiente"
                    aria-label="Mes siguiente"
                    [disabled]="loading"
                    (click)="goToNextMonth()"
                  >
                    ▶
                  </button>
                </div>

                <div class="measure-calendar-weekdays">
                  @for (dayName of weekDaysShort; track dayName) {
                    <span>{{ dayName }}</span>
                  }
                </div>

                <div class="measure-calendar-grid" role="grid" aria-label="Calendario mensual">
                  @for (cell of monthCells; track $index) {
                    <button
                      type="button"
                      class="measure-calendar-day"
                      [class.is-selected]="isSelectedDate(cell)"
                      [class.is-today]="isTodayDate(cell)"
                      [class.is-outside-month]="cell.getMonth() !== viewMonth || cell.getFullYear() !== viewYear"
                      [disabled]="loading"
                      (click)="onCalendarDateSelected(cell)"
                    >
                      {{ cell.getDate() }}
                    </button>
                  }
                </div>
              </div>
            </section>

            <section class="card rounded-3 measure-filters-split-card">
              <div class="card-body measure-filters-split-card-body measure-filters-right-panel">
                <div class="measure-filters-row measure-filters-row--clients">
                  <label class="form-label mb-1">Clientes</label>
                  <div class="measure-client-pill-box" (click)="clientPillInput.focus()">
                    @for (pill of clientIdPills; track pill) {
                      <span class="measure-client-pill">
                        <span>{{ pill }}</span>
                        <button
                          type="button"
                          class="measure-client-pill-remove"
                          aria-label="Eliminar cliente"
                          title="Eliminar cliente"
                          [disabled]="loading"
                          (click)="removeClientPill(pill, $event)"
                        >
                          &times;
                        </button>
                      </span>
                    }

                    <input
                      #clientPillInput
                      class="measure-client-pill-input"
                      [disabled]="loading"
                      [placeholder]="clientIdPills.length === 0 ? clientPlaceholder : ''"
                      [value]="clientIdDraft"
                      (input)="clientIdDraft = $any($event.target).value"
                      (keydown)="onClientDraftKeydown($event)"
                      (blur)="commitClientDraft()"
                      aria-label="Agregar ID cliente"
                    />
                  </div>
                </div>

                <div class="measure-filters-row measure-filters-row--tarifas">
                  <label class="form-label mb-1">Tarifas</label>
                  <select
                    class="form-select form-select-sm"
                    [value]="selectedTarifa"
                    (change)="onTarifaChange($any($event.target).value)"
                    [disabled]="loading"
                  >
                    <option value="">Todas las tarifas</option>
                    @for (tarifa of tarifasDisponibles; track tarifa) {
                      <option [value]="tarifa">{{ tarifa }}</option>
                    }
                  </select>
                </div>

                <div class="measure-filters-actions-row">
                  @if (showActionButtons) {
                    <div class="measure-client-actions">
                      <button class="btn btn-primary btn-sm px-3" (click)="searchRequested.emit()" [disabled]="loading || searchDisabled">Buscar</button>
                      <button class="btn btn-outline-secondary btn-sm px-3" (click)="clearRequested.emit()" [disabled]="loading || clearDisabled">Limpiar</button>
                    </div>
                  }
                </div>
              </div>
            </section>
          </div>
        } @else {
          <div class="row g-3 align-items-center">
            <div class="col-md-3">
              <div [class]="splitFieldsIntoCards ? 'measure-filter-field-shell card shadow-sm rounded-3 h-100' : 'measure-filter-field-shell'">
                <div [class]="splitFieldsIntoCards ? 'card-body py-2 px-3' : ''">
                  <label class="form-label">Fecha</label>
                  <div class="d-flex gap-2 align-items-center">
                    <button
                      class="btn btn-sm btn-outline-secondary"
                      (click)="previousDay()"
                      [disabled]="loading"
                      title="Día anterior"
                    >
                      ◀
                    </button>
                    <input
                      class="form-control"
                      type="date"
                      [value]="day"
                      (input)="dayChange.emit($any($event.target).value)"
                    />
                    <button
                      class="btn btn-sm btn-outline-secondary"
                      (click)="nextDay()"
                      [disabled]="loading"
                      title="Día siguiente"
                    >
                      ▶
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div class="col-md-6">
              <div [class]="splitFieldsIntoCards ? 'measure-filter-field-shell card shadow-sm rounded-3 h-100' : 'measure-filter-field-shell'">
                <div [class]="splitFieldsIntoCards ? 'card-body py-2 px-3' : ''">
                  <label class="form-label">Clientes</label>
                  <input
                    class="form-control"
                    [value]="clientIdsText"
                    (input)="clientIdsTextChange.emit($any($event.target).value)"
                    [placeholder]="clientPlaceholder"
                  />
                  <small class="text-muted">{{ helperText }}</small>
                </div>
              </div>
            </div>

            @if (showActionButtons) {
              <div class="col-md-3 d-flex gap-2">
                <button class="btn btn-primary" (click)="searchRequested.emit()" [disabled]="loading || searchDisabled">Buscar</button>
                <button class="btn btn-outline-secondary" (click)="clearRequested.emit()" [disabled]="loading || clearDisabled">Limpiar</button>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .measure-filters-top-split {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      align-items: stretch;
      gap: 0.75rem;
    }

    .measure-filters-split-card {
      border-color: #e4e7ec;
    }

    .measure-filters-split-card-body {
      padding: 0.75rem;
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      min-height: 0;
      height: 100%;
    }

    .measure-filters-right-panel {
      min-height: 0;
      display: grid;
      grid-template-rows: minmax(0, 1.1fr) auto auto;
      row-gap: 0.55rem;
      height: 100%;
    }

    .measure-filters-row {
      margin-bottom: 0;
    }

    .measure-filters-row .form-label {
      margin-bottom: 0.25rem;
    }

    .measure-filters-row--clients {
      min-height: 0;
      display: grid;
      grid-template-rows: auto minmax(0, 1fr);
    }

    .measure-client-pill-box {
      border: 1px solid #d0d5dd;
      border-radius: 0.5rem;
      min-height: 0;
      height: 100%;
      max-height: none;
      overflow-y: auto;
      padding: 0.4rem;
      display: flex;
      align-content: flex-start;
      align-items: flex-start;
      gap: 0.35rem;
      flex-wrap: wrap;
      background: #ffffff;
      cursor: text;
    }

    .measure-client-pill {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      background: #eff4ff;
      border: 1px solid #d6e4ff;
      color: #1d4ed8;
      border-radius: 999px;
      padding: 0.12rem 0.45rem;
      font-size: 0.78rem;
      line-height: 1.2;
      font-weight: 600;
    }

    .measure-client-pill-remove {
      border: 0;
      background: transparent;
      color: inherit;
      line-height: 1;
      font-size: 0.85rem;
      padding: 0;
      width: 1rem;
      height: 1rem;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 999px;
    }

    .measure-client-pill-remove:hover {
      background: rgba(29, 78, 216, 0.1);
    }

    .measure-client-pill-input {
      border: 0;
      outline: 0;
      min-width: 8rem;
      flex: 1 1 9rem;
      font-size: 0.92rem;
      line-height: 1.4;
      padding: 0.2rem 0;
    }

    .measure-client-actions {
      display: flex;
      gap: 0.5rem;
      align-items: center;
      justify-content: center;
      flex-wrap: nowrap;
    }

    .measure-calendar-toolbar {
      display: grid;
      grid-template-columns: 2rem minmax(4.3rem, 1fr) minmax(5.2rem, 1fr) 2rem;
      gap: 0.25rem;
      align-items: center;
      margin-bottom: 0.45rem;
    }

    .measure-calendar-title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 0.35rem;
      gap: 0.5rem;
    }

    .measure-calendar-today-btn {
      min-height: 1.55rem;
      padding: 0.12rem 0.45rem;
      font-size: 0.72rem;
      line-height: 1;
      border-color: #d0d5dd;
      background: #f8fafc;
      color: #475467;
      border-radius: 999px;
    }

    .measure-calendar-today-btn:hover,
    .measure-calendar-today-btn:focus-visible {
      background: #eef2f6;
      border-color: #cbd5e1;
      color: #344054;
    }

    .measure-calendar-today-btn.is-active {
      background: #dbeafe;
      border-color: #93c5fd;
      color: #1d4ed8;
      box-shadow: inset 0 0 0 1px #93c5fd;
      font-weight: 700;
    }

    .measure-calendar-month-select {
      min-width: 4.3rem;
    }

    .measure-calendar-year-select {
      min-width: 5rem;
    }

    .measure-calendar-weekdays {
      display: grid;
      grid-template-columns: repeat(7, minmax(0, 1fr));
      gap: 0.15rem;
      margin-bottom: 0.25rem;
      font-size: 0.7rem;
      font-weight: 700;
      color: #667085;
      text-align: center;
    }

    .measure-calendar-grid {
      display: grid;
      grid-template-columns: repeat(7, minmax(0, 1fr));
      grid-template-rows: repeat(6, minmax(0, 1fr));
      gap: 0.15rem;
      flex: 1 1 auto;
      min-height: 0;
    }

    .measure-calendar-day {
      height: 100%;
      border: 1px solid transparent;
      border-radius: 0.35rem;
      background: transparent;
      font-size: 0.78rem;
      line-height: 1;
      color: #344054;
      transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
    }

    .measure-calendar-day:hover {
      background: #eff4ff;
      border-color: #d6e4ff;
    }

    .measure-calendar-day.is-selected {
      background: #1570ef;
      border-color: #1570ef;
      color: #ffffff;
      font-weight: 700;
    }

    .measure-calendar-day.is-today {
      border-color: #98a2b3;
      font-weight: 700;
    }

    .measure-calendar-day.is-outside-month {
      color: #98a2b3;
    }

    .measure-calendar-day.is-outside-month:hover {
      background: #f2f4f7;
      border-color: #e4e7ec;
      color: #667085;
    }

    @media (max-width: 768px) {
      .measure-filters-top-split {
        grid-template-columns: 1fr;
      }

      .measure-calendar-toolbar {
        grid-template-columns: 2rem minmax(4.3rem, 1fr) 2rem;
      }

      .measure-calendar-toolbar select:last-of-type {
        grid-column: 2 / 3;
      }

      .measure-calendar-title-row {
        margin-bottom: 0.25rem;
      }
    }
  `]
})
export class MeasureFiltersComponent implements OnChanges {
  @Input() containerClass = 'card border-0 shadow-sm rounded-4 mb-4';
  @Input() day = '';
  @Input() clientIdsText = '';
  @Input() loading = false;
  @Input() calendarSyncToken = 0;
  @Input() clientPlaceholder = 'IDs separados por coma';
  @Input() helperText = 'Opcional. Separar IDs por coma; vacio = todos los clientes.';
  @Input() splitFieldsIntoCards = false;
  @Input() layoutMode: 'default' | 'top-split' = 'default';
  @Input() showActionButtons = true;
  @Input() searchDisabled = false;
  @Input() clearDisabled = false;
  @Input() selectedTarifa = '';
  @Input() tarifasDisponibles: string[] = [];

  readonly weekDaysShort = ['D', 'L', 'M', 'X', 'J', 'V', 'S'];
  readonly monthNames = [
    { value: 0, label: 'Ene' },
    { value: 1, label: 'Feb' },
    { value: 2, label: 'Mar' },
    { value: 3, label: 'Abr' },
    { value: 4, label: 'May' },
    { value: 5, label: 'Jun' },
    { value: 6, label: 'Jul' },
    { value: 7, label: 'Ago' },
    { value: 8, label: 'Sep' },
    { value: 9, label: 'Oct' },
    { value: 10, label: 'Nov' },
    { value: 11, label: 'Dic' }
  ];

  viewYear = new Date().getFullYear();
  viewMonth = new Date().getMonth();
  clientIdDraft = '';

  @Output() dayChange = new EventEmitter<string>();
  @Output() clientIdsTextChange = new EventEmitter<string>();
  @Output() selectedTarifaChange = new EventEmitter<string>();
  @Output() searchRequested = new EventEmitter<void>();
  @Output() clearRequested = new EventEmitter<void>();
  @Output() clientPillRemoved = new EventEmitter<void>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['day'] || changes['calendarSyncToken']) {
      this.syncCalendarToSelectedDay();
    }
  }

  get yearOptions(): number[] {
    const start = this.viewYear - 8;
    return Array.from({ length: 17 }, (_, index) => start + index);
  }

  get monthCells(): Date[] {
    const firstDay = new Date(this.viewYear, this.viewMonth, 1);
    const totalDays = new Date(this.viewYear, this.viewMonth + 1, 0).getDate();
    const previousMonthTotalDays = new Date(this.viewYear, this.viewMonth, 0).getDate();
    const leadingBlanks = firstDay.getDay();
    const cells: Date[] = [];

    for (let i = leadingBlanks; i > 0; i -= 1) {
      cells.push(new Date(this.viewYear, this.viewMonth - 1, previousMonthTotalDays - i + 1));
    }

    for (let day = 1; day <= totalDays; day += 1) {
      cells.push(new Date(this.viewYear, this.viewMonth, day));
    }

    let nextMonthDay = 1;
    while (cells.length < 42) {
      cells.push(new Date(this.viewYear, this.viewMonth + 1, nextMonthDay));
      nextMonthDay += 1;
    }

    return cells;
  }

  get clientIdPills(): string[] {
    return this.parseClientIdsText(this.clientIdsText);
  }

  previousDay(): void {
    const date = new Date(this.day || new Date());
    date.setDate(date.getDate() - 1);
    const newDay = date.toISOString().slice(0, 10);
    this.dayChange.emit(newDay);
  }

  nextDay(): void {
    const date = new Date(this.day || new Date());
    date.setDate(date.getDate() + 1);
    const newDay = date.toISOString().slice(0, 10);
    this.dayChange.emit(newDay);
  }

  goToPreviousMonth(): void {
    const date = new Date(this.viewYear, this.viewMonth, 1);
    date.setMonth(date.getMonth() - 1);
    this.viewYear = date.getFullYear();
    this.viewMonth = date.getMonth();
  }

  goToNextMonth(): void {
    const date = new Date(this.viewYear, this.viewMonth, 1);
    date.setMonth(date.getMonth() + 1);
    this.viewYear = date.getFullYear();
    this.viewMonth = date.getMonth();
  }

  goToToday(): void {
    const today = new Date();
    this.viewYear = today.getFullYear();
    this.viewMonth = today.getMonth();
    this.dayChange.emit(this.toIsoDate(today));
  }

  onMonthChanged(month: number): void {
    this.viewMonth = Number(month);
  }

  onYearChanged(year: number): void {
    this.viewYear = Number(year);
  }

  onTarifaChange(tarifa: string): void {
    this.selectedTarifaChange.emit(tarifa);
  }

  onCalendarDateSelected(date: Date): void {
    this.viewYear = date.getFullYear();
    this.viewMonth = date.getMonth();
    this.dayChange.emit(this.toIsoDate(date));
  }

  onClientDraftKeydown(event: KeyboardEvent): void {
    const key = event.key;
    if (key === 'Enter' || key === ',' || key === ';') {
      event.preventDefault();
      this.commitClientDraft();
      return;
    }

    if (key === 'Backspace' && !this.clientIdDraft.trim()) {
      const pills = this.clientIdPills;
      if (pills.length === 0) {
        return;
      }
      this.updateClientPills(pills.slice(0, -1));
      this.clientPillRemoved.emit();
    }
  }

  commitClientDraft(): void {
    const draftTokens = this.parseClientIdsText(this.clientIdDraft);
    this.clientIdDraft = '';
    if (draftTokens.length === 0) {
      return;
    }

    const merged = [...this.clientIdPills, ...draftTokens];
    this.updateClientPills(merged);
  }

  removeClientPill(pill: string, event?: Event): void {
    event?.stopPropagation();
    const next = this.clientIdPills.filter((item) => item !== pill);
    this.updateClientPills(next);
    this.clientPillRemoved.emit();
  }

  isSelectedDate(date: Date): boolean {
    if (!this.day) return false;
    return this.toIsoDate(date) === this.day;
  }

  isTodayDate(date: Date): boolean {
    return this.toIsoDate(date) === this.toIsoDate(new Date());
  }


  isCurrentDaySelected(): boolean {
    if (!this.day) {
      return false;
    }
    return this.day === this.toIsoDate(new Date());
  }

  private syncCalendarToSelectedDay(): void {
    if (!this.day) {
      return;
    }

    const selected = this.parseIsoDate(this.day);
    if (!selected) {
      return;
    }

    this.viewYear = selected.getFullYear();
    this.viewMonth = selected.getMonth();
  }

  private parseIsoDate(value: string): Date | null {
    const match = ISO_DATE_REGEX.exec(value);
    if (!match) {
      return null;
    }

    const [, year, month, day] = match;
    const parsed = new Date(Number(year), Number(month) - 1, Number(day));
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  private parseClientIdsText(raw: string): string[] {
    if (!raw) {
      return [];
    }

    const tokens = raw
      .split(/[,\s;]+/)
      .map((token) => token.trim())
      .filter((token) => /^\d+$/.test(token))
      .filter((token) => Number(token) > 0);

    return Array.from(new Set(tokens));
  }

  private updateClientPills(tokens: string[]): void {
    this.clientIdsTextChange.emit(tokens.join(', '));
  }
}
