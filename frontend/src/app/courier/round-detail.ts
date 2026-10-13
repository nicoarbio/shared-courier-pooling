import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { ApiError, toApiError } from '../core/api-error';
import { Round, Stop } from '../core/api.models';
import { TranslatePipe, TranslationKey } from '../i18n/translate';
import { ErrorMessage } from '../shared/error-message';
import { MapMarker, MapView } from '../shared/map';

/** A round for the courier: ordered stops with times, the map, navigation in Google Maps and "leave". */
@Component({
  selector: 'app-round-detail',
  imports: [RouterLink, MatButtonModule, CurrencyPipe, DatePipe, DecimalPipe, TranslatePipe, ErrorMessage, MapView],
  template: `
    <a mat-button routerLink="/repartidor">← {{ 'courier.round.back' | t }}</a>
    @if (loadError(); as e) {
      <app-error-message [error]="e" />
    }
    @if (round.value(); as r) {
      <h1>{{ 'courier.round.title' | t }}</h1>
      <p>
        {{
          'courier.round.summary'
            | t
              : {
                  km: (r.distanceMeters / 1000 | number: '1.1-1') ?? '',
                  departure: (r.departureAt | date: 'HH:mm') ?? '',
                  return: (r.estimatedReturn | date: 'HH:mm') ?? '',
                }
        }}
      </p>
      <p>{{ 'courier.round.cost' | t: { amount: (r.cost | currency: 'ARS' : 'symbol-narrow') ?? '' } }}</p>

      <app-map [markers]="markers()" [ariaLabel]="'courier.round.stops' | t" />

      <h2>{{ 'courier.round.stops' | t }}</h2>
      <ol>
        @for (stop of r.stops; track stop.seq) {
          <li>
            <strong>{{ stop.estimatedArrival | date: 'HH:mm' }}</strong>
            {{ stopKindKey(stop) | t }}
            {{ stop.kind === 'DELIVERY' ? stop.address : stop.merchantName }}
          </li>
        }
      </ol>

      <h2>{{ 'courier.round.shares' | t }}</h2>
      <ul>
        @for (share of r.shares; track share.merchantId) {
          <li>{{ share.merchantName }}: {{ share.amount | currency: 'ARS' : 'symbol-narrow' }}</li>
        }
      </ul>

      <div class="actions">
        @if (r.googleMapsUrl; as url) {
          <a mat-stroked-button [href]="url" target="_blank" rel="noopener">{{ 'courier.round.openMaps' | t }}</a>
        } @else {
          <p>{{ 'courier.round.noMaps' | t }}</p>
        }
        @if (r.status === 'PLANNED') {
          <button mat-flat-button type="button" [disabled]="dispatching()" (click)="dispatch(r)">{{ 'courier.round.dispatch' | t }}</button>
        } @else {
          <p role="status">{{ 'courier.round.dispatched' | t }}</p>
        }
      </div>
      @if (actionError(); as e) {
        <app-error-message [error]="e" />
      }
    }
  `,
  styles: `
    ol {
      padding-left: 1.25rem;
    }
    li {
      margin-block: 0.4rem;
    }
    .actions {
      display: grid;
      gap: 0.75rem;
      margin-block: 1.5rem;
    }
  `,
})
export class RoundDetail {
  /** Route param. */
  readonly roundId = input.required<string>();

  private readonly api = inject(Api);

  protected readonly round = rxResource({ params: () => this.roundId(), stream: ({ params }) => this.api.round(params) });
  protected readonly loadError = computed(() => (this.round.error() ? toApiError(this.round.error()) : null));
  protected readonly dispatching = signal(false);
  protected readonly actionError = signal<ApiError | null>(null);

  /** Numbered deliveries; shops shown once (pickups and return share the location). */
  protected readonly markers = computed<MapMarker[]>(() => {
    const r = this.round.value();
    if (!r) return [];
    const shops = new Map<string, Stop>();
    r.stops.filter((s) => s.kind === 'PICKUP').forEach((s) => shops.set(s.merchantId, s));
    let n = 0;
    return [
      ...[...shops.values()].map((s): MapMarker => ({ location: s.location, label: '🏪', title: s.merchantName, kind: 'shop' })),
      ...r.stops
        .filter((s) => s.kind === 'DELIVERY')
        .map((s): MapMarker => ({ location: s.location, label: String(++n), title: `${n}. ${s.address ?? ''}` })),
    ];
  });

  protected readonly stopKindKey = (stop: Stop): TranslationKey => `courier.round.stopKind.${stop.kind}`;

  protected dispatch(r: Round): void {
    this.dispatching.set(true);
    this.actionError.set(null);
    this.api.dispatch(r.id).subscribe({
      next: (updated) => {
        this.round.set(updated);
        this.dispatching.set(false);
      },
      error: (e: unknown) => {
        this.actionError.set(toApiError(e));
        this.dispatching.set(false);
      },
    });
  }
}
