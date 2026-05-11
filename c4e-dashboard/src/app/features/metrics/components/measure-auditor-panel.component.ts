import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges } from '@angular/core';

interface MeasureColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

type ClientStatusSubmodule = 'defectuosos' | 'adecuados';

@Component({
  selector: 'app-measure-auditor-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="measure-auditor-panel">
      <div class="auditor-header">
        <button
          type="button"
          class="auditor-toggle"
          (click)="toggleCollapsed()"
          [attr.aria-expanded]="!collapsed"
          [attr.aria-label]="collapsed ? 'Expandir estado de clientes' : 'Retraer estado de clientes'"
          [attr.title]="collapsed ? 'Expandir estado de clientes' : 'Retraer estado de clientes'"
        >
          <span class="auditor-toggle-icon" aria-hidden="true">{{ collapsed ? '▶' : '▼' }}</span>
          <span class="auditor-title">Estado de clientes</span>
        </button>
      </div>

      <!-- Wrapper de colapso: grid 0fr↔1fr para transición CSS suave sin JS -->
      <div class="auditor-collapsible" [class.auditor-collapsible--collapsed]="collapsed" [attr.aria-hidden]="collapsed">
        <div class="auditor-collapsible-inner">
          <div class="auditor-submodules" role="tablist" aria-label="Submodulos de estado de clientes">
            <button
              type="button"
              role="tab"
              class="auditor-submodule-btn"
              [class.auditor-submodule-btn--active]="activeSubmodule === 'defectuosos'"
              [attr.aria-selected]="activeSubmodule === 'defectuosos'"
              (click)="setSubmodule('defectuosos')"
            >
              Defectuosos ({{ defectiveCount }})
            </button>
            <button
              type="button"
              role="tab"
              class="auditor-submodule-btn"
              [class.auditor-submodule-btn--active]="activeSubmodule === 'adecuados'"
              [attr.aria-selected]="activeSubmodule === 'adecuados'"
              (click)="setSubmodule('adecuados')"
            >
              Adecuados ({{ adequateCount }})
            </button>
          </div>

          <div class="auditor-cards-wrapper" [class.auditor-cards-wrapper--scrolling]="showCardsScrollbar" (scroll)="onCardsScroll()">
            @if (visibleClientIds.length === 0) {
              <div class="auditor-empty">
                <p>{{ emptySubmoduleMessage }}</p>
              </div>
            } @else {
              @for (clientId of visibleClientIds; track clientId) {
                <div class="auditor-card"
                  [class.auditor-card-complete]="isComplete(clientId)"
                  [class.auditor-card-failed]="!isComplete(clientId)">
                  <div class="auditor-card-header">
                    <span class="auditor-client-label">Cliente {{ clientId }}</span>
                  </div>
                  <div class="auditor-card-date">{{ formattedDate }}</div>
                  <div class="auditor-card-status">
                    @if (isComplete(clientId)) {
                      <span class="status-complete">✓ Completo</span>
                    } @else {
                      <span class="status-failed">⚠ Faltan {{ getMissingCount(clientId) }}</span>
                    }
                  </div>
                </div>
              }
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .measure-auditor-panel {
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      width: 100%;
      box-sizing: border-box;
    }

    .auditor-header {
      background: #f5f5f5;
      padding: 12px 16px;
      border-bottom: 1px solid #e0e0e0;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .auditor-title {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .auditor-toggle {
      border: 0;
      background: transparent;
      padding: 0;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      color: inherit;
      cursor: pointer;
    }

    .auditor-toggle:hover .auditor-title,
    .auditor-toggle:focus-visible .auditor-title {
      color: #0f172a;
      text-decoration: underline;
      text-underline-offset: 2px;
    }

    .auditor-toggle:focus-visible {
      outline: 2px solid #93c5fd;
      outline-offset: 2px;
      border-radius: 4px;
    }

    .auditor-toggle-icon {
      font-size: 15px;
      line-height: 1;
      color: #374151;
    }

    /* ── Colapso suave: grid 0fr ↔ 1fr ─────────────────────── */
    .auditor-collapsible {
      display: grid;
      grid-template-rows: 1fr;
      transition: grid-template-rows 0.26s ease;
    }

    .auditor-collapsible--collapsed {
      grid-template-rows: 0fr;
    }

    /* El inner es el hijo directo que necesita overflow:hidden para el truco grid */
    .auditor-collapsible-inner {
      overflow: hidden;
      min-height: 0;
      display: flex;
      flex-direction: column;
    }
    /* ────────────────────────────────────────────────────────── */

    .auditor-submodules {
      display: flex;
      gap: 8px;
      padding: 10px 12px 0;
    }

    .auditor-submodule-btn {
      border: 1px solid #cbd5e1;
      border-radius: 999px;
      background: #ffffff;
      color: #475569;
      font-size: 12px;
      font-weight: 600;
      padding: 5px 10px;
      line-height: 1.2;
      cursor: pointer;
    }

    .auditor-submodule-btn--active {
      border-color: #93c5fd;
      background: #eff6ff;
      color: #1d4ed8;
    }

    .auditor-cards-wrapper {
      overflow-y: auto;
      max-height: 360px;
      padding: 12px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      scrollbar-width: none;
      -ms-overflow-style: none;
    }

    .auditor-cards-wrapper--scrolling {
      scrollbar-width: thin;
      scrollbar-color: #cbd5e1 transparent;
    }

    .auditor-empty {
      grid-column: 1 / -1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;
      color: #999;
      font-size: 12px;
      text-align: center;
    }

    .auditor-card {
      border: 1px solid #ddd;
      border-radius: 6px;
      padding: 10px;
      font-size: 12px;
      transition: all 0.2s ease;
    }

    .auditor-card-complete {
      background: #f0fdf4;
      border-color: #86efac;
    }

    .auditor-card-failed {
      background: #fef2f2;
      border-color: #fca5a5;
    }

    .auditor-card:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .auditor-card-header {
      font-weight: 600;
      color: #333;
      margin-bottom: 4px;
    }

    .auditor-client-label {
      display: block;
      word-wrap: break-word;
    }

    .auditor-card-date {
      color: #666;
      font-size: 11px;
      margin-bottom: 6px;
    }

    .auditor-card-status {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .status-complete {
      color: #16a34a;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .status-failed {
      color: #dc2626;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .auditor-cards-wrapper::-webkit-scrollbar {
      width: 8px;
    }

    .auditor-cards-wrapper::-webkit-scrollbar-track {
      background: transparent;
    }

    .auditor-cards-wrapper::-webkit-scrollbar-thumb {
      background: transparent;
      border-radius: 999px;
    }

    .auditor-cards-wrapper--scrolling::-webkit-scrollbar-thumb {
      background: #cbd5e1;
    }
  `]
})
export class MeasureAuditorPanelComponent implements OnDestroy, OnChanges {
  private readonly scrollbarHideDelayMs = 900;
  private cardsScrollbarTimer: ReturnType<typeof setTimeout> | null = null;
  @Input() clientIds: number[] = [];
  @Input() columnValidation: Record<string, MeasureColumnValidation> = {};
  @Input() currentDate = new Date().toISOString().slice(0, 10);
  @Input() focusDefectuososTrigger = 0;
  collapsed = false;
  activeSubmodule: ClientStatusSubmodule = 'defectuosos';
  showCardsScrollbar = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['focusDefectuososTrigger']) {
      return;
    }
    this.openDefectuososView();
  }

  ngOnDestroy(): void {
    if (this.cardsScrollbarTimer !== null) {
      clearTimeout(this.cardsScrollbarTimer);
      this.cardsScrollbarTimer = null;
    }
  }

  toggleCollapsed(): void {
    this.collapsed = !this.collapsed;
  }

  setSubmodule(submodule: ClientStatusSubmodule): void {
    this.activeSubmodule = submodule;
  }

  private openDefectuososView(): void {
    this.collapsed = false;
    this.activeSubmodule = 'defectuosos';
  }

  onCardsScroll(): void {
    this.showCardsScrollbar = true;
    if (this.cardsScrollbarTimer !== null) {
      clearTimeout(this.cardsScrollbarTimer);
    }
    this.cardsScrollbarTimer = setTimeout(() => {
      this.showCardsScrollbar = false;
      this.cardsScrollbarTimer = null;
    }, this.scrollbarHideDelayMs);
  }

  get defectiveCount(): number {
    return this.clientIds.filter((clientId) => !this.isComplete(clientId)).length;
  }

  get adequateCount(): number {
    return this.clientIds.filter((clientId) => this.isComplete(clientId)).length;
  }

  get visibleClientIds(): number[] {
    if (this.activeSubmodule === 'defectuosos') {
      return this.clientIds.filter((clientId) => !this.isComplete(clientId));
    }
    return this.clientIds.filter((clientId) => this.isComplete(clientId));
  }

  get emptySubmoduleMessage(): string {
    return this.activeSubmodule === 'defectuosos'
      ? 'No hay clientes defectuosos.'
      : 'No hay clientes adecuados.';
  }

  get formattedDate(): string {
    const [year, month, day] = this.currentDate.split('-');
    return `${day}/${month}/${year}`;
  }

  isComplete(clientId: number): boolean {
    const validation = this.columnValidation[String(clientId)];
    return validation?.missing === 0 || validation?.missing === undefined;
  }

  getMissingCount(clientId: number): number {
    return this.columnValidation[String(clientId)]?.missing ?? 0;
  }
}
