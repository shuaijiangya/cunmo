<script setup lang="ts">
import { ref } from 'vue';

import { AuthError, wechatAuthService } from '@/services/wechatAuthService';
import { useInventoryStore } from '@/stores/inventoryStore';

const store = useInventoryStore();
const loading = ref(false);
const errorMessage = ref('');

const login = async () => {
  if (loading.value) return;

  loading.value = true;
  errorMessage.value = '';
  try {
    const session = await wechatAuthService.login();
    store.completeLogin(session);
  } catch (error) {
    errorMessage.value =
      error instanceof AuthError ? error.message : '登录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <view class="login-overlay">
    <view class="login-brand">
      <view class="login-cube">
        <view class="login-cube-cell is-dark" />
        <view class="login-cube-cell is-light" />
        <view class="login-cube-cell is-light" />
        <view class="login-cube-cell is-dark" />
      </view>
      <view class="login-title-group">
        <text class="login-title">存量魔方</text>
        <text class="login-subtitle">高维数字化资产节点</text>
      </view>
    </view>

    <view class="login-action-zone">
      <view class="login-copy">
        <text class="login-copy-title">授权极速登录</text>
        <text class="login-copy-subtitle">使用微信一键绑定，多设备云端秒级同步</text>
      </view>
      <button
        class="login-button"
        :disabled="loading"
        @click="login"
      >
        <view class="login-mark">
          <view class="login-peak is-small" />
          <view class="login-peak is-large" />
          <view class="login-ground" />
        </view>
        <text>{{ loading ? '授权拉取中...' : '微信手机号快捷登录' }}</text>
      </button>
      <text v-if="errorMessage" class="login-error">{{ errorMessage }}</text>
    </view>
  </view>
</template>

<style scoped>
.login-overlay {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
  background: #fff;
}

.login-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 236rpx 80rpx 0;
  gap: 52rpx;
}

.login-cube {
  display: grid;
  width: 188rpx;
  height: 188rpx;
  grid-template-columns: repeat(2, 58rpx);
  grid-template-rows: repeat(2, 58rpx);
  place-content: center;
  gap: 18rpx;
  border: 8rpx solid #0f172a;
  border-radius: 36rpx;
  background: #fff;
  box-shadow: 0 34rpx 80rpx rgba(15, 23, 42, 0.13);
  transform: rotate(12deg);
}

.login-cube-cell {
  border-radius: 15rpx;
}

.login-cube-cell.is-dark {
  background: #0f172a;
}

.login-cube-cell.is-light {
  background: #cbd5e1;
}

.login-title-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18rpx;
  text-align: center;
}

.login-title {
  color: #0f172a;
  font-size: 60rpx;
  font-weight: 900;
  line-height: 1;
}

.login-subtitle {
  color: #94a3b8;
  font-size: 28rpx;
  font-weight: 600;
  letter-spacing: 6rpx;
  line-height: 1.3;
}

.login-action-zone {
  display: flex;
  flex-direction: column;
  gap: 48rpx;
  padding: 0 44rpx calc(72rpx + constant(safe-area-inset-bottom));
  padding: 0 44rpx calc(72rpx + env(safe-area-inset-bottom));
}

.login-copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
  text-align: center;
}

.login-copy-title {
  color: #1e293b;
  font-size: 34rpx;
  font-weight: 800;
  line-height: 1.2;
}

.login-copy-subtitle {
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 500;
  line-height: 1.3;
}

.login-button {
  display: flex;
  width: 100%;
  height: 104rpx;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  margin: 0;
  border-radius: 24rpx;
  background: #07c160;
  box-shadow: 0 18rpx 38rpx rgba(34, 197, 94, 0.18);
  color: #fff;
  font-size: 32rpx;
  font-weight: 800;
  letter-spacing: 1rpx;
  line-height: 1;
}

.login-button:active {
  transform: scale(0.98);
}

.login-button[disabled] {
  opacity: 0.7;
}

.login-error {
  margin-top: -24rpx;
  color: #ef4444;
  font-size: 24rpx;
  text-align: center;
}

.login-mark {
  position: relative;
  width: 46rpx;
  height: 46rpx;
  overflow: hidden;
  border-radius: 999rpx;
  background: #fff;
}

.login-peak {
  position: absolute;
  bottom: 10rpx;
  width: 0;
  height: 0;
}

.login-peak.is-small {
  left: 8rpx;
  border-right: 11rpx solid transparent;
  border-bottom: 15rpx solid #07c160;
  border-left: 8rpx solid transparent;
}

.login-peak.is-large {
  right: 6rpx;
  border-right: 12rpx solid transparent;
  border-bottom: 22rpx solid #07c160;
  border-left: 13rpx solid transparent;
}

.login-ground {
  position: absolute;
  right: 7rpx;
  bottom: 9rpx;
  left: 7rpx;
  height: 6rpx;
  border-radius: 999rpx;
  background: #07c160;
}
</style>
