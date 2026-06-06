<script setup lang="ts">
import { computed, ref } from 'vue';
import { onLoad } from '@dcloudio/uni-app';

import { useInventoryStore } from '@/stores/inventoryStore';
import VisualCube from '@/components/VisualCube.vue';

const store = useInventoryStore();

const isFocused = computed(() => store.currentFilter.space !== 'all' || store.currentFilter.cate !== 'all');
const registryTitle = computed(() => (isFocused.value ? 'Focused Stock' : 'Global Registry'));
const registryCount = computed(() => (isFocused.value ? store.filteredItems.reduce((sum, item) => sum + item.count, 0) : store.totalCount));
const registryUnit = computed(() => (isFocused.value ? '个域内存量' : '件'));
const currentSpaceLabel = computed(() => store.spaces[store.currentFilter.space] ?? store.currentFilter.space);
const currentCateLabel = computed(() => {
  return store.activeCategories[store.currentFilter.cate] ?? store.spaceToCates.all[store.currentFilter.cate] ?? store.currentFilter.cate;
});

const headerStyle = ref<Record<string, string>>({});

const syncMenuButtonInset = () => {
  const wxApi = (globalThis as { wx?: WechatMiniprogram.Wx }).wx;
  if (!wxApi?.getMenuButtonBoundingClientRect || !wxApi.getWindowInfo) {
    headerStyle.value = {};
    return;
  }

  const menuRect = wxApi.getMenuButtonBoundingClientRect();
  const windowInfo = wxApi.getWindowInfo();
  const topGap = Math.max(menuRect.bottom + 16, windowInfo.statusBarHeight + 56);
  const rightGap = Math.max(windowInfo.windowWidth - menuRect.left + 12, 24);

  headerStyle.value = {
    paddingTop: `${topGap}px`,
    paddingRight: `${rightGap}px`
  };
};

onLoad(syncMenuButtonInset);
</script>

<template>
  <view class="safe-top shrink-0 border-b border-slate-100 bg-[#fafafa] px-6 pb-6" :style="headerStyle">
    <view class="mb-6 flex items-center justify-between">
      <view class="min-w-0 flex flex-1 items-center gap-2.5">
        <text class="text-lg font-bold tracking-tight text-slate-900">存量魔方</text>
        <text class="rounded-sm bg-slate-900 px-1.5 py-0.5 font-mono text-[20rpx] text-white">
          {{ store.cubeOrder }}阶矩阵
        </text>
      </view>
      <button
        class="m-0 flex shrink-0 items-center gap-1 whitespace-nowrap rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold leading-none text-white shadow-sm transition active:scale-95"
        @click="store.dispatch({ type: 'OPEN_MODAL', payload: { kind: 'item', itemContext: null } })"
      >
        <text class="text-base leading-none">+</text>
        <text>自由入库</text>
      </button>
    </view>

    <view class="flex items-center justify-between py-2">
      <view class="min-w-0 flex-1">
        <text class="text-[20rpx] font-bold uppercase tracking-widest text-slate-400">{{ registryTitle }}</text>
        <view class="mt-0.5 flex items-end gap-1 text-3xl font-light tracking-tight text-slate-900">
          <text>{{ registryCount }}</text>
          <text class="pb-1 text-sm font-normal text-slate-400">{{ registryUnit }}</text>
        </view>
        <view class="mt-1 inline-flex items-center rounded-sm bg-indigo-100 px-1 text-[22rpx] font-medium text-indigo-400">
          <text>当前视图：</text>
          <text>{{ currentSpaceLabel }}</text>
          <text class="px-0.5">➜</text>
          <text>{{ currentCateLabel }}</text>
        </view>
      </view>
      <view class="mr-2 shrink-0">
        <VisualCube :space="store.currentFilter.space" :cate="store.currentFilter.cate" :order="store.cubeOrder" />
      </view>
    </view>
  </view>
</template>
