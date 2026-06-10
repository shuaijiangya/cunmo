import type {
  MembershipOrder,
  MembershipOrderStatus,
  PaymentParams
} from '@/types/membership';

interface PaymentDependencies {
  requestPayment: (params: {
    timeStamp: string;
    nonceStr: string;
    package: string;
    signType: 'RSA';
    paySign: string;
  }) => Promise<unknown>;
  getOrder: (orderNo: string) => Promise<MembershipOrder>;
  sleep?: (milliseconds: number) => Promise<void>;
  maxPolls?: number;
}

const defaultSleep = (milliseconds: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, milliseconds));

const isCancelled = (error: unknown) =>
  String((error as { errMsg?: string })?.errMsg ?? error)
    .toLowerCase()
    .includes('cancel');

const requestPaymentParams = (params: PaymentParams) => ({
  timeStamp: params.timeStamp,
  nonceStr: params.nonceStr,
  package: params.packageValue,
  signType: params.signType,
  paySign: params.paySign
});

export const createMembershipPayment = ({
  requestPayment,
  getOrder,
  sleep = defaultSleep,
  maxPolls = 6
}: PaymentDependencies) => ({
  async pay(order: MembershipOrder): Promise<MembershipOrder> {
    if (!order.paymentParams) {
      return { ...order, status: 'FAILED' };
    }
    try {
      await requestPayment(requestPaymentParams(order.paymentParams));
    } catch (error) {
      if (isCancelled(error)) {
        return { ...order, status: 'CANCELLED' };
      }
      throw error;
    }

    let latest: MembershipOrder = order;
    for (let index = 0; index < maxPolls; index += 1) {
      latest = await getOrder(order.orderNo);
      if (latest.status === 'PAID' || isTerminal(latest.status)) {
        return latest;
      }
      if (index < maxPolls - 1) await sleep(1000);
    }
    return { ...latest, status: 'UNKNOWN' };
  }
});

const isTerminal = (status: MembershipOrderStatus) =>
  status === 'CLOSED' || status === 'FAILED' || status === 'CANCELLED';

export const membershipPayment = createMembershipPayment({
  requestPayment: (params) =>
    new Promise((resolve, reject) => {
      uni.requestPayment({
        ...params,
        success: resolve,
        fail: reject
      });
    }),
  getOrder: (orderNo) =>
    import('./membershipApi').then(({ membershipApi }) =>
      membershipApi.getOrder(orderNo)
    )
});
