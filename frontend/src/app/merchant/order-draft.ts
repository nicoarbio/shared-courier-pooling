import { Service, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { form, required, validate } from '@angular/forms/signals';
import { catchError, debounceTime, distinctUntilChanged, map, of, startWith, switchMap } from 'rxjs';
import { Api } from '../core/api';
import { ApiError, toApiError } from '../core/api-error';
import { Coordinates, GeocodeCandidate, Merchant, Order } from '../core/api.models';
import { defaultWindow, toApiDateTime } from '../core/wall-clock';

const SEARCH_DEBOUNCE_MS = 400;
const MIN_QUERY_LENGTH = 3;

type SearchState = { status: 'idle' } | { status: 'searching' } | { status: 'done'; candidates: GeocodeCandidate[] } | { status: 'failed'; error: ApiError };

/**
 * State and behaviour of the "new order" screen, kept out of the component so the visual layer can change freely:
 * search the address while typing (Nominatim only matches whole words, so this is search-as-you-type, not prefix
 * autocomplete), choose a candidate, adjust the point on the map, set the delivery window and submit.
 * Provided per screen (not a singleton): each visit starts a fresh draft.
 */
@Service({ autoProvided: false })
export class OrderDraft {
  private readonly api = inject(Api);

  readonly merchant = signal<Merchant | null>(null);

  // Address search
  readonly query = signal('');
  readonly search = toSignal(
    toObservable(this.query).pipe(
      map((q) => q.trim()),
      debounceTime(SEARCH_DEBOUNCE_MS),
      distinctUntilChanged(),
      switchMap((q) => (q.length < MIN_QUERY_LENGTH ? of<SearchState>({ status: 'idle' }) : this.runSearch(q))),
    ),
    { initialValue: { status: 'idle' } as SearchState },
  );
  readonly candidates = computed(() => {
    const s = this.search();
    return s.status === 'done' ? s.candidates : [];
  });

  // Chosen point: from a candidate, then possibly dragged on the map
  readonly chosen = signal<GeocodeCandidate | null>(null);
  readonly destination = signal<Coordinates | null>(null);
  readonly missingHouseNumber = computed(() => this.chosen() !== null && this.chosen()?.houseNumber === null);

  // Delivery window (Signal Forms)
  readonly window = signal(defaultWindow(new Date()));
  readonly windowForm = form(this.window, (path) => {
    required(path.start);
    required(path.end);
    validate(path.end, ({ value, valueOf }) => (value() > valueOf(path.start) ? undefined : { kind: 'windowOrder' }));
  });

  // Submission
  readonly submitting = signal(false);
  readonly submitted = signal<Order | null>(null);
  readonly error = signal<ApiError | null>(null);
  readonly canSubmit = computed(() => this.destination() !== null && this.windowForm().valid() && !this.submitting());

  choose(candidate: GeocodeCandidate): void {
    this.chosen.set(candidate);
    this.destination.set(candidate.location);
  }

  move(point: Coordinates): void {
    this.destination.set(point);
  }

  submit(): void {
    const merchant = this.merchant();
    const destination = this.destination();
    if (!merchant || !destination || !this.canSubmit()) return;
    this.submitting.set(true);
    this.error.set(null);
    this.api
      .createOrder({
        merchantId: merchant.id,
        destination,
        address: this.chosen()?.label ?? this.query().trim(),
        windowStart: toApiDateTime(this.window().start),
        windowEnd: toApiDateTime(this.window().end),
      })
      .subscribe({
        next: (order) => {
          this.submitted.set(order);
          this.submitting.set(false);
        },
        error: (e: unknown) => {
          this.error.set(toApiError(e));
          this.submitting.set(false);
        },
      });
  }

  /** Clears everything but the merchant, for the next order. */
  reset(): void {
    this.query.set('');
    this.chosen.set(null);
    this.destination.set(null);
    this.window.set(defaultWindow(new Date()));
    this.submitted.set(null);
    this.error.set(null);
  }

  private runSearch(q: string) {
    const merchant = this.merchant();
    if (!merchant) return of<SearchState>({ status: 'idle' });
    return this.api.geocode(q, merchant.id).pipe(
      map((candidates): SearchState => ({ status: 'done', candidates })),
      catchError((e: unknown) => of<SearchState>({ status: 'failed', error: toApiError(e) })),
      startWith<SearchState>({ status: 'searching' }),
    );
  }
}
