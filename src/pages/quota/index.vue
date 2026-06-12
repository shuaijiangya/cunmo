<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import { membershipApi } from '@/services/membershipApi';
import { membershipPayment } from '@/services/membershipPayment';
import type {
  MembershipDashboard,
  MembershipProductCode
} from '@/types/membership';

const dashboard = ref<MembershipDashboard | null>(null);
const loading = ref(true);
const busy = ref(false);
const errorMessage = ref('');
const selectedProduct = ref<MembershipProductCode>('MONTHLY_PRO');
const autoRenew = ref(false);

const currentPlan = computed(
  () => dashboard.value?.plan.displayName ?? '免费基础版'
);
const planTag = computed(() =>
  dashboard.value?.plan.type === 'FREE' ? 'FREE' : 'PRO'
);
const planDescription = computed(() => {
  if (dashboard.value?.plan.type === 'LIFETIME_PRO') {
    return '永久释放全部魔方维度与腔体容量。';
  }
  if (dashboard.value?.plan.type === 'MONTHLY_PRO') {
    return dashboard.value.plan.expiresAt
      ? `有效期至 ${formatDate(dashboard.value.plan.expiresAt)}`
      : '月度 PRO 已生效。';
  }
  return '受限于三维基础矩阵，适合轻量盘点。';
});

const selected = computed(() =>
  dashboard.value?.products.find(
    (product) => product.code === selectedProduct.value
  )
);

const loadDashboard = async () => {
  loading.value = true;
  errorMessage.value = '';
  try {
    dashboard.value = await membershipApi.getDashboard();
    autoRenew.value =
      dashboard.value.renewal.enabled ||
      dashboard.value.renewal.status === 'PENDING';
  } catch {
    errorMessage.value = '容量与配额加载失败，请重试';
  } finally {
    loading.value = false;
  }
};

const goBack = () => uni.navigateBack();

const buy = async () => {
  if (busy.value) return;
  busy.value = true;
  errorMessage.value = '';
  try {
    const order = await membershipApi.createOrder(
      selectedProduct.value,
      selectedProduct.value === 'MONTHLY_PRO' && autoRenew.value
    );
    const result = await membershipPayment.pay(order);
    if (result.status === 'PAID') {
      uni.showToast({ title: 'PRO 已生效', icon: 'success' });
      if (
        selectedProduct.value === 'MONTHLY_PRO' &&
        autoRenew.value &&
        dashboard.value?.renewal.status !== 'ACTIVE' &&
        dashboard.value?.renewal.status !== 'PENDING'
      ) {
        try {
          await startRenewalAgreement();
        } catch {
          uni.showModal({
            title: '月卡已生效',
            content: '自动续费签约未完成，当前仍可手动续费。',
            showCancel: false
          });
        }
      }
      await loadDashboard();
    } else if (result.status !== 'CANCELLED') {
      uni.showModal({
        title: '支付结果确认中',
        content: '订单结果尚未同步，请稍后刷新页面。',
        showCancel: false
      });
    }
  } catch {
    errorMessage.value = '创建支付订单失败，请稍后重试';
  } finally {
    busy.value = false;
  }
};

const toggleRenewal = async () => {
  if (!dashboard.value?.renewal.supported || busy.value) return;
  busy.value = true;
  try {
    if (
      dashboard.value.renewal.status === 'ACTIVE' ||
      dashboard.value.renewal.status === 'PENDING'
    ) {
      await membershipApi.terminateRenewalAgreement();
      autoRenew.value = false;
      uni.showToast({ title: '已关闭自动续费', icon: 'none' });
    } else if (dashboard.value.plan.type === 'MONTHLY_PRO') {
      await startRenewalAgreement();
      autoRenew.value = true;
      uni.showToast({ title: '签约请求已提交', icon: 'none' });
    } else {
      autoRenew.value = !autoRenew.value;
    }
    if (dashboard.value.plan.type === 'MONTHLY_PRO') {
      await loadDashboard();
    }
  } catch {
    errorMessage.value = '自动续费操作未完成，请稍后重试';
  } finally {
    busy.value = false;
  }
};

const startRenewalAgreement = async () => {
  const agreement = await membershipApi.createRenewalAgreement();
  await openRenewalContract(
    agreement.appId,
    agreement.path,
    agreement.extraData
  );
};

const openRenewalContract = (
  appId: string,
  path: string,
  extraData: Record<string, string>
) =>
  new Promise<void>((resolve, reject) => {
    uni.navigateToMiniProgram({
      appId,
      path,
      extraData,
      envVersion: 'release',
      success: resolve,
      fail: reject
    });
  });

