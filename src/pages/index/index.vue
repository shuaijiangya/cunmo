<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';

import AxisTimeline from '@/components/AxisTimeline.vue';
import BottomNav from '@/components/BottomNav.vue';
import HeaderBar from '@/components/HeaderBar.vue';
import InventoryModals from '@/components/InventoryModals.vue';
import InventoryDeleteModal from '@/components/InventoryDeleteModal.vue';
import ItemList from '@/components/ItemList.vue';
import LensView from '@/components/LensView.vue';
import LoginOverlay from '@/components/LoginOverlay.vue';
import ProfileView from '@/components/ProfileView.vue';
import SearchBar from '@/components/SearchBar.vue';
import SpaceNavigator from '@/components/SpaceNavigator.vue';
import { setUnauthorizedHandler } from '@/services/httpClient';
import { wechatAuthService } from '@/services/wechatAuthService';
import { useInventoryStore } from '@/stores/inventoryStore';

const store = useInventoryStore();

/**
 * 初始化未授权回调，并按本地会话进入登录或游客模式。
 */
const initializePage = () => {
  setUnauthorizedHandler(() => store.handleUnauthorized());
  store.initializeExperience(wechatAuthService.restore());
};

/**
 * 页面销毁时移除全局未授权回调。
 */
const releasePage = () => {
  setUnauthorizedHandler(undefined);
};

onMounted(initializePage);
onUnmounted(releasePage);
</script>

<template>
  <view class="app-shell">
    <LoginOverlay v-if="store.loginVisible" />

    <HeaderBar v-if="store.activeView !== 'profile'" />
    <view
      v-if="store.activeView !== 'profile' && (!store.isLoggedIn || store.noticeMessage)"
      class="shrink-0 bg-amber-50 px-6 py-2 text-center text-[20rpx] font-medium text-amber-700"
    >
      <text>{{ store.noticeMessage || '当前展示体验数据，登录后管理你的真实库存' }}</text>
    </view>

    <view v-if="store.activeView === 'home'" class="main-shell">
      <view class="flex h-full min-h-0 flex-col overflow-hidden">
        <SearchBar />
        <SpaceNavigator />
        <view class="min-h-0 flex-1 overflow-hidden">
          <ItemList class="h-full" />
        </view>
      </view>
    </view>

    <view v-else-if="store.activeView === 'lens'" class="main-shell">
      <view class="h-full min-h-0 overflow-hidden">
        <LensView class="h-full" />
      </view>
    </view>

    <view v-else-if="store.activeView === 'axis'" class="main-shell">
      <view class="h-full min-h-0 overflow-hidden">
        <AxisTimeline class="h-full" />
      </view>
    </view>

    <view v-else class="main-shell">
      <ProfileView class="h-full min-h-0" />
    </view>

    <BottomNav />
    <InventoryModals />
    <InventoryDeleteModal />
  </view>
</template>
