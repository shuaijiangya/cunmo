<script setup lang="ts">
import { ref } from 'vue';

import { AuthError, wechatAuthService } from '@/services/wechatAuthService';
import { useInventoryStore } from '@/stores/inventoryStore';

const store = useInventoryStore();
const loading = ref(false);
const errorMessage = ref('');

/**
 * 发起微信登录并恢复用户原本准备执行的受保护操作。
 */
const login = async () => {
  if (loading.value) return;

  loading.value = true;
  errorMessage.value = '';
  store.startAuthentication();
  try {
    const session = await wechatAuthService.login();
    await store.completeLogin(session);
  } catch (error) {
    store.authenticationFailed();
    errorMessage.value =
      error instanceof AuthError ? error.message : '登录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <view class="login-overlay" @tap="store.cancelAuthentication()">
    <view class="login-sheet" @tap.stop>
      <view class="login-brand">
        <view class="login-cube">
          <view class="login-cube-cell is-dark" />
          <view class="login-cube-cell is-light" />
          <view class="login-cube-cell is-light" />
          <view class="login-cube-cell is-dark" />
        </view>
        <view class="login-title-group">
          <text class="login-title">登录存量魔方</text>
          <text class="login-subtitle">保存真实库存并开启云端同步</text>
        </view>
      </view>

      <view class="login-action-zone">
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
          <text>{{ loading ? '登录中...' : '微信授权登录' }}</text>
        </button>
        <button
          class="login-cancel"
          :disabled="loading"
          @click="store.cancelAuthentication()"
        >
          暂不登录，继续浏览
        </button>
        <text v-if="errorMessage" class="login-error">{{ errorMessage }}</text>
      </view>
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
  align-items: flex-end;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.45);
}

.login-sheet {
  width: 100%;
  border-radius: 40rpx 40rpx 0 0;
  background: #fff;
  padding: 52rpx 44rpx calc(48rpx + env(safe-area-inset-bottom));
}

.login-brand {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28rpx;
}

.login-cube {
  display: grid;
  width: 112rpx;
  height: 112rpx;
  grid-template-columns: repeat(2, 34rpx);
  grid-template-rows: repeat(2, 34rpx);
  place-content: center;
  gap: 10rpx;
  border: 6rpx solid #0f172a;
  border-radius: 26rpx;
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
  font-size: 38rpx;
  font-weight: 900;
  line-height: 1;
}

.login-subtitle {
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 600;
  line-height: 1.3;
}

.login-action-zone {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
  margin-top: 44rpx;
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
  color: #ef4444;
  font-size: 24rpx;
  text-align: center;
}

.login-cancel {
  width: 100%;
  margin: 0;
  background: transparent;
  color: #64748b;
  font-size: 26rpx;
  line-height: 1;
  padding: 24rpx;
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
