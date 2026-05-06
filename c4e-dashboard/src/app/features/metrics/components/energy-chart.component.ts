import { Component, inject, ChangeDetectionStrategy, computed, signal, ViewChild, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { EnergyMeasurementService } from '../services/energy-measurement.service';

// Formatea números con formato europeo: miles (.) y redondea hacia arriba (Math.ceil)
const formatEnergyValue = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '-';
  return Math.ceil(value).toLocaleString('es-ES');
};

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
           <canvas #chartRef baseChart
             [data]="chartData()"
             [options]="chartOptions()"
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

  @ViewChild(BaseChartDirective) chartComponent?: BaseChartDirective;

  showMax = signal(false);
  showMin = signal(false);
  showAvg = signal(false);

  // Calcular opciones del gráfico dinámicamente (COMPUTED para OnPush)
  chartOptions = computed((): ChartConfiguration<'line'>['options'] => {
    const points = this.service.consumptionData();
    let maxValue = 0;

    if (points.length > 0) {
      // Encontrar el máximo valor entre consumo, máx, mín, promedio
      maxValue = Math.max(
        this.service.maxConsumption() || 0,
        ...points.map(p => p.consumption || 0)
      );
    }

    // Calcular máximo del eje Y con 20% de padding para mejor vista
    const yMax = maxValue > 0 ? Math.ceil(maxValue * 1.2) : 100;

    return {
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
        y: {
          beginAtZero: true,
          max: yMax,
          title: { display: true, text: 'kWh' }
        },
        x: { title: { display: true, text: 'Hora' } }
      }
    };
  });

  constructor() {
    // Effect para actualizar el gráfico cuando cambien opciones o datos
    effect(() => {
      // Accesar a ambos para que el effect se reactive a cambios
      this.chartOptions();
      this.chartData();

      // Pequeño delay para asegurar que el canvas está renderizado
      setTimeout(() => {
        const chart = this.chartComponent?.chart;
        if (chart) {
          const options = this.chartOptions();
          const yScale = options?.scales?.['y'];
          if (yScale) {
            chart.options.scales = chart.options.scales ?? {};
            (chart.options.scales as any)['y'] = {
              ...((chart.options.scales as any)['y'] ?? {}),
              ...yScale
            };
          }
          chart.update('active');
        }
      }, 0);
    });
  }

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
        label: `Máximo (${formatEnergyValue(max)})`,
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
        label: `Mínimo (${formatEnergyValue(min)})`,
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
        label: `Promedio (${formatEnergyValue(avg)})`,
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
