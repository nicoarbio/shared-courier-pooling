import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from './i18n/translate';

/** Entry point: there is no login, the user just says who they are. */
@Component({
  selector: 'app-home',
  imports: [RouterLink, MatButtonModule, TranslatePipe],
  template: `
    <h1>{{ 'home.question' | t }}</h1>
    <section class="choice">
      <a mat-flat-button routerLink="/comercio">{{ 'home.merchant' | t }}</a>
      <p>{{ 'home.merchantHint' | t }}</p>
    </section>
    <section class="choice">
      <a mat-stroked-button routerLink="/repartidor">{{ 'home.courier' | t }}</a>
      <p>{{ 'home.courierHint' | t }}</p>
    </section>
  `,
  styles: `
    .choice {
      margin-block: 1.5rem;
    }
    .choice a {
      width: 100%;
    }
  `,
})
export class Home {}
