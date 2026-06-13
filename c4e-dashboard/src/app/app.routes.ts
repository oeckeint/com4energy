import { Routes, provideRouter, withEnabledBlockingInitialNavigation } from '@angular/router';

export const routes: Routes = [
	{ path: '', redirectTo: 'metrics', pathMatch: 'full' },
	{ path: 'metrics', loadComponent: () => import('./features/metrics/pages/metrics.page').then(m => m.MetricsPage) },

	{ path: 'metrics/cch', loadComponent: () => import('./features/metrics/pages/medida-cch-view.page').then(m => m.MedidaCchPage) },
	{ path: 'metrics/qh', loadComponent: () => import('./features/metrics/pages/medida-qh-view.page').then(m => m.MedidaQHPage) },
	{ path: 'metrics/h', loadComponent: () => import('./features/metrics/pages/medida-h-view.page').then(m => m.MedidaHPage) },
	{ path: 'files/upload', loadComponent: () => import('./features/metrics/pages/file-upload.page').then(m => m.FileUploadPage) },
	{ path: 'files/records', loadComponent: () => import('./features/metrics/pages/file-records.page').then(m => m.FileRecordsPage) },
	{ path: 'files/events', loadComponent: () => import('./features/metrics/pages/file-record-events.page').then(m => m.FileRecordEventsPage) }
];

export const appRouterProviders = [
	provideRouter(routes, withEnabledBlockingInitialNavigation())
];
