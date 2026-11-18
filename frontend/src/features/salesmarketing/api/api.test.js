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
  it('uses the /api/sales-marketing prefix and returns the shared client result', () => {
    const body = [{ forecastId: 7 }];
    apiClientMocks.get.mockReturnValue(body);

    expect(api.getDemandForecasts()).toBe(body);
    api.getShipmentEvents();

    expect(apiClientMocks.get).toHaveBeenNthCalledWith(1, '/api/sales-marketing/demand-forecasts');
    expect(apiClientMocks.get).toHaveBeenNthCalledWith(2, '/api/sales-marketing/shipment-events');
  });

  it('preserves write methods, routes and payloads', () => {
    const body = { companyName: 'Farm Market' };

    api.createCustomer(body);
    api.updateCustomer(12, body);
    api.deleteCustomer(12);

    expect(apiClientMocks.post).toHaveBeenCalledWith('/api/sales-marketing/customers', body);
    expect(apiClientMocks.put).toHaveBeenCalledWith('/api/sales-marketing/customers/12', body);
    expect(apiClientMocks.delete).toHaveBeenCalledWith('/api/sales-marketing/customers/12');
  });

  it('passes customer status as query params and omits params without a status', () => {
    api.getCustomers('active');
    api.getCustomers();

    expect(apiClientMocks.get).toHaveBeenNthCalledWith(1, '/api/sales-marketing/customers', {
      params: { status: 'active' },
    });
    expect(apiClientMocks.get).toHaveBeenNthCalledWith(2, '/api/sales-marketing/customers', undefined);
  });

  it('sends every call under /api/sales-marketing', () => {
    Object.values(api).forEach((call) => call(1, 2));

    const paths = ['get', 'post', 'put', 'delete'].flatMap((method) =>
      apiClientMocks[method].mock.calls.map(([path]) => path),
    );
    expect(paths.length).toBe(Object.keys(api).length);
    paths.forEach((path) => expect(path).toMatch(/^\/api\/sales-marketing\//));
  });
});
