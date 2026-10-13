import { HttpErrorResponse } from '@angular/common/http';

/** What the UI shows when a call fails: a message key and, when available, the request id to look up in the logs. */
export interface ApiError {
  key: 'errors.network' | 'errors.notFound' | 'errors.conflict' | 'errors.invalid' | 'errors.unavailable' | 'errors.unexpected';
  requestId: string | null;
}

export function toApiError(error: unknown): ApiError {
  if (!(error instanceof HttpErrorResponse)) return { key: 'errors.unexpected', requestId: null };
  const requestId = error.headers?.get('X-Request-Id') ?? null;
  switch (true) {
    case error.status === 0:
      return { key: 'errors.network', requestId };
    case error.status === 400:
      return { key: 'errors.invalid', requestId };
    case error.status === 404:
      return { key: 'errors.notFound', requestId };
    case error.status === 409:
      return { key: 'errors.conflict', requestId };
    case error.status === 502 || error.status === 503:
      return { key: 'errors.unavailable', requestId };
    default:
      return { key: 'errors.unexpected', requestId };
  }
}
