import { describe, expect, it } from 'vitest';
import routes, { nav } from './routes';

describe('salesmarketing module routes', () => {
  it('renders inside the application shell', () => {
    expect(nav.shell).toBe(true);
  });

  it('keeps the same URLs, without a module layout of its own', () => {
    const [moduleRoute] = routes;
    expect(moduleRoute.path).toBe('salesmarketing');
    expect(moduleRoute.element).toBeUndefined();
    expect(moduleRoute.children.map((child) => child.path ?? '')).toEqual([
      '',
      'forecasting',
      'demand-alerts',
      'reports',
      'crm',
      'sales-channels',
      'pricing',
      'campaigns',
      'delivery',
    ]);
    expect(nav.items.map((item) => item.to).sort()).toEqual(
      moduleRoute.children.map((child) => (child.path ? `/salesmarketing/${child.path}` : '/salesmarketing')).sort()
    );
  });
});
