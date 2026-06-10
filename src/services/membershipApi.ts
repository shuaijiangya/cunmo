import type {
  MembershipDashboard,
  MembershipOrder,
  MembershipProductCode,
  UpgradeRequestInput,
  UpgradeRequestResponse
} from '@/types/membership';
import { authHttpClient, type RequestOptions } from './httpClient';

type RequestAdapter = <T>(options: RequestOptions) => Promise<T>;

export const createMembershipApi = (request: RequestAdapter) => ({
  getDashboard: () =>
    request<MembershipDashboard>({
      url: '/api/membership/dashboard',
      method: 'GET'
    }),

  createOrder: (
    productCode: MembershipProductCode,
    autoRenew: boolean
  ) =>
    request<MembershipOrder>({
      url: '/api/membership/orders',
      method: 'POST',
      data: { productCode, autoRenew }
    }),

  getOrder: (orderNo: string) =>
    request<MembershipOrder>({
      url: `/api/membership/orders/${encodeURIComponent(orderNo)}`,
      method: 'GET'
    }),

  createRenewalAgreement: () =>
    request<{
      contractCode: string;
      businessType: string;
      invokeParams: Record<string, string>;
    }>({
      url: '/api/membership/renewal-agreements',
      method: 'POST'
    }),

  terminateRenewalAgreement: () =>
    request<void>({
      url: '/api/membership/renewal-agreements/terminate',
      method: 'POST'
    }),

  submitUpgradeRequest: (data: UpgradeRequestInput) =>
    request<UpgradeRequestResponse>({
      url: '/api/membership/upgrade-requests',
      method: 'POST',
      data
    })
});

export const membershipApi = createMembershipApi((options) =>
  authHttpClient.request(options)
);
