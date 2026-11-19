// @vitest-environment jsdom
// DOM environment declared locally: the project has no shared Vitest configuration.
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { ConfirmDialog } from './ConfirmDialog';

afterEach(cleanup);

describe('ConfirmDialog', () => {
  it('has an accessible name and describes itself with the warning message', () => {
    render(
      <ConfirmDialog
        title="Delete batch"
        message="Delete batch B-001? This action cannot be undone."
        onConfirm={() => {}}
        onCancel={() => {}}
      />,
    );

    const dialog = screen.getByRole('dialog', { name: 'Delete batch' });
    expect(dialog.getAttribute('aria-describedby')).toBeTruthy();

    const message = screen.getByText('Delete batch B-001? This action cannot be undone.');
    expect(message.id).toBe(dialog.getAttribute('aria-describedby'));
    // 4.5:1 is required at 14px; #55635f on white is 6.30:1.
    expect(['#55635f', 'rgb(85, 99, 95)']).toContain(message.style.color);
  });

  it('starts on Cancel, cancels on Escape and keeps a single level-2 heading', () => {
    const onCancel = vi.fn();
    render(
      <ConfirmDialog
        message="Delete intake RI-1?"
        onConfirm={() => {}}
        onCancel={onCancel}
      />,
    );

    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getAllByRole('heading')).toHaveLength(1);
    expect(screen.getByRole('heading', { level: 2 })).toBeTruthy();

    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('does not close while the deletion is running', () => {
    const onCancel = vi.fn();
    render(
      <ConfirmDialog busy message="Deleting…" onConfirm={() => {}} onCancel={onCancel} />,
    );

    fireEvent.keyDown(document, { key: 'Escape' });

    expect(onCancel).not.toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Deleting…' }).disabled).toBe(true);
  });
});
