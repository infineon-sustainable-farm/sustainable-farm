import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiClientMocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
}));

vi.mock('../../../shared/api/client.js', () => ({
  apiClient: apiClientMocks,
}));

import { api } from './api';

beforeEach(() => {
  vi.clearAllMocks();
});

describe('Sales and Marketing API wrapper', () => {
  it('uses the /api prefix and returns the shared client result', () => {
    const body = [{ forecastId: 7 }];
    apiClientMocks.get.mockReturnValue(body);

    expect(api.getDemandForecasts()).toBe(body);
    api.getShipmentEvents();

    expect(apiClientMocks.get).toHaveBeenNthCalledWith(1, '/api/demand-forecasts');
    expect(apiClientMocks.get).toHaveBeenNthCalledWith(2, '/api/shipment-events');
  });

  it('preserves write methods, routes and payloads', () => {
    const body = { companyName: 'Farm Market' };

    api.createCustomer(body);
    api.updateCustomer(12, body);
    api.deleteCustomer(12);

    expect(apiClientMocks.post).toHaveBeenCalledWith('/api/customers', body);
    expect(apiClientMocks.put).toHaveBeenCalledWith('/api/customers/12', body);
    expect(apiClientMocks.delete).toHaveBeenCalledWith('/api/customers/12');
  });

  it('passes customer status as query params and omits params without a status', () => {
    api.getCustomers('active');
    api.getCustomers();

    expect(apiClientMocks.get).toHaveBeenNthCalledWith(1, '/api/customers', {
      params: { status: 'active' },
    });
    expect(apiClientMocks.get).toHaveBeenNthCalledWith(2, '/api/customers', undefined);
  });
});
