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
      id = randomId();
      sessionStorage.setItem(SESSION_KEY, id);
    }
    return id;
  } catch {
    // Storage unavailable (e.g. private mode restrictions): fall back to a per-load id.
    return (fallbackId ??= randomId());
  }
}

/**
 * A random UUID (v4). crypto.randomUUID() only exists in secure contexts (HTTPS or localhost), so on a plain-HTTP
 * deployment it is undefined; crypto.getRandomValues() is available everywhere.
 */
export function randomId(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40; // version 4
  bytes[8] = (bytes[8] & 0x3f) | 0x80; // RFC 4122 variant
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

let fallbackId: string | undefined;
