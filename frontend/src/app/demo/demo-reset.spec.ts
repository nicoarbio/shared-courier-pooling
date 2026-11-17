import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Api } from '../core/api';
import { DemoReset } from './demo-reset';

describe('DemoReset', () => {
  const setup = (enabled: boolean) => {
    const api = {
      demoStatus: vi.fn(() => (enabled ? of({ resetEnabled: true }) : throwError(() => new Error('404')))),
      resetDemo: vi.fn(() => of({ deletedOrders: 5, deletedRounds: 1 })),
    };
    TestBed.configureTestingModule({ providers: [DemoReset, { provide: Api, useValue: api }] });
    return { demo: TestBed.inject(DemoReset), api };
  };

  it('is hidden when the backend does not enable it', () => {
    expect(setup(false).demo.enabled()).toBe(false);
  });

  it('only resets after an explicit confirmation', () => {
    const { demo, api } = setup(true);
    expect(demo.enabled()).toBe(true);

    demo.ask();
    expect(api.resetDemo).not.toHaveBeenCalled();
    demo.cancel();
    expect(demo.state()).toEqual({ step: 'idle' });

    demo.ask();
    demo.confirm();
    expect(api.resetDemo).toHaveBeenCalledTimes(1);
    expect(demo.state()).toEqual({ step: 'done', orders: 5, rounds: 1 });
  });
});
