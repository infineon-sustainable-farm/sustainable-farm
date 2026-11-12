// @vitest-environment jsdom
// DOM environment and cleanup are declared locally so this module does not depend on
// extra Vitest configuration (same approach as the Water Supply view tests).
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProductTransformationSidebar from './ProductTransformationSidebar';

afterEach(cleanup);

const renderSidebar = (initialPath = '/product-transformation') =>
  render(
    <MemoryRouter initialEntries={[initialPath]}>
      <ProductTransformationSidebar />
    </MemoryRouter>,
  );

describe('ProductTransformationSidebar', () => {
  it('renders the ten module entries with their real routes', () => {
    renderSidebar();

    const nav = screen.getByRole('navigation', { name: 'Product Transformation module' });
    const links = within(nav).getAllByRole('link');

    expect(links.map((link) => link.textContent.trim())).toEqual([
      'Dashboard',
      'Raw Intake',
      'Batches',
      'Washing & Sorting',
      'Drying',
      'Quality Control',
      'Compliance',
      'Packaging',
      'Equipment',
      'Operators',
    ]);
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/product-transformation',
      '/product-transformation/raw-intake',
      '/product-transformation/batches',
      '/product-transformation/washing-sorting',
      '/product-transformation/drying',
      '/product-transformation/quality-control',
      '/product-transformation/compliance',
      '/product-transformation/packaging',
      '/product-transformation/equipment',
      '/product-transformation/operators',
    ]);
  });

  it('no longer exposes the dead Settings screen', () => {
    renderSidebar();

    expect(screen.queryByRole('link', { name: 'Settings' })).toBeNull();
  });

  it('states the demo limitation when Log out is clicked', () => {
    renderSidebar();

    expect(screen.queryByRole('status')).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: 'Log out' }));

    expect(screen.getByRole('status').textContent).toBe(
      'Authentication is not enabled in this demo.',
    );
  });

  it('marks the current route as active', () => {
    renderSidebar('/product-transformation/packaging');

    const active = screen.getByRole('link', { name: 'Packaging' });
    const inactive = screen.getByRole('link', { name: 'Batches' });

    expect(active.className).toContain('font-semibold');
    expect(inactive.className).not.toContain('font-semibold');
  });

  it('opens and closes the mobile navigation', () => {
    renderSidebar();

    fireEvent.click(screen.getByRole('button', { name: 'Open navigation' }));
    expect(screen.getByRole('button', { name: 'Close navigation' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Close navigation' }));
    expect(screen.getByRole('button', { name: 'Open navigation' })).toBeTruthy();
  });
});
