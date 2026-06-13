import { getDashboardRuntimeConfig } from './runtime-config';

export function formatDashboardDateTime(
  value: string | Date | null | undefined,
  options: Intl.DateTimeFormatOptions = {},
): string {
  if (!value) {
    return 'Ahora';
  }

  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) {
    return 'Ahora';
  }

  const config = getDashboardRuntimeConfig();

  return date.toLocaleString(config.locale, {
    timeZone: config.timeZone,
    hour12: config.hour12,
    ...options,
  });
}

