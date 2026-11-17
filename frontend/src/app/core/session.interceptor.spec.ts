import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { sessionInterceptor } from './session.interceptor';

describe('sessionInterceptor', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting()],
    });
  });

  it('sends the same session id on every API call of the tab', () => {
    const http = TestBed.inject(HttpClient);
    const backend = TestBed.inject(HttpTestingController);

    http.get('/api/merchants').subscribe();
    http.get('/api/rounds').subscribe();
    const [first, second] = backend.match((r) => r.url.startsWith('/api/'));

    const id = first.request.headers.get('X-Session-Id');
    expect(id).toMatch(/^[0-9a-f-]{36}$/);
    expect(second.request.headers.get('X-Session-Id')).toBe(id);
  });

  it('works on plain HTTP, where crypto.randomUUID does not exist (insecure context)', () => {
    const original = crypto.randomUUID;
    Object.defineProperty(crypto, 'randomUUID', { value: undefined, configurable: true });
    try {
      TestBed.inject(HttpClient).get('/api/merchants').subscribe();
      const request = TestBed.inject(HttpTestingController).expectOne('/api/merchants');
      expect(request.request.headers.get('X-Session-Id')).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/);
    } finally {
      Object.defineProperty(crypto, 'randomUUID', { value: original, configurable: true });
    }
  });

    it('does not add the header to non-API calls', () => {
    TestBed.inject(HttpClient).get('/assets/x.json').subscribe();
    const request = TestBed.inject(HttpTestingController).expectOne('/assets/x.json');
    expect(request.request.headers.has('X-Session-Id')).toBe(false);
  });
});
