// @vitest-environment jsdom
// DOM environment declared locally: the project has no shared Vitest configuration.
import { afterEach, describe, expect, it } from 'vitest';
import { cleanup, render, screen } from '@testing-library/react';
import PTListHeader from './PTListHeader';

afterEach(cleanup);

describe('PTListHeader', () => {
  it('wraps the title and the actions instead of pushing the page sideways', () => {
    const { container } = render(
      <PTListHeader title="Compliance" subtitle="Track records per batch.">
        <button type="button">Export CSV</button>
        <button type="button">New Compliance Record</button>
      </PTListHeader>,
    );

    const root = container.firstChild;
    expect(root.className).toContain('flex-wrap');
    expect(root.className).toContain('items-start');
    expect(root.className).toContain('gap-3');

    // The title block may shrink; the action group may wrap.
    expect(root.firstChild.className).toContain('min-w-0');
    expect(root.lastChild.className).toContain('flex-wrap');

    expect(screen.getByRole('heading', { level: 1, name: 'Compliance' })).toBeTruthy();
    expect(screen.getByText('Track records per batch.')).toBeTruthy();
    expect(screen.getAllByRole('button')).toHaveLength(2);
  });

  it('renders without actions or subtitle when a screen has none', () => {
    render(<PTListHeader title="Operators" />);

    expect(screen.getByRole('heading', { level: 1, name: 'Operators' })).toBeTruthy();
    expect(screen.queryByRole('button')).toBeNull();
  });
});
