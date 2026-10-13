import { HttpClient, HttpParams } from '@angular/common/http';
import { Service, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateOrderRequest,
  GeocodeCandidate,
  Merchant,
  Order,
  Round,
  RoundStatus,
  RoundSummary,
} from './api.models';

/** Thin client for the backend REST API. Paths are relative: served behind the same origin (dev proxy / nginx). */
@Service()
export class Api {
  private readonly http = inject(HttpClient);

  merchants(): Observable<Merchant[]> {
    return this.http.get<Merchant[]>('/api/merchants');
  }

  merchantOrders(merchantId: string): Observable<Order[]> {
    return this.http.get<Order[]>(`/api/merchants/${merchantId}/orders`);
  }

  geocode(query: string, merchantId: string): Observable<GeocodeCandidate[]> {
    return this.http.get<GeocodeCandidate[]>('/api/geocode', {
      params: new HttpParams().set('q', query).set('merchantId', merchantId),
    });
  }

  createOrder(request: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>('/api/orders', request);
  }

  rounds(status: RoundStatus = 'PLANNED'): Observable<RoundSummary[]> {
    return this.http.get<RoundSummary[]>('/api/rounds', { params: { status } });
  }

  round(id: string): Observable<Round> {
    return this.http.get<Round>(`/api/rounds/${id}`);
  }

  dispatch(id: string): Observable<Round> {
    return this.http.post<Round>(`/api/rounds/${id}/dispatch`, null);
  }
}
