import { defaultWindow, toApiDateTime } from './wall-clock';

describe('wall clock helpers', () => {
  it('default window starts at the next multiple of 5 minutes and lasts 30', () => {
    expect(defaultWindow(new Date(2026, 9, 13, 18, 52, 30))).toEqual({ start: '2026-10-13T18:55', end: '2026-10-13T19:25' });
    expect(defaultWindow(new Date(2026, 9, 13, 18, 55, 0))).toEqual({ start: '2026-10-13T19:00', end: '2026-10-13T19:30' });
  });

  it('rolls over to the next hour and day', () => {
    expect(defaultWindow(new Date(2026, 9, 13, 23, 58))).toEqual({ start: '2026-10-14T00:00', end: '2026-10-14T00:30' });
  });

  it('adds seconds for the API', () => {
    expect(toApiDateTime('2026-10-13T19:00')).toBe('2026-10-13T19:00:00');
  });
});
