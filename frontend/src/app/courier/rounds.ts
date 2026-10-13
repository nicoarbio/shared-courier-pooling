import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { toApiError } from '../core/api-error';
import { reloadEvery } from '../core/polling';
import { TranslatePipe } from '../i18n/translate';
import { ErrorMessage } from '../shared/error-message';

/** Rounds ready to leave, by departure time. Refreshes on its own. */
@Component({
  selector: 'app-rounds',
  imports: [RouterLink, MatButtonModule, MatCardModule, DatePipe, DecimalPipe, TranslatePipe, ErrorMessage],
  template: `
    <header>
      <h1>{{ 'courier.rounds.title' | t }}</h1>
      <button mat-button type="button" (click)="rounds.reload()">{{ 'common.refresh' | t }}</button>
    </header>
    @if (error(); as e) {
      <app-error-message [error]="e" />
    }
    @for (round of rounds.value(); track round.id) {
      <mat-card appearance="outlined">
        <mat-card-content>
          <p>
            {{
              'courier.rounds.summary'
                | t
                  : {
                      deliveries: round.deliveries,
                      km: (round.distanceMeters / 1000 | number: '1.1-1') ?? '',
                      time: (round.departureAt | date: 'HH:mm') ?? '',
                    }
            }}
          </p>
        </mat-card-content>
        <mat-card-actions>
          <a mat-button [routerLink]="['/repartidor/rondas', round.id]">{{ 'courier.rounds.open' | t }}</a>
        </mat-card-actions>
      </mat-card>
    } @empty {
      @if (!rounds.isLoading() && !error()) {
        <p>{{ 'courier.rounds.empty' | t }}</p>
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
export class Rounds {
  private readonly api = inject(Api);

  protected readonly rounds = rxResource({ stream: () => this.api.rounds('PLANNED'), defaultValue: [] });
  protected readonly error = computed(() => (this.rounds.error() ? toApiError(this.rounds.error()) : null));

  constructor() {
    reloadEvery(this.rounds);
  }
}
