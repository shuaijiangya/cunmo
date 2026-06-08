<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import { AuthError } from '@/services/wechatAuthService';
import { userProfileService } from '@/services/userProfileService';
import { useInventoryStore } from '@/stores/inventoryStore';

const store = useInventoryStore();
const editing = ref(false);
const saving = ref(false);
const uploading = ref(false);
const loggingOut = ref(false);
const errorMessage = ref('');
const nickname = ref('');
const avatarUrl = ref('');
const displayName = computed(() => store.currentUser?.nickname || '微信用户');

/**
 * 使用当前用户资料初始化编辑表单。
 */
const startEditing = () => {
  nickname.value = store.currentUser?.nickname || '';
  avatarUrl.value = store.currentUser?.avatarUrl || '';
  editing.value = true;
};

onMounted(() => {
  if (store.currentUser && !store.currentUser.profileCompleted) {
    startEditing();
  }
});

/**
 * 上传微信头像选择器返回的临时图片。
 */
const chooseAvatar = async (event: { detail: { avatarUrl?: string } }) => {
  const temporaryPath = event.detail.avatarUrl;
  if (!temporaryPath || uploading.value) return;
  uploading.value = true;
  errorMessage.value = '';
  try {
    avatarUrl.value = await userProfileService.uploadAvatar(temporaryPath);
  } catch (error) {
    errorMessage.value =
      error instanceof AuthError ? error.message : '头像上传失败，请重试';
  } finally {
    uploading.value = false;
  }
};

/**
 * 校验并保存用户昵称和头像。
 */
const saveProfile = async () => {
  if (!store.currentUser || saving.value || uploading.value) return;
  const normalizedNickname = nickname.value.trim();
  if (!normalizedNickname || !avatarUrl.value) {
    errorMessage.value = '请选择头像并填写昵称';
    return;
  }
  saving.value = true;
  errorMessage.value = '';
  try {
    const user = await userProfileService.updateProfile({
      nickname: normalizedNickname,
      avatarUrl: avatarUrl.value
    });
    store.updateCurrentUser(user);
    editing.value = false;
  } catch (error) {
    errorMessage.value =
      error instanceof AuthError ? error.message : '资料保存失败，请重试';
  } finally {
    saving.value = false;
  }
};

/**
 * 二次确认后退出当前账号并切换回游客体验。
 */
const confirmLogout = () => {
  if (loggingOut.value) return;
  uni.showModal({
    title: '退出当前账号',
    content: '退出后将清除本机登录态，并回到示例数据浏览。',
    confirmText: '确认退出',
    confirmColor: '#ef4444',
    success: ({ confirm }) => {
      if (!confirm) return;
      loggingOut.value = true;
      void store.logout().finally(() => {
        loggingOut.value = false;
      });
    }
  });
};
</script>

<template>
  <view class="flex min-h-0 flex-1 flex-col overflow-hidden bg-[#f4f5f6]">
    <scroll-view scroll-y class="min-h-0 flex-1 overflow-y-auto">
      <view class="safe-top flex items-center gap-5 border-b border-slate-100 bg-white px-6 pb-8">
        <button
          class="profile-avatar-button"
          :open-type="editing ? 'chooseAvatar' : undefined"
          @chooseavatar="chooseAvatar"
        >
          <image v-if="avatarUrl || store.currentUser?.avatarUrl" class="profile-avatar-image" :src="avatarUrl || store.currentUser?.avatarUrl || ''" mode="aspectFill" />
          <view v-else class="profile-avatar-mark">
            <view />
            <view />
          </view>
        </button>
        <view class="min-w-0 flex-1">
          <input
            v-if="editing"
            v-model="nickname"
            type="nickname"
            class="profile-nickname-input"
            placeholder="填写微信昵称"
          />
          <text v-else class="block text-xl font-bold text-slate-900">{{ displayName }}</text>
        </view>
        <button
          class="m-0 shrink-0 rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold leading-none text-slate-500"
          @click="editing ? saveProfile() : startEditing()"
        >
          {{ editing ? '保存' : '编辑' }}
        </button>
      </view>
      <text v-if="uploading" class="profile-message">头像上传中...</text>
      <text v-else-if="errorMessage" class="profile-message is-error">{{ errorMessage }}</text>

      <view class="grid grid-cols-2 gap-3 px-6 py-4">
        <view class="min-w-0 rounded-xl border border-slate-100 bg-white p-4 shadow-sm">
          <text class="block text-[20rpx] font-bold uppercase text-slate-400">管理总资产</text>
          <text class="mt-1 block text-2xl font-black text-slate-900">{{ store.totalCount }}</text>
        </view>
        <view class="min-w-0 rounded-xl border border-slate-100 bg-white p-4 shadow-sm">
          <text class="block text-[20rpx] font-bold uppercase text-slate-400">创建魔方域</text>
          <text class="mt-1 block text-2xl font-black text-slate-900">{{ Object.keys(store.spaces).length - 1 }}</text>
        </view>
      </view>

      <view class="space-y-4 px-6 pb-6">
        <view class="overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm">
          <button class="m-0 flex w-full items-center justify-between border-b border-slate-50 bg-transparent px-4 py-3.5 text-sm font-medium leading-none text-slate-700">
            <text>⚙ 全局阈值设置</text>
          </button>
          <button class="m-0 flex w-full items-center justify-between border-b border-slate-50 bg-transparent px-4 py-3.5 text-sm font-medium leading-none text-slate-700">
            <text>☁ 手动拉取云端同步</text>
            <text class="rounded bg-slate-100 px-2 py-0.5 text-xs text-slate-400">Java 核心</text>
          </button>
        </view>
        <button
          class="mt-4 flex w-full items-center justify-center gap-2 rounded-2xl border border-slate-100 bg-white py-3.5 text-sm font-bold leading-none text-red-500 shadow-sm"
          :disabled="loggingOut"
          @click="confirmLogout"
        >
          <text>{{ loggingOut ? '正在退出...' : '退出当前账号' }}</text>
        </button>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
.profile-avatar-button {
  display: grid;
  width: 128rpx;
  height: 128rpx;
  flex-shrink: 0;
  place-items: center;
  margin: 0;
  padding: 0;
  overflow: hidden;
  border: 4rpx solid #f1f5f9;
  border-radius: 999rpx;
  background: #f1f5f9;
  box-shadow: 0 4rpx 12rpx rgba(15, 23, 42, 0.05);
}

.profile-avatar-image {
  width: 100%;
  height: 100%;
}

.profile-nickname-input {
  width: 100%;
  height: 64rpx;
  color: #0f172a;
  font-size: 34rpx;
  font-weight: 700;
}

.profile-avatar-mark {
  position: relative;
  width: 46rpx;
  height: 46rpx;
  color: #64748b;
}

.profile-avatar-mark view:first-child {
  position: absolute;
  left: 15rpx;
  top: 5rpx;
  width: 16rpx;
  height: 16rpx;
  border: 4rpx solid currentColor;
  border-radius: 999rpx;
  background: transparent;
}

.profile-avatar-mark view:last-child {
  position: absolute;
  left: 8rpx;
  bottom: 5rpx;
  width: 30rpx;
  height: 18rpx;
  border: 4rpx solid currentColor;
  border-bottom: 0;
  border-top-left-radius: 999rpx;
  border-top-right-radius: 999rpx;
}

.profile-message {
  display: block;
  padding: 16rpx 48rpx 0;
  background: #fff;
  color: #64748b;
  font-size: 24rpx;
}

.profile-message.is-error {
  color: #ef4444;
}
</style>
