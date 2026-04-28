import { Routes } from '@angular/router';
import { provideRouter, withEnabledBlockingInitialNavigation } from '@angular/router';

export const routes: Routes = [
	{ path: '', redirectTo: 'metrics', pathMatch: 'full' },
	{ path: 'metrics', loadComponent: () => import('./features/metrics/pages/metrics.page').then(m => m.MetricsPage) }
];

export const appRouterProviders = [
	provideRouter(routes, withEnabledBlockingInitialNavigation())
];
