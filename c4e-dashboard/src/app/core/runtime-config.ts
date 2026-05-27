export interface DashboardRuntimeConfig {
  timeZone: string;
  locale: string;
  hour12: boolean;
}

declare global {
  interface Window {
    __C4E_DASHBOARD_CONFIG__?: Partial<DashboardRuntimeConfig>;
  }
}

const DEFAULT_CONFIG: DashboardRuntimeConfig = {
  timeZone: detectDefaultTimeZone(),
  locale: detectDefaultLocale(),
  hour12: detectDefaultHour12(),
};

function detectDefaultTimeZone(): string {
  try {
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    return timeZone && timeZone.trim() ? timeZone : 'UTC';
  } catch {
    return 'UTC';
  }
}

function detectDefaultLocale(): string {
  const timeZone = detectDefaultTimeZone();
  if (timeZone.startsWith('Europe/')) {
    return 'es-ES';
  }
  if (timeZone.startsWith('America/')) {
    return 'es-MX';
  }

  try {
    const locale = globalThis.navigator?.language;
    return locale && locale.trim() ? locale : 'es-ES';
  } catch {
    return 'es-ES';
  }
}

function detectDefaultHour12(): boolean {
  const timeZone = detectDefaultTimeZone();
  if (timeZone.startsWith('Europe/')) {
    return false;
  }
  if (timeZone.startsWith('America/')) {
    return true;
  }
  return false;
}

export function getDashboardRuntimeConfig(): DashboardRuntimeConfig {
  const runtimeConfig = globalThis.window?.__C4E_DASHBOARD_CONFIG__;

  return {
    timeZone: typeof runtimeConfig?.timeZone === 'string' && runtimeConfig.timeZone.trim()
      ? runtimeConfig.timeZone
      : DEFAULT_CONFIG.timeZone,
    locale: typeof runtimeConfig?.locale === 'string' && runtimeConfig.locale.trim()
      ? runtimeConfig.locale
      : DEFAULT_CONFIG.locale,
    hour12: typeof runtimeConfig?.hour12 === 'boolean'
      ? runtimeConfig.hour12
      : DEFAULT_CONFIG.hour12,
  };
}

