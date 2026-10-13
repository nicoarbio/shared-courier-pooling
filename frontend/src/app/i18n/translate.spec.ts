import { TestBed } from '@angular/core/testing';
import { Translations } from './translate';

describe('Translations', () => {
  const t = () => TestBed.inject(Translations);

  it('resolves a nested key', () => {
    expect(t().t('merchant.orders.title')).toBe('Mis pedidos');
  });

  it('replaces placeholders with params', () => {
    expect(t().t('courier.rounds.summary', { deliveries: 5, km: '3,7', time: '19:00' })).toBe('5 entregas · 3,7 km · sale 19:00');
  });

  it('keeps a placeholder when its param is missing', () => {
    expect(t().t('errors.requestId')).toBe('Código de error: {id}');
  });
});
