<script setup lang="ts">
import { useInventoryStore } from '@/stores/inventoryStore';
import type { LensTab } from '@/types/inventory';

const store = useInventoryStore();

const tabs: Array<{ key: LensTab; label: string }> = [
  { key: 'space', label: '按空间切割' },
  { key: 'cate', label: '按分类切割' }
];

const loadMore = () => {
  if (store.hasMoreAnalytics && !store.loadingMoreAnalytics) {
    void store.loadAnalytics(false);
  }
};
</script>

<template>
  <view class="lens-shell bg-[#fcfcfc]">
    <view class="flex shrink-0 justify-center border-b border-slate-100 bg-white px-6 py-6">
      <view class="flex w-full max-w-xs gap-1 rounded-lg bg-slate-100 p-0.5 text-xs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="m-0 flex-1 rounded-md py-1.5 text-center font-medium leading-none transition"
          :class="store.currentLensTab === tab.key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500'"
          @click="store.dispatch({ type: 'SET_LENS_TAB', payload: tab.key })"
        >
          {{ tab.label }}
        </button>
      </view>
    </view>

    <scroll-view
      scroll-y
      enable-flex
      :show-scrollbar="false"
      :lower-threshold="120"
      class="lens-scroll"
      @scrolltolower="loadMore"
    >
      <view class="lens-content w-full space-y-6 p-6">
        <view v-if="store.analyticsError && !store.lensStats.length" class="view-error">
          <text>{{ store.analyticsError }}</text>
          <button class="retry-button" @click="store.loadAnalytics(true)">重新加载</button>
        </view>
        <view v-for="stat in store.lensStats" :key="stat.key" class="w-full space-y-2 rounded-xl border border-slate-100 bg-white p-4">
          <view class="flex min-w-0 justify-between gap-3 text-xs">
            <text class="min-w-0 flex-1 truncate font-bold text-slate-900">{{ stat.label }}</text>
            <text class="shrink-0 text-slate-500">{{ stat.count }}件 ({{ stat.percent }}%)</text>
          </view>
          <view class="h-2 w-full rounded-full bg-slate-50">
            <view
              class="h-full rounded-full transition-all"
              :class="store.currentLensTab === 'space' ? 'bg-slate-900' : 'bg-indigo-600'"
              :style="{ width: `${stat.percent}%` }"
            />
          </view>
        </view>
        <view v-if="store.loadingMoreAnalytics || store.hasMoreAnalytics" class="load-more-status">
          <text>{{ store.loadingMoreAnalytics ? '正在加载...' : '继续上滑加载更多' }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
.lens-shell {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.lens-scroll {
  width: 100%;
  min-height: 0;
  flex: 1 1 0%;
  overflow: hidden;
}

.lens-content {
  padding-bottom: calc(48rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
}

.load-more-status {
  display: flex;
  min-height: 64rpx;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 22rpx;
}

.view-error {
  display: flex;
  min-height: 320rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24rpx;
  color: #64748b;
  font-size: 24rpx;
  text-align: center;
}

.retry-button {
  margin: 0;
  border-radius: 16rpx;
  background: #0f172a;
  color: #fff;
  font-size: 22rpx;
  line-height: 1;
  padding: 20rpx 30rpx;
}
</style>
