import { HttpInterceptorFn } from '@angular/common/http';

const SESSION_KEY = 'sessionId';

/**
 * Adds an `X-Session-Id` header to API calls: one id per browser tab (sessionStorage), so the backend logs can
 * follow everything a merchant did in that tab (search an address, confirm it, submit the order).
 */
export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/')) return next(request);
  return next(request.clone({ setHeaders: { 'X-Session-Id': sessionId() } }));
};

function sessionId(): string {
  try {
    let id = sessionStorage.getItem(SESSION_KEY);
    if (!id) {
      id = crypto.randomUUID();
      sessionStorage.setItem(SESSION_KEY, id);
    }
    return id;
  } catch {
    // Storage unavailable (e.g. private mode restrictions): fall back to a per-load id.
    return (fallbackId ??= crypto.randomUUID());
  }
}

let fallbackId: string | undefined;
