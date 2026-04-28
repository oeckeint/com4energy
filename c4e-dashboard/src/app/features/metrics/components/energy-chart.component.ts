import { Component, inject, ChangeDetectionStrategy, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { EnergyMeasurementService } from '../services/energy-measurement.service';

@Component({
  selector: 'app-energy-chart',
  standalone: true,
  imports: [CommonModule, BaseChartDirective],
  providers: [provideCharts(withDefaultRegisterables())],
  template: `
    <div class="p-4">
      <div class="flex items-center justify-between mb-3">
        <h3 class="text-lg font-semibold">Consumo horario (gráfico)</h3>
        @if (service.consumptionData().length > 0) {
          <div class="flex gap-4 text-sm bg-gray-50 p-2 rounded border">
            <label class="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" [checked]="showMax()" (change)="showMax.set(!showMax())" />
              <span class="font-medium text-red-600">Máximo</span>
            </label>
            <label class="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" [checked]="showMin()" (change)="showMin.set(!showMin())" />
              <span class="font-medium text-green-600">Mínimo</span>
            </label>
            <label class="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" [checked]="showAvg()" (change)="showAvg.set(!showAvg())" />
              <span class="font-medium text-orange-600">Promedio</span>
            </label>
          </div>
        }
      </div>
      <div style="height:360px" class="position-relative">
        @if (service.consumptionData().length === 0 && !service.loading() && !service.error()) {
          <div class="position-absolute top-50 start-50 translate-middle text-center w-100 p-4">
            <div class="mb-3 fs-2 opacity-50">📊</div>
            <h5 class="text-muted fw-bold">Sin datos para graficar</h5>
            <p class="text-secondary small">Selecciona un cliente o rango de fechas con mediciones.</p>
          </div>
        }
        @if (service.consumptionData().length > 0) {
          <canvas baseChart
            [data]="chartData()"
            [options]="chartOptions"
            type="line">
          </canvas>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EnergyChartComponent {
  public service = inject(EnergyMeasurementService);

  showMax = signal(false);
  showMin = signal(false);
  showAvg = signal(false);

  // chart.js options
  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: true },
      tooltip: {
        callbacks: {
          afterBody: (context) => {
            const index = context[0].dataIndex;
            const point = this.service.consumptionData()[index];
            return point ? `Archivo: ${point.origin}` : '';
          }
        }
      }
    },
    scales: {
      y: { beginAtZero: true, title: { display: true, text: 'kWh' } },
      x: { title: { display: true, text: 'Hora' } }
    }
  };

  // computed chart payload based on service.consumptionData
  chartData = computed(() => {
    const points = this.service.consumptionData();
    const datasets: any[] = [
      {
        label: 'Consumo (kWh)',
        data: points.map(p => p.consumption),
        borderColor: '#1976d2',
        backgroundColor: 'rgba(25,118,210,0.1)',
        tension: 0.3,
        fill: true
      }
    ];

    if (this.showMax() && points.length > 0) {
      const max = this.service.maxConsumption();
      datasets.push({
        label: `Máximo (${max.toFixed(2)})`,
        data: points.map(() => max),
        borderColor: '#d32f2f',
        borderDash: [5, 5],
        pointRadius: 0,
        fill: false,
        borderWidth: 2
      });
    }

    if (this.showMin() && points.length > 0) {
      const min = this.service.minConsumption();
      datasets.push({
        label: `Mínimo (${min.toFixed(2)})`,
        data: points.map(() => min),
        borderColor: '#388e3c',
        borderDash: [5, 5],
        pointRadius: 0,
        fill: false,
        borderWidth: 2
      });
    }

    if (this.showAvg() && points.length > 0) {
      const avg = this.service.avgConsumption();
      datasets.push({
        label: `Promedio (${avg.toFixed(2)})`,
        data: points.map(() => avg),
        borderColor: '#f57c00',
        borderDash: [2, 2],
        pointRadius: 0,
        fill: false,
        borderWidth: 2
      });
    }

    return {
      labels: points.map(p => p.hour),
      datasets
    };
  });
}
