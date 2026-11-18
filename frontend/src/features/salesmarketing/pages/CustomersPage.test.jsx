// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CustomersPage from './CustomersPage';

const api = vi.hoisted(() => ({
  getCustomers: vi.fn(),
  getOrders: vi.fn(),
  getCustomerContacts: vi.fn(),
  getCertifications: vi.fn(),
  getCustomerCertifications: vi.fn(),
  getCustomerById: vi.fn(),
  createCustomer: vi.fn(),
  updateCustomer: vi.fn(),
  deleteCustomer: vi.fn(),
  createCustomerContact: vi.fn(),
  updateCustomerContact: vi.fn(),
  deleteCustomerContact: vi.fn(),
  linkCustomerCertification: vi.fn(),
  unlinkCustomerCertification: vi.fn(),
}));

// The page waits for several requests before the list shows: leave room on a busy machine.
const LOAD = { timeout: 5000 };

vi.mock('../api/api', () => ({ api }));

const customer = {
  customerId: 3,
  companyName: 'Fresh Fruits GmbH',
  customerCode: 'CUST-0003-GER',
  customerType: 'distributor',
  country: 'Germany',
  region: 'Bavaria',
  city: 'Munich',
  status: 'active',
  paymentTerms: 'Net 30',
};
const contact = { contactId: 9, customerId: 3, fullName: 'Purchasing desk', email: 'desk@fresh.example', role: 'Buyer' };

beforeEach(() => {
  Object.values(api).forEach((mock) => mock.mockReset().mockResolvedValue([]));
  api.getCustomers.mockResolvedValue([customer]);
  api.getCustomerContacts.mockResolvedValue([contact]);
  api.getOrders.mockResolvedValue([
    { customerId: 3, orderDate: `${new Date().getFullYear()}-02-01T12:00:00`, totalVolumeKg: 1250.5 },
  ]);
  api.getCustomerById.mockResolvedValue(customer);
  api.createCustomer.mockResolvedValue({});
  api.updateCustomer.mockResolvedValue({});
  api.deleteCustomer.mockResolvedValue(null);
  api.createCustomerContact.mockResolvedValue({});
  api.deleteCustomerContact.mockResolvedValue(null);
});

function renderPage() {
  return render(
    <MemoryRouter>
      <CustomersPage />
    </MemoryRouter>
  );
}

