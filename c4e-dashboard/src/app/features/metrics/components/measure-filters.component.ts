import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-measure-filters',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="card border-0 shadow-sm rounded-4 mb-4">
      <div class="card-body">
        <div class="row g-3 align-items-end">
          <div class="col-md-3">
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
                [ngModel]="day"
                (ngModelChange)="dayChange.emit($event)"
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

          <div class="col-md-6">
            <label class="form-label">Clientes</label>
            <input
              class="form-control"
              [ngModel]="clientIdsText"
              (ngModelChange)="clientIdsTextChange.emit($event)"
              [placeholder]="clientPlaceholder"
            />
            <small class="text-muted">{{ helperText }}</small>
          </div>

          <div class="col-md-3 d-flex gap-2">
            <button class="btn btn-primary" (click)="search.emit()" [disabled]="loading">Buscar</button>
            <button class="btn btn-outline-secondary" (click)="clear.emit()" [disabled]="loading">Limpiar</button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class MeasureFiltersComponent {
  @Input() day = '';
  @Input() clientIdsText = '';
  @Input() loading = false;
  @Input() clientPlaceholder = '34133, 55221';
  @Input() helperText = 'Opcional. Separar IDs por coma; vacio = todos los clientes.';

  @Output() dayChange = new EventEmitter<string>();
  @Output() clientIdsTextChange = new EventEmitter<string>();
  @Output() search = new EventEmitter<void>();
  @Output() clear = new EventEmitter<void>();

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
}

