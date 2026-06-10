export type MembershipPlanType =
  | 'FREE'
  | 'MONTHLY_PRO'
  | 'LIFETIME_PRO';

export type MembershipProductCode = 'MONTHLY_PRO' | 'LIFETIME_PRO';
export type RenewalStatus = 'NONE' | 'PENDING' | 'ACTIVE' | 'TERMINATED';
export type CapacityAlertStatus = 'WARNING' | 'LOCKED';
export type MembershipOrderStatus =
  | 'CREATED'
  | 'PAYING'
  | 'PAID'
  | 'CLOSED'
  | 'FAILED'
  | 'CANCELLED'
  | 'UNKNOWN';

export interface MembershipPlan {
  type: MembershipPlanType;
  displayName: string;
  effectiveAt: string | null;
  expiresAt: string | null;
}

export interface QuotaUsage {
  used: number;
  limit: number | null;
  percent: number;
  reached: boolean;
}

export interface CategoryQuotaUsage extends QuotaUsage {
  spaceId: number;
  spaceName: string;
}

export interface CapacityAlert {
  spaceId: number;
  spaceName: string;
  categoryId: number;
  categoryName: string;
  used: number;
  limit: number;
  percent: number;
  status: CapacityAlertStatus;
}

export interface MembershipQuota {
  rootSpaces: QuotaUsage;
  constrainedCategories: CategoryQuotaUsage[];
  itemCapacityLimit: number | null;
  capacityAlerts: CapacityAlert[];
}

export interface MembershipProduct {
  code: MembershipProductCode;
  name: string;
  priceFen: number;
  priceText: string;
  durationDays: number | null;
  recommended: boolean;
}

export interface RenewalSummary {
  supported: boolean;
  enabled: boolean;
  status: RenewalStatus;
  nextChargeAt: string | null;
}

export interface MembershipContact {
  customerServiceEnabled: boolean;
  enterpriseWechatQrUrl: string | null;
  phone: string | null;
}

export interface MembershipDashboard {
  plan: MembershipPlan;
  quota: MembershipQuota;
  products: MembershipProduct[];
  renewal: RenewalSummary;
  contact: MembershipContact;
}

export interface PaymentParams {
  timeStamp: string;
  nonceStr: string;
  packageValue: string;
  signType: 'RSA';
  paySign: string;
}

export interface MembershipOrder {
  orderNo: string;
  status: MembershipOrderStatus;
  paymentParams?: PaymentParams | null;
}

export interface UpgradeRequestInput {
  contact: string;
  remark: string;
}

export interface UpgradeRequestResponse {
  id: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
}