const submitUpgradeRequest = () => {
  uni.showModal({
    title: '提交升级申请',
    content: '',
    editable: true,
    placeholderText: '请输入手机号、微信号或邮箱',
    confirmText: '下一步',
    success: ({ confirm, content }) => {
      if (!confirm || !content?.trim()) return;
      askRemark(content.trim());
    }
  });
};

const askRemark = (contact: string) => {
  uni.showModal({
    title: '升级说明',
    content: '',
    editable: true,
    placeholderText: '可填写期望套餐或使用场景',
    confirmText: '提交',
    success: async ({ confirm, content }) => {
      if (!confirm) return;
      busy.value = true;
      try {
        await membershipApi.submitUpgradeRequest({
          contact,
          remark: content?.trim() ?? ''
        });
        uni.showToast({ title: '申请已提交', icon: 'success' });
      } catch {
        errorMessage.value = '升级申请提交失败，请稍后重试';
      } finally {
        busy.value = false;
      }
    }
  });
};

const contactAdmin = () => {
  if (!dashboard.value) return;
  const actions: string[] = [];
  if (dashboard.value.contact.enterpriseWechatQrUrl) {
    actions.push('查看企业微信二维码');
  }
  if (dashboard.value.contact.phone) actions.push('拨打管理员电话');
  if (!actions.length) {
    uni.showToast({ title: '暂未配置管理员联系方式', icon: 'none' });
    return;
  }
  uni.showActionSheet({
    itemList: actions,
    success: ({ tapIndex }) => {
      const action = actions[tapIndex];
      if (action === '查看企业微信二维码') {
        uni.previewImage({
          urls: [dashboard.value!.contact.enterpriseWechatQrUrl!]
        });
      } else {
        uni.makePhoneCall({ phoneNumber: dashboard.value!.contact.phone! });
      }
    }
  });
};

const formatDate = (value: string) =>
  new Date(value).toLocaleDateString('zh-CN');

onMounted(loadDashboard);
</script>

