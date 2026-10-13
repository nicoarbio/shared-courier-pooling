import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MerchantSession } from '../core/merchant-session';
import { TranslatePipe } from '../i18n/translate';

/** Header of the merchant pages: who is acting and the two sections. */
@Component({
  selector: 'app-merchant-nav',
  imports: [RouterLink, RouterLinkActive, MatButtonModule, TranslatePipe],
  template: `
    @if (session.merchant(); as merchant) {
      <p class="who">
        <strong>{{ merchant.name }}</strong>
        <a routerLink="/comercio">{{ 'merchant.select.change' | t }}</a>
      </p>
      <nav>
        <a mat-button [routerLink]="['/comercio', merchant.id, 'nuevo']" routerLinkActive="active" ariaCurrentWhenActive="page">
          {{ 'merchant.nav.newOrder' | t }}
        </a>
        <a mat-button [routerLink]="['/comercio', merchant.id, 'pedidos']" routerLinkActive="active" ariaCurrentWhenActive="page">
          {{ 'merchant.nav.orders' | t }}
        </a>
      </nav>
    }
  `,
  styles: `
    .who {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
    }
    .active {
      text-decoration: underline;
    }
  `,
})
export class MerchantNav {
  protected readonly session = inject(MerchantSession);
}
