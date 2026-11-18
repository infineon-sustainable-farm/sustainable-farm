import { vi } from 'vitest';

/**
 * Puts the clock on 1 April 2026 at 00:30 in Berlin, when the UTC day is still 31 March: a date
 * computed with Date#toISOString shows the day before. Only Date is faked, so waitFor still runs.
 * Returns the function that restores the clock and the time zone.
 */
export function justAfterLocalMidnight() {
  // Node reads TZ again when it changes; the browser globals of the lint have no process.
  const { env } = globalThis.process;
  const previousTz = env.TZ;
  env.TZ = 'Europe/Berlin';
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date(2026, 3, 1, 0, 30));
  return () => {
    vi.useRealTimers();
    if (previousTz === undefined) delete env.TZ;
    else env.TZ = previousTz;
  };
}