<template>
  <view class="quota-page">
    <view class="quota-nav safe-top">
      <button class="back-button" @click="goBack">
        <text class="back-mark">‹</text>
        <text>返回</text>
      </button>
      <text class="nav-title">容量与配额</text>
      <view class="nav-spacer" />
    </view>

    <view v-if="loading" class="state-panel">
      <text>正在读取魔方容量...</text>
    </view>
    <view v-else-if="!dashboard" class="state-panel">
      <text class="state-error">{{ errorMessage }}</text>
      <button class="retry-button" @click="loadDashboard">重新加载</button>
    </view>

    <scroll-view v-else scroll-y class="quota-scroll">
      <view class="plan-hero">
        <view class="hero-orbit" />
        <view class="hero-content">
          <view>
            <text class="eyebrow">CURRENT PLAN</text>
            <view class="plan-title-row">
              <text class="plan-title">{{ currentPlan }}</text>
              <text class="plan-tag">{{ planTag }}</text>
            </view>
            <text class="plan-description">{{ planDescription }}</text>
          </view>
          <view class="cube-badge">🧊</view>
        </view>
      </view>

      <view class="dashboard-content">
        <view v-if="errorMessage" class="inline-error">{{ errorMessage }}</view>

        <view class="quota-card">
          <view class="section-title">
            <text class="section-icon structure-icon">◱</text>
            <text>魔方结构限额</text>
          </view>

          <view class="metric">
            <view class="metric-line">
              <text class="metric-label">根空间大分类</text>
              <text class="metric-value">
                {{ dashboard.quota.rootSpaces.used }}
                <text class="metric-limit">
                  / {{ dashboard.quota.rootSpaces.limit ?? '∞' }}
                </text>
              </text>
            </view>
            <view class="progress-track">
              <view
                class="progress-fill"
                :style="{ width: `${dashboard.quota.rootSpaces.percent}%` }"
              />
            </view>
          </view>

          <view
            v-for="space in dashboard.quota.constrainedCategories"
            :key="space.spaceId"
            class="metric metric-separated"
          >
            <view class="metric-line">
              <view>
                <text class="metric-label">{{ space.spaceName }} · 子腔体小分类</text>
                <text v-if="space.reached" class="restricted-badge">受限</text>
              </view>
              <text class="metric-value" :class="{ danger: space.reached }">
                {{ space.used }}
                <text class="metric-limit">/ {{ space.limit ?? '∞' }}</text>
              </text>
            </view>
            <view class="progress-track">
              <view
                class="progress-fill"
                :class="{ dangerFill: space.reached }"
                :style="{ width: `${space.percent}%` }"
              />
            </view>
            <text v-if="space.reached" class="metric-hint">
              {{ space.spaceName }}空间已无法切割新的分类腔体。
            </text>
          </view>
        </view>

        <view class="quota-card">
          <view class="section-title">
            <text class="section-icon radar-icon">⚠</text>
            <text>腔体爆仓雷达</text>
          </view>
          <text class="radar-description">
            免费版单分类腔体最多容纳
            {{ dashboard.quota.itemCapacityLimit ?? '无限' }} 条物品记录。
          </text>
          <view v-if="!dashboard.quota.capacityAlerts.length" class="empty-radar">
            当前没有即将满载的腔体
          </view>
          <view
            v-for="alert in dashboard.quota.capacityAlerts"
            :key="`${alert.spaceId}-${alert.categoryId}`"
            class="radar-item"
            :class="{ locked: alert.status === 'LOCKED' }"
          >
            <view>
              <text class="radar-name">{{ alert.categoryName }}</text>
              <text class="radar-space">所属：{{ alert.spaceName }}</text>
            </view>
            <view class="radar-right">
              <text class="radar-count">
                {{ alert.used }} / {{ alert.limit }}
              </text>
              <text class="radar-action">
                {{ alert.status === 'LOCKED' ? '已锁定' : '去清理' }}
              </text>
            </view>
          </view>
        </view>

        <view class="pro-card">
          <view class="shimmer-effect" />
          <view class="pro-content">
            <view class="pro-heading">
              <text class="pro-title">👑 魔方 PRO</text>
              <text class="unlimited-tag">无限空间</text>
            </view>
            <view class="benefits">
              <text>✓ 无限扩展根空间与分类维度</text>
              <text>✓ 单个腔体无限存量，告别爆仓</text>
              <text>✓ 开启云端同步与多设备对账</text>
              <text>✓ 解锁全景透视镜与导出报表</text>
            </view>
            <view class="product-grid">
              <button
                v-for="product in dashboard.products"
                :key="product.code"
                class="product-option"
                :class="{ selected: selectedProduct === product.code }"
                @click="selectedProduct = product.code"
              >
                <text v-if="product.recommended" class="recommend-tag">灵活订阅</text>
                <text v-else class="recommend-tag saving">一次买断</text>
                <text class="product-name">{{ product.name }}</text>
                <text class="product-price">{{ product.priceText }}</text>
              </button>
            </view>
            <button
              v-if="selectedProduct === 'MONTHLY_PRO' && dashboard.renewal.supported"
              class="renewal-row"
              @click="toggleRenewal"
            >
              <text>到期自动续费，可随时关闭</text>
              <view class="switch" :class="{ enabled: autoRenew }">
                <view />
              </view>
            </button>
            <text
              v-else-if="selectedProduct === 'MONTHLY_PRO'"
              class="renewal-unavailable"
            >
              当前商户暂未开放自动续费，可手动续费
            </text>
            <button class="buy-button" :disabled="busy" @click="buy">
              {{ busy ? '正在处理...' : `立即购买 ${selected?.priceText ?? ''}` }}
            </button>
          </view>
        </view>

        <view class="admin-card">
          <view class="admin-heading">
            <text>需要管理员协助？</text>
            <text>💬</text>
          </view>
          <text class="admin-description">
            可提交升级申请，也可通过微信客服、企业微信二维码或电话直接联系管理员。
          </text>
          <view class="admin-actions">
            <button class="apply-button" @click="submitUpgradeRequest">
              提交升级申请
            </button>
            <button class="contact-button" @click="contactAdmin">
              联系管理员
            </button>
          </view>
          <button
            v-if="dashboard.contact.customerServiceEnabled"
            class="customer-service"
            open-type="contact"
          >
            打开微信客服
          </button>
        </view>
        <text class="fine-print">
          支付即表示同意《会员服务协议》；自动续费将在到期前提醒并按约定扣款。
        </text>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
