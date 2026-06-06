<script setup lang="ts">
import { onMounted } from 'vue';

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
import { wechatAuthService } from '@/services/wechatAuthService';
import { useInventoryStore } from '@/stores/inventoryStore';

const store = useInventoryStore();

onMounted(() => {
  store.restoreAuth(wechatAuthService.restore());
});
</script>

<template>
  <view class="app-shell">
    <LoginOverlay v-if="!store.isLoggedIn" />

    <HeaderBar v-if="store.activeView !== 'profile'" />

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
