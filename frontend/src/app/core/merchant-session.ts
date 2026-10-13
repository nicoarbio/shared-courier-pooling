import { Service, signal } from '@angular/core';
import { Merchant } from './api.models';

const STORAGE_KEY = 'merchant';

/** The merchant the user chose to act as (there is no login). Remembered in the browser across visits. */
@Service()
export class MerchantSession {
  private readonly selected = signal<Merchant | null>(load());

  readonly merchant = this.selected.asReadonly();

  select(merchant: Merchant): void {
    this.selected.set(merchant);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(merchant));
    } catch {
      // Storage unavailable: the choice lasts until the page is reloaded.
    }
  }
}

function load(): Merchant | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as Merchant) : null;
  } catch {
    return null;
  }
}
