import { Routes } from '@angular/router';
import { Home } from './home';

export const routes: Routes = [
  { path: '', component: Home },
  {
    path: 'comercio',
    children: [
      { path: '', loadComponent: () => import('./merchant/select-merchant').then((m) => m.SelectMerchant) },
      { path: ':merchantId/nuevo', loadComponent: () => import('./merchant/new-order').then((m) => m.NewOrder) },
      { path: ':merchantId/pedidos', loadComponent: () => import('./merchant/my-orders').then((m) => m.MyOrders) },
    ],
  },
  { path: '**', redirectTo: '' },
];
