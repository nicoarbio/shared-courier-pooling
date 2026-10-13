import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TranslatePipe } from './i18n/translate';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, MatToolbarModule, TranslatePipe],
  template: `
    <mat-toolbar color="primary">
      <a routerLink="/" class="brand">{{ 'app.title' | t }}</a>
    </mat-toolbar>
    <main>
      <router-outlet />
    </main>
  `,
  styles: `
    .brand {
      color: inherit;
      text-decoration: none;
    }
    main {
      max-width: 40rem;
      margin: 0 auto;
      padding: 1rem;
    }
  `,
})
export class App {}
