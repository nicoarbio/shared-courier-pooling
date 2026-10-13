import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { Api } from '../core/api';
import { ApiError, toApiError } from '../core/api-error';
import { Merchant } from '../core/api.models';
import { MerchantSession } from '../core/merchant-session';
import { TranslatePipe } from '../i18n/translate';
import { ErrorMessage } from '../shared/error-message';

/** "Who are you?": picks the merchant to act as (no login). */
@Component({
  selector: 'app-select-merchant',
  imports: [MatButtonModule, TranslatePipe, ErrorMessage],
  template: `
    <h1>{{ 'merchant.select.title' | t }}</h1>
    @if (error(); as e) {
      <app-error-message [error]="e" />
    }
    <ul>
      @for (merchant of merchants(); track merchant.id) {
        <li>
          <button mat-stroked-button type="button" (click)="choose(merchant)">{{ merchant.name }}</button>
        </li>
      } @empty {
        @if (!error()) {
          <li>{{ 'common.loading' | t }}</li>
        }
      }
    </ul>
  `,
  styles: `
    ul {
      list-style: none;
      padding: 0;
    }
    li {
      margin-block: 0.5rem;
    }
    button {
      width: 100%;
    }
  `,
})
export class SelectMerchant {
  private readonly api = inject(Api);
  private readonly session = inject(MerchantSession);
  private readonly router = inject(Router);

  protected readonly merchants = signal<Merchant[]>([]);
  protected readonly error = signal<ApiError | null>(null);

  constructor() {
    this.api.merchants().subscribe({
      next: (list) => this.merchants.set(list),
      error: (e: unknown) => this.error.set(toApiError(e)),
    });
  }

  protected choose(merchant: Merchant): void {
    this.session.select(merchant);
    void this.router.navigate(['/comercio', merchant.id, 'nuevo']);
  }
}
