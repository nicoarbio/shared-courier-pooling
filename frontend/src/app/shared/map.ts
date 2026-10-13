import { Component, ElementRef, afterNextRender, effect, input, output, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { Coordinates } from '../core/api.models';

export interface MapMarker {
  location: Coordinates;
  /** Short text inside the marker, e.g. a stop number. */
  label: string;
  /** Accessible name of the marker. */
  title: string;
  kind?: 'shop' | 'destination';
  draggable?: boolean;
}

/**
 * Minimal Leaflet map over OpenStreetMap tiles (free, no API key; attribution required).
 * Markers are HTML (divIcon): no image assets to bundle, and they can show stop numbers.
 * A draggable marker reports its new position through `moved`. Dragging is a fine adjustment: the accessible
 * way to set a point is choosing an address from the list, which places the marker.
 */
@Component({
  selector: 'app-map',
  template: `<div #container class="map" role="region" [attr.aria-label]="ariaLabel()"></div>`,
  styles: `
    .map {
      height: 18rem;
      border-radius: 0.5rem;
    }
    :host ::ng-deep .marker {
      display: grid;
      place-items: center;
      width: 1.75rem;
      height: 1.75rem;
      border-radius: 50%;
      border: 2px solid #fff;
      font: 600 0.8rem/1 Roboto, sans-serif;
      color: #fff;
      background: #1a237e;
      box-shadow: 0 1px 3px rgb(0 0 0 / 40%);
    }
    :host ::ng-deep .marker.shop {
      background: #00695c;
    }
    :host ::ng-deep .marker.draggable {
      background: #b71c1c;
      cursor: grab;
    }
  `,
})
export class MapView {
  readonly markers = input.required<MapMarker[]>();
  readonly ariaLabel = input('Mapa');
  readonly moved = output<Coordinates>();

  private readonly container = viewChild.required<ElementRef<HTMLElement>>('container');
  private map?: L.Map;
  private layer = L.layerGroup();

  constructor() {
    // Leaflet needs the DOM element, so the map is created after the first render.
    afterNextRender(() => {
      this.map = L.map(this.container().nativeElement, { center: [-34.62, -58.44], zoom: 15 });
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(this.map);
      this.layer.addTo(this.map);
      this.render(this.markers());
    });
    effect(() => this.render(this.markers()));
  }

  private render(markers: MapMarker[]): void {
    if (!this.map) return;
    this.layer.clearLayers();
    for (const m of markers) {
      const marker = L.marker([m.location.lat, m.location.lon], {
        title: m.title,
        alt: m.title,
        draggable: m.draggable ?? false,
        keyboard: true,
        icon: L.divIcon({
          className: '',
          html: `<span class="marker ${m.kind ?? 'destination'}${m.draggable ? ' draggable' : ''}">${m.label}</span>`,
          iconSize: [28, 28],
          iconAnchor: [14, 14],
        }),
      });
      if (m.draggable) {
        marker.on('dragend', () => {
          const { lat, lng } = marker.getLatLng();
          this.moved.emit({ lon: lng, lat });
        });
      }
      marker.addTo(this.layer);
    }
    if (markers.length === 1) {
      this.map.setView([markers[0].location.lat, markers[0].location.lon], 17);
    } else if (markers.length > 1) {
      this.map.fitBounds(L.latLngBounds(markers.map((m) => [m.location.lat, m.location.lon])), { padding: [24, 24] });
    }
  }
}
