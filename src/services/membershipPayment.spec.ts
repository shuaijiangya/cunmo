import { describe, expect, it, vi } from 'vitest';

import { createMembershipPayment } from './membershipPayment';

describe('membershipPayment', () => {
  it('polls the server until the paid order is authoritative', async () => {
    const requestPayment = vi.fn().mockResolvedValue(undefined);
    const getOrder = vi
      .fn()
      .mockResolvedValueOnce({ orderNo: 'M1', status: 'PAYING' })
      .mockResolvedValueOnce({ orderNo: 'M1', status: 'PAID' });
    const sleep = vi.fn().mockResolvedValue(undefined);
    const payment = createMembershipPayment({
      requestPayment,
      getOrder,
      sleep,
      maxPolls: 3
    });

    const result = await payment.pay({
      orderNo: 'M1',
      status: 'PAYING',
      paymentParams: {
        timeStamp: '1',
        nonceStr: 'nonce',
        packageValue: 'prepay_id=abc',
        signType: 'RSA',
        paySign: 'sign'
      }
    });

    expect(result.status).toBe('PAID');
    expect(getOrder).toHaveBeenCalledTimes(2);
  });

  it('returns cancelled when the user cancels requestPayment', async () => {
    const requestPayment = vi.fn().mockRejectedValue({
      errMsg: 'requestPayment:fail cancel'
    });
    const payment = createMembershipPayment({
      requestPayment,
      getOrder: vi.fn(),
      sleep: vi.fn(),
      maxPolls: 1
    });

    const result = await payment.pay({
      orderNo: 'M1',
      status: 'PAYING',
      paymentParams: {
        timeStamp: '1',
        nonceStr: 'nonce',
        packageValue: 'prepay_id=abc',
        signType: 'RSA',
        paySign: 'sign'
      }
    });

    expect(result.status).toBe('CANCELLED');
  });
});
