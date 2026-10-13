/*
 * Mirror of the backend API contract (backend/src/main/kotlin/.../api/Dtos.kt).
 * Date-times are city wall-clock times without offset, e.g. "2026-10-13T19:00:00".
 */

export interface Coordinates {
  lon: number;
  lat: number;
}

export interface Merchant {
  id: string;
  name: string;
  location: Coordinates;
}

export interface GeocodeCandidate {
  label: string;
  street: string | null;
  houseNumber: string | null;
  neighbourhood: string | null;
  location: Coordinates;
}

export interface CreateOrderRequest {
  merchantId: string;
  destination: Coordinates;
  address: string | null;
  windowStart: string;
  windowEnd: string;
}

export type OrderStatus = 'PENDING' | 'PLANNED' | 'DISPATCHED';
export type RoundStatus = 'PLANNED' | 'DISPATCHED';
export type StopKind = 'PICKUP' | 'DELIVERY' | 'RETURN';

export interface OrderRoundInfo {
  roundId: string;
  roundStatus: RoundStatus;
  merchantShare: number;
  estimatedDelivery: string;
}

export interface Order {
  id: string;
  merchantId: string;
  address: string | null;
  destination: Coordinates;
  windowStart: string;
  windowEnd: string;
  status: OrderStatus;
  round: OrderRoundInfo | null;
}

export interface RoundSummary {
  id: string;
  status: RoundStatus;
  departureAt: string;
  estimatedReturn: string;
  deliveries: number;
  distanceMeters: number;
  cost: number;
}

export interface Stop {
  seq: number;
  kind: StopKind;
  merchantId: string;
  merchantName: string;
  orderId: string | null;
  address: string | null;
  location: Coordinates;
  estimatedArrival: string;
}

export interface Share {
  merchantId: string;
  merchantName: string;
  amount: number;
}

export interface Round {
  id: string;
  status: RoundStatus;
  departureAt: string;
  estimatedReturn: string;
  distanceMeters: number;
  drivingSeconds: number;
  cost: number;
  pricePerKm: number;
  stops: Stop[];
  shares: Share[];
  googleMapsUrl: string | null;
}
