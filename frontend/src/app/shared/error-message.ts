import { Component, input } from '@angular/core';
import { ApiError } from '../core/api-error';
import { TranslatePipe } from '../i18n/translate';

/** A failed call, with the request id the backend returned so it can be found in the logs. */
@Component({
  selector: 'app-error-message',
  imports: [TranslatePipe],
  template: `
    <p role="alert" class="error">
      {{ error().key | t }}
      @if (error().requestId; as id) {
        <small>{{ 'errors.requestId' | t: { id } }}</small>
      }
    </p>
  `,
  styles: `
    .error {
      color: var(--mat-sys-error);
    }
    small {
      display: block;
    }
  `,
})
export class ErrorMessage {
  readonly error = input.required<ApiError>();
}
