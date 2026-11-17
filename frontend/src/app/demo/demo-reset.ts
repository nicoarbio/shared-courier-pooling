import { Component, Service, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { Api } from '../core/api';
import { ApiError, toApiError } from '../core/api-error';
import { TranslatePipe } from '../i18n/translate';
import { ErrorMessage } from '../shared/error-message';

type ResetState =
  | { step: 'idle' }
  | { step: 'confirming' }
  | { step: 'resetting' }
  | { step: 'done'; orders: number; rounds: number }
  | { step: 'failed'; error: ApiError };

/**
 * "Reset demo": only offered when the backend has it enabled (GET /api/demo answers 200), and only after an
 * explicit confirmation, since it deletes every order and round.
 */
@Service({ autoProvided: false })
export class DemoReset {
  private readonly api = inject(Api);

  readonly enabled = signal(false);
  readonly state = signal<ResetState>({ step: 'idle' });

  constructor() {
    this.api.demoStatus().subscribe({ next: () => this.enabled.set(true), error: () => this.enabled.set(false) });
  }

  ask(): void {
    this.state.set({ step: 'confirming' });
  }

  cancel(): void {
    this.state.set({ step: 'idle' });
  }

  confirm(): void {
    this.state.set({ step: 'resetting' });
    this.api.resetDemo().subscribe({
      next: (r) => this.state.set({ step: 'done', orders: r.deletedOrders, rounds: r.deletedRounds }),
      error: (e: unknown) => this.state.set({ step: 'failed', error: toApiError(e) }),
    });
  }
}

@Component({
  selector: 'app-demo-reset',
  providers: [DemoReset],
  imports: [MatButtonModule, TranslatePipe, ErrorMessage],
  template: `
    @if (demo.enabled()) {
      <section class="demo">
        @switch (demo.state().step) {
          @case ('confirming') {
            <p role="alert">{{ 'demo.confirm' | t }}</p>
            <button mat-flat-button type="button" (click)="demo.confirm()">{{ 'demo.confirmYes' | t }}</button>
            <button mat-button type="button" (click)="demo.cancel()">{{ 'demo.confirmNo' | t }}</button>
          }
          @case ('resetting') {
            <p role="status">{{ 'demo.resetting' | t }}</p>
          }
          @default {
            <button mat-stroked-button type="button" (click)="demo.ask()">{{ 'demo.reset' | t }}</button>
          }
        }
        @if (doneMessage(); as message) {
          <p role="status">{{ 'demo.done' | t: message }}</p>
        }
        @if (failure(); as e) {
          <app-error-message [error]="e" />
        }
      </section>
    }
  `,
  styles: `
    .demo {
      margin-top: 3rem;
      padding-top: 1rem;
      border-top: 1px solid var(--mat-sys-outline-variant);
    }
  `,
})
export class DemoResetPanel {
  protected readonly demo = inject(DemoReset);

  protected doneMessage() {
    const s = this.demo.state();
    return s.step === 'done' ? { orders: s.orders, rounds: s.rounds } : null;
  }

  protected failure() {
    const s = this.demo.state();
    return s.step === 'failed' ? s.error : null;
  }
}
