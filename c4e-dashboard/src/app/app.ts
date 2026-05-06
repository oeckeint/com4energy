import { Component, signal, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { EnergyMeasurementService } from './features/metrics/services/energy-measurement.service';
import { FileProcessingNotificationsService } from './features/metrics/services/file-processing-notifications.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('c4e-dashboard');
  protected readonly measurementsService = inject(EnergyMeasurementService);
  protected readonly notificationsService = inject(FileProcessingNotificationsService);

  // Estados para los dropdowns
  showVistasDropdown = signal(false);
  showArchivosDropdown = signal(false);

  constructor() {
    // Cargar por defecto el cliente 3215 al iniciar la aplicación
    this.selectClient('3215');
    this.notificationsService.connect();
  }

  selectClient(clientId: string) {
    const id = Number(clientId);
    if (!Number.isNaN(id)) {
      this.measurementsService.fetchMeasurements(0, 20, id);
    }
  }

  applyRange(startStr?: string, endStr?: string) {
    if (!startStr || !endStr) return;
    const start = new Date(startStr).toISOString();
    const end = new Date(endStr).toISOString();
    this.measurementsService.fetchAllMeasurements(this.measurementsService.currentClientId() ?? undefined, start, end);
  }

  loadLast24() {
    this.measurementsService.fetchLast24Hours(this.measurementsService.currentClientId() ?? undefined);
  }
}
