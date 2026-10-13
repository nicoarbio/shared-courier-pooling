import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, effect, inject, input } from '@angular/core';
import { FormField } from '@angular/forms/signals';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { GeocodeCandidate } from '../core/api.models';
import { MerchantSession } from '../core/merchant-session';
import { TranslatePipe } from '../i18n/translate';
import { ErrorMessage } from '../shared/error-message';
import { MapMarker, MapView } from '../shared/map';
import { MerchantNav } from './merchant-nav';
import { OrderDraft } from './order-draft';

/** New order: address (search + map confirmation) and delivery window. All behaviour lives in OrderDraft. */
@Component({
  selector: 'app-new-order',
  providers: [OrderDraft],
  imports: [
    FormField,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    CurrencyPipe,
    DatePipe,
    TranslatePipe,
    ErrorMessage,
    MapView,
    MerchantNav,
  ],
  template: `
    <app-merchant-nav />
    <h1>{{ 'merchant.newOrder.title' | t }}</h1>

    @if (draft.submitted(); as order) {
      <section class="done" role="status">
        <h2>{{ 'merchant.newOrder.done' | t }}</h2>
        @if (order.round; as round) {
          <p>{{ 'merchant.newOrder.doneRound' | t: { time: (round.estimatedDelivery | date: 'HH:mm') ?? '' } }}</p>
          <p>{{ 'merchant.newOrder.doneShare' | t: { amount: (round.merchantShare | currency: 'ARS' : 'symbol-narrow') ?? '' } }}</p>
        }
        <button mat-flat-button type="button" (click)="draft.reset()">{{ 'merchant.newOrder.another' | t }}</button>
      </section>
    } @else {
      <form (submit)="$event.preventDefault(); draft.submit()">
        <mat-form-field appearance="outline">
          <mat-label>{{ 'merchant.newOrder.addressLabel' | t }}</mat-label>
          <input
            matInput
            autocomplete="off"
            [value]="draft.query()"
            (input)="draft.query.set($any($event.target).value)"
            [matAutocomplete]="suggestions"
          />
          <mat-hint>{{ 'merchant.newOrder.addressHint' | t }}</mat-hint>
          <mat-autocomplete #suggestions [displayWith]="label" (optionSelected)="draft.choose($event.option.value)">
            @for (candidate of draft.candidates(); track $index) {
              <mat-option [value]="candidate">{{ candidate.label }}</mat-option>
            }
          </mat-autocomplete>
        </mat-form-field>

        <p class="status" aria-live="polite">
          @switch (draft.search().status) {
            @case ('searching') {
              {{ 'merchant.newOrder.searching' | t }}
            }
            @case ('done') {
              @if (draft.candidates().length === 0) {
                {{ 'merchant.newOrder.noResults' | t }}
              }
            }
          }
          @if (draft.missingHouseNumber()) {
            {{ 'merchant.newOrder.noHouseNumber' | t }}
          }
        </p>
        @if (searchError(); as e) {
          <app-error-message [error]="e" />
        }

        @if (draft.destination()) {
          <p>{{ 'merchant.newOrder.mapHint' | t }}</p>
          <app-map [markers]="markers()" [ariaLabel]="'merchant.newOrder.addressLabel' | t" (moved)="draft.move($event)" />
        }

        <div class="window">
          <mat-form-field appearance="outline">
            <mat-label>{{ 'merchant.newOrder.windowStart' | t }}</mat-label>
            <input matInput type="datetime-local" [formField]="draft.windowForm.start" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>{{ 'merchant.newOrder.windowEnd' | t }}</mat-label>
            <input matInput type="datetime-local" [formField]="draft.windowForm.end" />
          </mat-form-field>
        </div>
        @if (draft.windowForm.end().invalid()) {
          <p class="invalid" role="alert">{{ 'merchant.newOrder.windowInvalid' | t }}</p>
        }

        @if (draft.error(); as e) {
          <app-error-message [error]="e" />
        }
        <button mat-flat-button type="submit" [disabled]="!draft.canSubmit()">
          {{ (draft.submitting() ? 'merchant.newOrder.submitting' : 'merchant.newOrder.submit') | t }}
        </button>
      </form>
    }
  `,
  styles: `
    mat-form-field {
      width: 100%;
    }
    .window {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;
      margin-top: 1rem;
    }
    .window mat-form-field {
      flex: 1 1 12rem;
    }
    .status {
      min-height: 1.25rem;
    }
    .invalid {
      color: var(--mat-sys-error);
    }
    button[type='submit'] {
      width: 100%;
    }
  `,
})
export class NewOrder {
  /** Route param. */
  readonly merchantId = input.required<string>();

  protected readonly draft = inject(OrderDraft);
  private readonly session = inject(MerchantSession);
  private readonly router = inject(Router);

  protected readonly searchError = computed(() => {
    const s = this.draft.search();
    return s.status === 'failed' ? s.error : null;
  });

  protected readonly markers = computed<MapMarker[]>(() => {
    const merchant = this.draft.merchant();
    const destination = this.draft.destination();
    return [
      ...(merchant ? [{ location: merchant.location, label: '🏪', title: merchant.name, kind: 'shop' as const }] : []),
      ...(destination ? [{ location: destination, label: '📦', title: this.draft.chosen()?.label ?? '', draggable: true }] : []),
    ];
  });

  protected readonly label = (candidate: GeocodeCandidate | string | null) =>
    typeof candidate === 'string' ? candidate : (candidate?.label ?? '');

  constructor() {
    // The page acts for the merchant chosen in this browser; a link for another merchant goes back to the picker.
    effect(() => {
      const merchant = this.session.merchant();
      if (!merchant || merchant.id !== this.merchantId()) {
        void this.router.navigate(['/comercio']);
      } else {
        this.draft.merchant.set(merchant);
      }
    });
  }
}
