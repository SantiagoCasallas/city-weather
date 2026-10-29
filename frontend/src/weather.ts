export interface City {
  id: number;
  name: string;
  region: string;
  country: string;
  countryCode: string;
  latitude: number;
  longitude: number;
  timezone: string;
}
export interface Forecast {
  latitude: number;
  longitude: number;
  timezone: string;
  fetchedAt: string;
  current: {
    time: string;
    temperature: number | null;
    apparentTemperature: number | null;
    humidity: number | null;
    windSpeed: number | null;
    weatherCode: number | null;
    isDay: boolean;
  };
  units: {
    temperature: string;
    windSpeed: string;
    humidity: string;
    precipitationProbability: string;
  };
  days: {
    date: string;
    min: number | null;
    max: number | null;
    weatherCode: number | null;
    precipitationProbability: number | null;
  }[];
}
export function condition(code: number | null): {
  label: string;
  icon: string;
} {
  if (code === null) return { label: "Sin información", icon: "◌" };
  if (code === 0) return { label: "Despejado", icon: "☀" };
  if (code === 1 || code === 2)
    return { label: "Parcialmente nublado", icon: "⛅" };
  if (code === 3) return { label: "Nublado", icon: "☁" };
  if ([45, 48].includes(code)) return { label: "Niebla", icon: "≋" };
  if ([51, 53, 55, 56, 57].includes(code))
    return { label: "Llovizna", icon: "☂" };
  if ([61, 63, 65, 66, 67, 80, 81, 82].includes(code))
    return { label: "Lluvia", icon: "☂" };
  if ([71, 73, 75, 77, 85, 86].includes(code))
    return { label: "Nieve", icon: "❄" };
  if ([95, 96, 99].includes(code)) return { label: "Tormenta", icon: "ϟ" };
  return { label: "Condición desconocida", icon: "◌" };
}
// Dates from the provider are city-local wall times, never browser-local instants.
export function dayLabel(date: string): string {
  return new Intl.DateTimeFormat("es", {
    weekday: "short",
    day: "numeric",
    month: "short",
    timeZone: "UTC",
  }).format(new Date(date + "T12:00:00Z"));
}

export function currentCondition(
  code: number | null,
  isDay: boolean,
): { label: string; icon: string } {
  const value = condition(code);
  if (!isDay && code === 0) return { ...value, icon: "☾" };
  if (!isDay && (code === 1 || code === 2)) return { ...value, icon: "☁" };
  return value;
}
export function utcLabel(instant: string): string {
  return new Intl.DateTimeFormat("es", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "UTC",
  }).format(new Date(instant));
}
