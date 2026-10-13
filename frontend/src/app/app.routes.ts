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
  {
    path: 'repartidor',
    children: [
      { path: '', loadComponent: () => import('./courier/rounds').then((m) => m.Rounds) },
      { path: 'rondas/:roundId', loadComponent: () => import('./courier/round-detail').then((m) => m.RoundDetail) },
    ],
  },
  { path: '**', redirectTo: '' },
];