.quota-page {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  overflow: hidden;
  background: #f8fafc;
  color: #1e293b;
}
.quota-nav {
  display: grid;
  z-index: 20;
  flex-shrink: 0;
  grid-template-columns: 120rpx 1fr 120rpx;
  align-items: end;
  padding-right: 36rpx;
  padding-bottom: 24rpx;
  padding-left: 36rpx;
  border-bottom: 1rpx solid #f1f5f9;
  background: #fff;
}
.back-button {
  display: flex;
  align-items: center;
  margin: 0;
  padding: 0;
  background: transparent;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 1;
}
.back-mark { margin-right: 6rpx; font-size: 42rpx; font-weight: 400; }
.nav-title { text-align: center; color: #0f172a; font-size: 30rpx; font-weight: 800; }
.nav-spacer { width: 120rpx; }
.quota-scroll { min-height: 0; flex: 1; }
.plan-hero {
  position: relative;
  overflow: hidden;
  padding: 58rpx 44rpx 96rpx;
  border-radius: 0 0 68rpx 68rpx;
  background: radial-gradient(circle at 90% 0%, rgba(99,102,241,.30), transparent 34%), linear-gradient(145deg,#111827,#0f172a 58%,#1e1b4b);
  box-shadow: 0 20rpx 44rpx rgba(15,23,42,.20);
}
.hero-orbit { position:absolute; right:-70rpx; top:-84rpx; width:240rpx; height:240rpx; border:34rpx solid rgba(255,255,255,.035); border-radius:50%; }
.hero-content { position:relative; z-index:1; display:flex; justify-content:space-between; gap:24rpx; }
.eyebrow { color:#94a3b8; font-size:18rpx; font-weight:800; letter-spacing:4rpx; }
.plan-title-row { display:flex; align-items:center; margin-top:10rpx; }
.plan-title { color:#fff; font-size:44rpx; font-weight:900; }
.plan-tag { margin-left:14rpx; padding:5rpx 12rpx; border-radius:10rpx; background:#334155; color:#cbd5e1; font-size:17rpx; font-weight:800; }
.plan-description { display:block; margin-top:15rpx; color:#94a3b8; font-size:22rpx; }
.cube-badge { display:flex; width:94rpx; height:94rpx; flex-shrink:0; align-items:center; justify-content:center; border:1rpx solid #475569; border-radius:26rpx; background:rgba(30,41,59,.75); box-shadow:inset 0 2rpx 12rpx rgba(255,255,255,.06); font-size:48rpx; }
.dashboard-content { position:relative; z-index:2; margin-top:-50rpx; padding:0 36rpx 52rpx; }
.quota-card { margin-bottom:28rpx; padding:34rpx; border:1rpx solid #eef2f7; border-radius:30rpx; background:#fff; box-shadow:0 10rpx 32rpx rgba(15,23,42,.055); }
.section-title { display:flex; align-items:center; margin-bottom:28rpx; color:#0f172a; font-size:26rpx; font-weight:800; }
.section-icon { margin-right:12rpx; font-size:30rpx; }.structure-icon{color:#6366f1}.radar-icon{color:#f59e0b}
.metric-separated { margin-top:28rpx; padding-top:26rpx; border-top:1rpx dashed #f1f5f9; }
.metric-line { display:flex; justify-content:space-between; align-items:center; margin-bottom:14rpx; }
.metric-label { color:#64748b; font-size:22rpx; font-weight:600; }
.metric-value { color:#0f172a; font-size:22rpx; font-weight:900; }.metric-limit{color:#94a3b8;font-weight:500}.danger{color:#ef4444}
.restricted-badge { margin-left:8rpx; padding:3rpx 8rpx; border-radius:7rpx; background:#fef2f2; color:#ef4444; font-size:17rpx; }
.progress-track { height:14rpx; overflow:hidden; border-radius:999rpx; background:#f1f5f9; }
.progress-fill { height:100%; border-radius:inherit; background:#111827; transition:width .3s; }.dangerFill{background:linear-gradient(90deg,#fb7185,#ef4444)}
.metric-hint { display:block; margin-top:12rpx; color:#94a3b8; font-size:18rpx; }
.radar-description { display:block; margin-top:-12rpx; margin-bottom:20rpx; color:#94a3b8; font-size:18rpx; }
.radar-item { display:flex; justify-content:space-between; align-items:center; margin-top:14rpx; padding:22rpx; border:1rpx solid #f1f5f9; border-radius:20rpx; background:#f8fafc; }.radar-item.locked{border-left:5rpx solid #ef4444}
.radar-name { display:block; color:#334155; font-size:22rpx; font-weight:800; }.radar-space{display:block;margin-top:7rpx;color:#94a3b8;font-size:18rpx}.radar-right{text-align:right}.radar-count{display:block;color:#d97706;font-size:22rpx;font-weight:900}.radar-action{display:block;margin-top:6rpx;color:#64748b;font-size:18rpx;text-decoration:underline}.locked .radar-count,.locked .radar-action{color:#ef4444;text-decoration:none}
.empty-radar { padding:26rpx; border-radius:20rpx; background:#f8fafc; color:#94a3b8; text-align:center; font-size:20rpx; }
.pro-card { position:relative; overflow:hidden; padding:38rpx; border-radius:42rpx; background:linear-gradient(140deg,#312e81 0%,#581c87 48%,#111827 100%); box-shadow:0 28rpx 56rpx rgba(67,56,202,.25); color:#fff; }
@keyframes shimmer { 0%{transform:translateX(-100%)} 100%{transform:translateX(100%)} }
.shimmer-effect { position:absolute; inset:0; background:linear-gradient(90deg,transparent,rgba(255,255,255,.2),transparent); animation:shimmer 2.5s infinite; }
.pro-content{position:relative;z-index:1}.pro-heading{display:flex;justify-content:space-between;align-items:center}.pro-title{font-size:34rpx;font-weight:900}.unlimited-tag{padding:9rpx 15rpx;border-radius:14rpx;background:rgba(255,255,255,.11);font-size:19rpx}
.benefits{display:flex;flex-direction:column;gap:14rpx;margin:28rpx 0;color:#e0e7ff;font-size:21rpx}.benefits text::first-letter{color:#34d399}
.product-grid{display:grid;grid-template-columns:1fr 1fr;gap:16rpx}.product-option{position:relative;display:flex;min-height:142rpx;flex-direction:column;align-items:flex-start;margin:0;padding:24rpx;border:1rpx solid rgba(255,255,255,.17);border-radius:22rpx;background:rgba(255,255,255,.07);color:#fff;text-align:left}.product-option.selected{border-color:#fff;background:rgba(255,255,255,.15);box-shadow:0 0 0 1rpx rgba(255,255,255,.25)}.recommend-tag{position:absolute;right:12rpx;top:-13rpx;padding:4rpx 10rpx;border-radius:14rpx;background:#fbbf24;color:#713f12;font-size:15rpx;font-weight:900}.recommend-tag.saving{background:#a7f3d0;color:#065f46}.product-name{color:#c7d2fe;font-size:20rpx}.product-price{margin-top:12rpx;font-size:28rpx;font-weight:900}
.renewal-row{display:flex;justify-content:space-between;align-items:center;width:100%;margin:20rpx 0;padding:18rpx 22rpx;border-radius:18rpx;background:rgba(15,23,42,.28);color:#e0e7ff;font-size:19rpx}.switch{width:58rpx;height:34rpx;padding:4rpx;border-radius:999rpx;background:#64748b}.switch view{width:26rpx;height:26rpx;border-radius:50%;background:#fff;transition:transform .2s}.switch.enabled{background:#10b981}.switch.enabled view{transform:translateX(24rpx)}.renewal-unavailable{display:block;margin:18rpx 0;color:#c7d2fe;font-size:18rpx;text-align:center}
.buy-button{width:100%;margin:0;padding:24rpx;border-radius:22rpx;background:#fff;color:#0f172a;font-size:24rpx;font-weight:900;box-shadow:0 12rpx 28rpx rgba(15,23,42,.20)}
.admin-card{margin-top:28rpx;padding:30rpx;border:1rpx solid #e0e7ff;border-radius:30rpx;background:linear-gradient(135deg,#fff,#f5f3ff)}.admin-heading{display:flex;justify-content:space-between;color:#312e81;font-size:24rpx;font-weight:900}.admin-description{display:block;margin-top:10rpx;color:#64748b;font-size:19rpx;line-height:1.6}.admin-actions{display:grid;grid-template-columns:1fr 1fr;gap:14rpx;margin-top:22rpx}.admin-actions button{margin:0;padding:19rpx 8rpx;border-radius:18rpx;font-size:20rpx;font-weight:800}.apply-button{background:#4f46e5;color:#fff}.contact-button{border:1rpx solid #c7d2fe;background:#fff;color:#4338ca}.customer-service{margin:14rpx 0 0;padding:18rpx;border:1rpx solid #c7d2fe;border-radius:18rpx;background:#eef2ff;color:#4338ca;font-size:20rpx;font-weight:800}
.fine-print{display:block;padding:18rpx 26rpx;color:#94a3b8;text-align:center;font-size:17rpx;line-height:1.5}.inline-error{margin-bottom:18rpx;padding:18rpx;border-radius:16rpx;background:#fef2f2;color:#ef4444;font-size:20rpx}.state-panel{display:flex;min-height:60vh;flex-direction:column;align-items:center;justify-content:center;gap:24rpx;color:#64748b}.state-error{color:#ef4444}.retry-button{padding:18rpx 32rpx;border-radius:18rpx;background:#0f172a;color:#fff;font-size:22rpx}
</style>
