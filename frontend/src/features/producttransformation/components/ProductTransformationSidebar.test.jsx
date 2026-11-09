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
  it('renders the six module entries with their real routes', () => {
    renderSidebar();

    const nav = screen.getByRole('navigation', { name: 'Product Transformation module' });
    const links = within(nav).getAllByRole('link');

    expect(links.map((link) => link.textContent.trim())).toEqual([
      'Dashboard',
      'Batches',
      'Quality Control',
      'Compliance',
      'Packaging',
      'Settings',
    ]);
    expect(links.map((link) => link.getAttribute('href'))).toEqual([
      '/product-transformation',
      '/product-transformation/batches',
      '/product-transformation/quality-control',
      '/product-transformation/compliance',
      '/product-transformation/packaging',
      '/product-transformation/settings',
    ]);
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
