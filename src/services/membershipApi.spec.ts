import { describe, expect, it, vi } from 'vitest';

import { createMembershipApi } from './membershipApi';

describe('membershipApi', () => {
  it('creates an order with the selected product and renewal preference', async () => {
    const request = vi.fn().mockResolvedValue({});
    const api = createMembershipApi(request);

    await api.createOrder('MONTHLY_PRO', true);

    expect(request).toHaveBeenCalledWith({
      url: '/api/membership/orders',
      method: 'POST',
      data: {
        productCode: 'MONTHLY_PRO',
        autoRenew: true
      }
    });
  });

  it('submits an administrator upgrade request', async () => {
    const request = vi.fn().mockResolvedValue({ id: 7, status: 'PENDING' });
    const api = createMembershipApi(request);

    await api.submitUpgradeRequest({
      contact: '13800000000',
      remark: '希望管理员协助开通'
    });

    expect(request).toHaveBeenCalledWith({
      url: '/api/membership/upgrade-requests',
      method: 'POST',
      data: {
        contact: '13800000000',
        remark: '希望管理员协助开通'
      }
    });
  });
});
