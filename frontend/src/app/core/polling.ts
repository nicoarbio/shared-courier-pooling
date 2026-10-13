import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

/** How often lists refresh on their own: a new order elsewhere can re-plan rounds and change costs and times. */
export const REFRESH_INTERVAL_MS = 30_000;

/** Reloads a resource periodically while the calling component is alive. Call it in an injection context. */
export function reloadEvery(resource: { reload(): unknown }, ms = REFRESH_INTERVAL_MS): void {
  interval(ms)
    .pipe(takeUntilDestroyed())
    .subscribe(() => resource.reload());
}
