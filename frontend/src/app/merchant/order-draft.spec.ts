import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Api } from '../core/api';
import { GeocodeCandidate, Merchant, Order } from '../core/api.models';
import { OrderDraft } from './order-draft';

const merchant: Merchant = { id: 'm1', name: 'Kiosco', location: { lon: -58.44415, lat: -34.62025 } };
const yerbal: GeocodeCandidate = {
  label: 'Yerbal 1550, Caballito',
  street: 'Yerbal',
  houseNumber: '1550',
  neighbourhood: 'Caballito',
  location: { lon: -58.45229, lat: -34.62366 },
};

describe('OrderDraft', () => {
  const api = {
    geocode: vi.fn(() => of([yerbal])),
    createOrder: vi.fn(() => of({ id: 'o1' } as Order)),
  };

  beforeEach(() => {
    vi.useFakeTimers();
    api.geocode.mockClear();
    api.createOrder.mockClear();
    TestBed.configureTestingModule({ providers: [OrderDraft, { provide: Api, useValue: api }] });
  });

  afterEach(() => vi.useRealTimers());

  const draft = () => {
    const d = TestBed.inject(OrderDraft);
    d.merchant.set(merchant);
    return d;
  };

  it('searches once the user stops typing, and not for very short text', async () => {
    const d = draft();
    d.search(); // subscribe
    d.query.set('Ye');
    TestBed.tick();
    await vi.advanceTimersByTimeAsync(500);
    expect(api.geocode).not.toHaveBeenCalled();

    d.query.set('Yerbal 15');
    TestBed.tick();
    d.query.set('Yerbal 1550');
    TestBed.tick();
    await vi.advanceTimersByTimeAsync(500);
    expect(api.geocode).toHaveBeenCalledTimes(1);
    expect(api.geocode).toHaveBeenCalledWith('Yerbal 1550', 'm1');
    expect(d.candidates()).toEqual([yerbal]);
  });

  it('flags a candidate without house number so the user adjusts it on the map', () => {
    const d = draft();
    d.choose({ ...yerbal, houseNumber: null });
    expect(d.missingHouseNumber()).toBe(true);
    d.move({ lon: -58.45, lat: -34.62 });
    expect(d.destination()).toEqual({ lon: -58.45, lat: -34.62 });
  });

  it('only submits with a point and a valid window, sending wall-clock times', () => {
    const d = draft();
    d.window.set({ start: '2026-10-13T19:00', end: '2026-10-13T19:30' });
    expect(d.canSubmit()).toBe(false); // no point yet

    d.choose(yerbal);
    d.window.set({ start: '2026-10-13T19:30', end: '2026-10-13T19:00' });
    expect(d.canSubmit()).toBe(false); // end before start

    d.window.set({ start: '2026-10-13T19:00', end: '2026-10-13T19:30' });
    expect(d.canSubmit()).toBe(true);
    d.submit();
    expect(api.createOrder).toHaveBeenCalledWith({
      merchantId: 'm1',
      destination: yerbal.location,
      address: 'Yerbal 1550, Caballito',
      windowStart: '2026-10-13T19:00:00',
      windowEnd: '2026-10-13T19:30:00',
    });
    expect(d.submitted()?.id).toBe('o1');
  });
});