describe('CustomersPage', () => {
  it('lists the customers with their order volume and shows the selected one', async () => {
    renderPage();
    expect(await screen.findByRole('button', { name: 'Fresh Fruits GmbH' }, LOAD)).toBeInTheDocument();
    expect(screen.getByText('1,250.5 kg')).toBeInTheDocument();
    expect(screen.getByText('ID: CUST-0003-GER')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Fresh Fruits GmbH' })).toBeInTheDocument();
    expect(screen.getByText('Purchasing desk')).toBeInTheDocument();
    expect(screen.getByText('Showing 1 of 1 matching (1 total)')).toBeInTheDocument();
  });

  it('creates a customer from the Add customer dialog', async () => {
    renderPage();
    await screen.findByRole('button', { name: 'Fresh Fruits GmbH' }, LOAD);
    fireEvent.click(screen.getByRole('button', { name: 'ADD CUSTOMER' }));
    const dialog = screen.getByRole('dialog', { name: 'Add New Customer' });
    fireEvent.change(within(dialog).getByLabelText(/Company Name/), { target: { value: 'Mango Export SARL' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Customer' }));

    await waitFor(() => expect(api.createCustomer).toHaveBeenCalledTimes(1));
    expect(api.createCustomer).toHaveBeenCalledWith(
      expect.objectContaining({ companyName: 'Mango Export SARL', customerType: 'distributor', country: 'Germany', paymentTerms: 'Net 30' })
    );
    expect(await screen.findByText('Customer "Mango Export SARL" created successfully.')).toBeInTheDocument();
  });

  it('edits a customer from the row menu', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Actions for Fresh Fruits GmbH' }, LOAD));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Edit Customer' }));
    const dialog = await screen.findByRole('dialog', { name: 'Edit Customer #3' });
    expect(within(dialog).getByLabelText(/Company Name/)).toHaveValue('Fresh Fruits GmbH');
    fireEvent.click(within(dialog).getByRole('button', { name: 'Save Changes' }));
    await waitFor(() => expect(api.updateCustomer).toHaveBeenCalledWith(3, expect.objectContaining({ city: 'Munich' })));
  });

  it('adds a contact to the selected customer', async () => {
    renderPage();
    await screen.findByRole('button', { name: 'Fresh Fruits GmbH' }, LOAD);
    fireEvent.click(screen.getByRole('button', { name: 'ADD' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Contact' });
    fireEvent.change(within(dialog).getByLabelText(/Full Name/), { target: { value: 'Logistics desk' } });
    fireEvent.click(within(dialog).getByRole('checkbox', { name: 'Mark as Primary Contact' }));
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add Contact' }));
    await waitFor(() =>
      expect(api.createCustomerContact).toHaveBeenCalledWith(
        expect.objectContaining({ customerId: 3, fullName: 'Logistics desk', isPrimary: true, email: null })
      )
    );
  });

  it('asks for confirmation before deleting a customer or a contact', async () => {
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Actions for Fresh Fruits GmbH' }, LOAD));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Delete Customer' }));
    let dialog = screen.getByRole('dialog', { name: 'Delete customer?' });
    expect(within(dialog).getByText('Munich, Bavaria, Germany')).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(api.deleteCustomer).toHaveBeenCalledWith(3));
    expect(await screen.findByText('Customer "Fresh Fruits GmbH" deleted successfully.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Delete contact' }));
    dialog = screen.getByRole('dialog', { name: 'Delete contact?' });
    expect(within(dialog).getByText('desk@fresh.example')).toBeInTheDocument();
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(api.deleteCustomerContact).toHaveBeenCalledWith(9));
  });

  // The shared client returns the body: these read the response itself, not a .data field.
  it('shows the linked certifications of the customer and unlinks one on click', async () => {
    api.getCertifications.mockResolvedValue([{ certificationId: 1, name: 'Organic EU' }]);
    api.getCustomerCertifications.mockResolvedValue([{ certificationId: 1, name: 'Organic EU' }]);
    renderPage();
    const toggle = await screen.findByRole('button', { name: 'Organic EU' }, LOAD);
    await waitFor(() => expect(toggle).toHaveAttribute('aria-pressed', 'true'));
    fireEvent.click(toggle);
    await waitFor(() => expect(api.unlinkCustomerCertification).toHaveBeenCalledWith(3, 1));
    expect(api.linkCustomerCertification).not.toHaveBeenCalled();
  });

  it('selects the customer it has just created', async () => {
    const created = { ...customer, customerId: 5, companyName: 'Mango Export SARL', customerCode: 'CUST-0005' };
    api.createCustomer.mockResolvedValue(created);
    renderPage();
    await screen.findByRole('button', { name: 'Fresh Fruits GmbH' }, LOAD);
    api.getCustomers.mockResolvedValue([customer, created]);
    fireEvent.click(screen.getByRole('button', { name: 'ADD CUSTOMER' }));
    const dialog = screen.getByRole('dialog', { name: 'Add New Customer' });
    fireEvent.change(within(dialog).getByLabelText(/Company Name/), { target: { value: 'Mango Export SARL' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Create Customer' }));
    expect(await screen.findByRole('heading', { level: 2, name: 'Mango Export SARL' }, LOAD)).toBeInTheDocument();
  });

  it('fills the edit form from the single customer fetch, not from the list row', async () => {
    api.getCustomerById.mockResolvedValue({ ...customer, companyName: 'Fresh Fruits AG' });
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Actions for Fresh Fruits GmbH' }, LOAD));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Edit Customer' }));
    const dialog = await screen.findByRole('dialog', { name: 'Edit Customer #3' });
    expect(within(dialog).getByLabelText(/Company Name/)).toHaveValue('Fresh Fruits AG');
  });

  it("attaches a contact added from a row menu to that row's customer", async () => {
    const other = { ...customer, customerId: 8, companyName: 'Zest Imports Ltd', customerCode: 'CUST-0008' };
    api.getCustomers.mockResolvedValue([customer, other]);
    renderPage();
    fireEvent.click(await screen.findByRole('button', { name: 'Actions for Zest Imports Ltd' }, LOAD));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Add Contact' }));
    const dialog = screen.getByRole('dialog', { name: 'Add Contact' });
    fireEvent.change(within(dialog).getByLabelText(/Full Name/), { target: { value: 'Import desk' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Add Contact' }));
    await waitFor(() =>
      expect(api.createCustomerContact).toHaveBeenCalledWith(expect.objectContaining({ customerId: 8, fullName: 'Import desk' }))
    );
  });

  it('shows no month-over-month trend the API does not provide', async () => {
    renderPage();
    await screen.findByRole('button', { name: 'Fresh Fruits GmbH' }, LOAD);
    expect(screen.getByText('Total Active')).toBeInTheDocument();
    expect(screen.queryByText(/vs last month/)).not.toBeInTheDocument();
  });
});
