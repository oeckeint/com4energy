import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EnergyListComponent } from '../components/energy-list.component';
import { EnergyChartComponent } from '../components/energy-chart.component';
import { EnergyMeasurementService } from '../services/energy-measurement.service';

@Component({
  selector: 'app-metrics-page',
  standalone: true,
  imports: [CommonModule, EnergyListComponent, EnergyChartComponent],
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Medida QH</h1>
      <div class="flex items-center gap-4 mb-4">
        <button (click)="prevWindow()" style="padding:6px 10px;border-radius:4px;border:1px solid #444;background:#fff;color:#444">Anterior 24h</button>
        <button (click)="nextWindow()" style="padding:6px 10px;border-radius:4px;border:1px solid #444;background:#fff;color:#444">Siguiente 24h</button>
        <span style="font-size:14px;color:#555">
          {{ windowStart.toLocaleString('es-MX') }} — {{ windowEnd.toLocaleString('es-MX') }}
        </span>
      </div>
      <app-energy-chart />
      <div class="my-6"></div>
      <app-energy-list />
    </div>
  `
})
export class MetricsPage implements OnInit {
    measurementService = inject(EnergyMeasurementService);

    windowEnd: Date = new Date('2025-01-20T00:00:00');
    windowStart: Date = new Date('2025-01-19T00:00:00');

    ngOnInit(): void {
      this.loadWindow();
    }

    getStartOfDay(date: Date): Date {
      return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
    }

    loadWindow(): void {
      this.measurementService.fetchAllMeasurements(
        this.measurementService.currentClientId() ?? undefined,
        this.windowStart.toISOString(),
        this.windowEnd.toISOString()
      );
    }

    prevWindow(): void {
      // Move window back 24h
      const prevEnd = new Date(this.windowStart);
      const prevStart = new Date(prevEnd.getTime() - 24 * 60 * 60 * 1000);
      this.windowEnd = prevEnd;
      this.windowStart = prevStart;
      this.loadWindow();
    }

    nextWindow(): void {
      // Move window forward 24h (max: today)
      const nextStart = new Date(this.windowEnd);
      const nextEnd = new Date(nextStart.getTime() + 24 * 60 * 60 * 1000);
      const today = new Date();
      if (nextEnd > today) {
        this.windowEnd = today;
        this.windowStart = this.getStartOfDay(today);
      } else {
        this.windowEnd = nextEnd;
        this.windowStart = nextStart;
      }
      this.loadWindow();
    }

    applyRange(startStr?: string, endStr?: string) {
      if (!startStr || !endStr) return;
      const start = new Date(startStr);
      const end = new Date(endStr);
      this.windowStart = start;
      this.windowEnd = end;
      this.loadWindow();
    }

    loadLast24() {
      const now = new Date();
      const start = new Date(now.getTime() - 24 * 60 * 60 * 1000);
      this.windowStart = start;
      this.windowEnd = now;
      this.loadWindow();
    }
  }
