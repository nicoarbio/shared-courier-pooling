/*
 * The backend exchanges city wall-clock date-times without offset ("2026-10-13T19:00:00"); the browser is assumed
 * to be in the same city. These helpers convert between Date and the formats used by the API and by
 * <input type="datetime-local"> ("2026-10-13T19:00").
 */

const pad = (n: number) => String(n).padStart(2, '0');

/** "YYYY-MM-DDTHH:mm" in local time, as datetime-local inputs expect. */
export function toInputValue(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** datetime-local value → API wall-clock date-time. */
export function toApiDateTime(inputValue: string): string {
  return inputValue.length === 16 ? `${inputValue}:00` : inputValue;
}

/** Default delivery window: from the next multiple of 5 minutes, lasting 30 minutes. */
export function defaultWindow(now: Date): { start: string; end: string } {
  const start = new Date(now);
  start.setSeconds(0, 0);
  start.setMinutes(Math.ceil((start.getMinutes() + 1) / 5) * 5);
  const end = new Date(start.getTime() + 30 * 60_000);
  return { start: toInputValue(start), end: toInputValue(end) };
}
