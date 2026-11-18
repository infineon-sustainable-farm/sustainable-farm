// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import EquipmentFormModal from './EquipmentFormModal';

afterEach(cleanup);

function renderForm() {
  const onSubmit = vi.fn();
  render(<EquipmentFormModal isOpen onClose={() => {}} onSubmit={onSubmit} serverError={null} />);
  // The kit modal renders in a portal, outside the render container.
  const nameInput = document.querySelector('input[name="name"]');
  return { onSubmit, nameInput };
}

describe('EquipmentFormModal', () => {
  it('accepts a name of 100 characters, the length of the shared equipment register', () => {
    const { onSubmit, nameInput } = renderForm();
    fireEvent.change(nameInput, { target: { name: 'name', value: 'N'.repeat(100) } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ name: 'N'.repeat(100) }));
  });

  it('refuses a name longer than 100 characters', () => {
    const { onSubmit, nameInput } = renderForm();
    fireEvent.change(nameInput, { target: { name: 'name', value: 'N'.repeat(101) } });
    fireEvent.click(screen.getByRole('button', { name: 'Save' }));
    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('Name must be 100 characters or fewer.')).toBeTruthy();
  });
});
