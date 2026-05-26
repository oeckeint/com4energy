import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadComponent } from '../components/file-upload.component';

@Component({
  selector: 'app-file-upload-page',
  standalone: true,
  imports: [CommonModule, FileUploadComponent],
  template: `
    <div class="p-6">
      <h1 class="text-2xl font-bold mb-6">Carga de Archivos</h1>
      <app-file-upload />
    </div>
  `
})
export class FileUploadPage {}
