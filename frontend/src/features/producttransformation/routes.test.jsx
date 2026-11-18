// @vitest-environment jsdom
import { render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import routes, { nav } from './routes';

// Every request stays pending: the test checks that each screen is reached by its URL, not its data.
vi.mock('../../shared/api/client', () => {
  const pending = () => new Promise(() => {});
  return {
    apiClient: { get: vi.fn(pending), post: vi.fn(pending), put: vi.fn(pending), patch: vi.fn(pending), delete: vi.fn(pending) },
  };
});

const SCREENS = [
  ['', 'Product Transformation Dashboard'],
  ['raw-intake', 'Raw Intake'],
  ['batches', 'Batch Management'],
  ['washing-sorting', 'Washing & Sorting'],
  ['drying', 'Drying'],
  ['quality-control', 'Quality Control'],
  ['compliance', 'Compliance'],
  ['packaging', 'Packaging'],
  ['equipment', 'Equipment'],
];

function renderAt(path) {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(<RouterProvider router={router} />);
}

describe('product transformation module routes', () => {
  it('renders inside the application shell, without a module layout of its own', () => {
    const [moduleRoute] = routes;
    expect(nav.shell).toBe(true);
    expect(moduleRoute.path).toBe('product-transformation');
    expect(moduleRoute.element).toBeUndefined();
  });

  it('keeps the same URLs, one sidebar entry per screen, and no Settings placeholder', () => {
    const [moduleRoute] = routes;
    expect(moduleRoute.children.map((child) => child.path ?? '')).toEqual(SCREENS.map(([path]) => path));
    expect(nav.items.map((item) => item.to)).toEqual(
      SCREENS.map(([path]) => (path ? `/product-transformation/${path}` : '/product-transformation')),
    );
    expect(nav.items.some((item) => item.to.endsWith('/settings'))).toBe(false);
  });

  it.each(SCREENS)('reaches the screen at /product-transformation/%s, titled %s', (path, title) => {
    renderAt(path ? `/product-transformation/${path}` : '/product-transformation');
    expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument();
  });

  it('no longer routes the Settings placeholder', () => {
    renderAt('/product-transformation/settings');
    expect(screen.queryByText('Settings module coming soon...')).toBeNull();
  });
});
