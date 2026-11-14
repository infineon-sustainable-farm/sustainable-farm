// @vitest-environment jsdom
// DOM environment declared locally: the project has no shared Vitest
// configuration (same approach as the Water Supply and useAuth tests).
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { useState } from 'react';
import { useAccessibleDialog } from './useAccessibleDialog';

afterEach(cleanup);

/** Minimal dialog wired to the hook, mirroring how the PT forms use it. */
function TestDialog({ onClose, closeOnEscape = true, closeOnBackdrop = true }) {
  const { titleId, panelRef, handleBackdropClick } = useAccessibleDialog({
    onClose,
    closeOnEscape,
    closeOnBackdrop,
  });

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      data-testid="backdrop"
      onClick={handleBackdropClick}
    >
      <div ref={panelRef} tabIndex={-1} data-testid="panel">
        <h2 id={titleId}>Dialog title</h2>
        <button type="button">First action</button>
        <button type="button">Last action</button>
      </div>
    </div>
  );
}

function Harness({ closeOnEscape, closeOnBackdrop }) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Open dialog
      </button>
      {open && (
        <TestDialog
          onClose={() => setOpen(false)}
          closeOnEscape={closeOnEscape}
          closeOnBackdrop={closeOnBackdrop}
        />
      )}
    </>
  );
}

/**
 * Mirrors the list screens: the hook is mounted with the screen, the dialog
 * markup appears later when `open` flips to true.
 */
function LateMountDialog() {
  const [open, setOpen] = useState(false);
  const { titleId, panelRef, handleBackdropClick } = useAccessibleDialog({
    open,
    onClose: () => setOpen(false),
  });

  return (
    <>
      <button type="button" onClick={() => setOpen(true)}>
        Open late dialog
      </button>
      {open && (
        <div role="dialog" aria-modal="true" aria-labelledby={titleId} onClick={handleBackdropClick}>
          <div ref={panelRef} tabIndex={-1}>
            <h2 id={titleId}>Late dialog</h2>
            <button type="button">Late action</button>
          </div>
        </div>
      )}
    </>
  );
}

describe('useAccessibleDialog', () => {
  it('moves focus in when the dialog opens after the hook mounted', () => {
    render(<LateMountDialog />);

    fireEvent.click(screen.getByRole('button', { name: 'Open late dialog' }));

    expect(screen.getByRole('dialog', { name: 'Late dialog' })).toBeTruthy();
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'Late action' }));
  });
  it('names the dialog from the title and moves focus to the first control', () => {
    render(<TestDialog onClose={() => {}} />);

    expect(screen.getByRole('dialog', { name: 'Dialog title' })).toBeTruthy();
    expect(document.activeElement).toBe(screen.getByRole('button', { name: 'First action' }));
  });

  it('cycles Tab and Shift+Tab inside the panel', () => {
    render(<TestDialog onClose={() => {}} />);

    const first = screen.getByRole('button', { name: 'First action' });
    const last = screen.getByRole('button', { name: 'Last action' });

    last.focus();
    fireEvent.keyDown(document, { key: 'Tab' });
    expect(document.activeElement).toBe(first);

    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(last);
  });

  it('closes on Escape and returns focus to the trigger', () => {
    render(<Harness />);

    const trigger = screen.getByRole('button', { name: 'Open dialog' });
    trigger.focus();
    fireEvent.click(trigger);

    expect(screen.getByRole('dialog')).toBeTruthy();

    fireEvent.keyDown(document, { key: 'Escape' });

    expect(screen.queryByRole('dialog')).toBeNull();
    expect(document.activeElement).toBe(trigger);
  });

  it('can disable Escape while a request is in flight', () => {
    const onClose = vi.fn();
    render(<TestDialog onClose={onClose} closeOnEscape={false} />);

    fireEvent.keyDown(document, { key: 'Escape' });

    expect(onClose).not.toHaveBeenCalled();
  });

  it('closes on a backdrop click but not on a click inside the panel', () => {
    const onClose = vi.fn();
    render(<TestDialog onClose={onClose} />);

    fireEvent.click(screen.getByTestId('panel'));
    expect(onClose).not.toHaveBeenCalled();

    fireEvent.click(screen.getByTestId('backdrop'));
    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('ignores backdrop clicks when they are disabled', () => {
    const onClose = vi.fn();
    render(<TestDialog onClose={onClose} closeOnBackdrop={false} />);

    fireEvent.click(screen.getByTestId('backdrop'));

    expect(onClose).not.toHaveBeenCalled();
  });
});
