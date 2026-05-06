import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';

interface MeasureColumnValidation {
  expected: number;
  present: number;
  missing: number;
  complete: boolean;
}

@Component({
  selector: 'app-measure-auditor-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="measure-auditor-panel">
      <div class="auditor-header">
        <h3 class="auditor-title">▼ Auditor</h3>
      </div>

      <div class="auditor-cards-wrapper">
        @if (clientIds.length === 0) {
          <div class="auditor-empty">
            <p>Sin clientes seleccionados</p>
          </div>
        } @else {
          @for (clientId of clientIds; track clientId) {
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
  `,
  styles: [`
    .measure-auditor-panel {
      background: white;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      height: 100%;
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
      margin: 0;
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .auditor-cards-wrapper {
      flex: 1;
      overflow-y: auto;
      padding: 12px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
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

    /* Scrollbar personalizado */
    .auditor-cards-wrapper::-webkit-scrollbar {
      width: 6px;
    }

    .auditor-cards-wrapper::-webkit-scrollbar-track {
      background: #f1f1f1;
      border-radius: 3px;
    }

    .auditor-cards-wrapper::-webkit-scrollbar-thumb {
      background: #ccc;
      border-radius: 3px;
    }

    .auditor-cards-wrapper::-webkit-scrollbar-thumb:hover {
      background: #999;
    }
  `]
})
export class MeasureAuditorPanelComponent {
  @Input() clientIds: number[] = [];
  @Input() columnValidation: Record<string, any> = {};
  @Input() currentDate = new Date().toISOString().slice(0, 10);

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

