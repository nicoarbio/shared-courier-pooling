import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject, input } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Api } from '../core/api';
import { Order } from '../core/api.models';
import { toApiError } from '../core/api-error';
import { reloadEvery } from '../core/polling';
import { TranslatePipe, TranslationKey } from '../i18n/translate';
import { ErrorMessage } from '../shared/error-message';
import { MerchantNav } from './merchant-nav';

/** A merchant's orders: state, round, the merchant's share and the estimated delivery. Refreshes on its own. */
@Component({
  selector: 'app-my-orders',
  imports: [MatButtonModule, MatCardModule, CurrencyPipe, DatePipe, TranslatePipe, ErrorMessage, MerchantNav],
  template: `
    <app-merchant-nav />
    <header>
      <h1>{{ 'merchant.orders.title' | t }}</h1>
      <button mat-button type="button" (click)="orders.reload()">{{ 'common.refresh' | t }}</button>
    </header>
    @if (error(); as e) {
      <app-error-message [error]="e" />
    }
    @for (order of orders.value(); track order.id) {
      <mat-card appearance="outlined">
        <mat-card-header>
          <mat-card-title>{{ order.address }}</mat-card-title>
          <mat-card-subtitle>{{ statusKey(order) | t }}</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p>{{ 'merchant.orders.window' | t: { start: (order.windowStart | date: 'dd/MM HH:mm') ?? '', end: (order.windowEnd | date: 'HH:mm') ?? '' } }}</p>
          @if (order.round; as round) {
            <p>{{ 'merchant.orders.estimatedDelivery' | t: { time: (round.estimatedDelivery | date: 'HH:mm') ?? '' } }}</p>
            <p>{{ 'merchant.orders.share' | t: { amount: (round.merchantShare | currency: 'ARS' : 'symbol-narrow') ?? '' } }}</p>
          }
        </mat-card-content>
      </mat-card>
    } @empty {
      @if (!orders.isLoading() && !error()) {
        <p>{{ 'merchant.orders.empty' | t }}</p>
      }
    }
  `,
  styles: `
    header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    mat-card {
      margin-block: 0.75rem;
    }
  `,
})
export class MyOrders {
  /** Route param. */
  readonly merchantId = input.required<string>();

  private readonly api = inject(Api);

  protected readonly orders = rxResource({
    params: () => this.merchantId(),
    stream: ({ params }) => this.api.merchantOrders(params),
    defaultValue: [],
  });
  protected readonly error = computed(() => (this.orders.error() ? toApiError(this.orders.error()) : null));

  protected readonly statusKey = (order: Order): TranslationKey => `merchant.orders.status.${order.status}`;

  constructor() {
    reloadEvery(this.orders);
  }
}
