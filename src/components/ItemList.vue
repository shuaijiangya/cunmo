<script setup lang="ts">
import { computed } from 'vue';

import EmptyCavityPanel from '@/components/EmptyCavityPanel.vue';
import ItemCard from '@/components/ItemCard.vue';
import { useInventoryStore } from '@/stores/inventoryStore';
import type { CategoryKey, Item } from '@/types/inventory';

const store = useInventoryStore();

const groups = computed(() => Object.entries(store.groupedFilteredItems) as [CategoryKey, Item[]][]);

const categoryLabel = (cate: CategoryKey) => {
  return store.activeCategories[cate] ?? store.spaceToCates.all[cate] ?? cate;
};

const creationSpaceFor = (items: Item[]) => {
  return store.currentFilter.space !== 'all' ? store.currentFilter.space : items[0]?.space ?? 'bedroom';
};

const loadMore = () => {
  if (store.hasMoreItems && !store.loadingMoreItems) {
    void store.loadItems(false);
  }
};
</script>

<template>
  <scroll-view
    scroll-y
    enable-flex
    :show-scrollbar="false"
    :lower-threshold="120"
    class="item-list-scroll bg-[#fcfcfc]"
    @scrolltolower="loadMore"
  >
    <view class="w-full space-y-6 p-6 item-list-content">
      <view v-if="store.itemError && store.filteredItems.length === 0" class="list-error">
        <text>{{ store.itemError }}</text>
        <button class="retry-button" @click="store.loadItems(true)">重新加载</button>
      </view>
      <view v-else-if="store.loadingMoreItems && store.filteredItems.length === 0" class="list-status">
        <text>正在读取魔方存量...</text>
      </view>
      <EmptyCavityPanel v-else-if="store.filteredItems.length === 0" :context="store.emptyCavityContext" />

      <view v-for="[cate, items] in groups" v-else :key="cate" class="space-y-2.5">
        <view class="flex items-center justify-between px-1">
          <text class="text-xs font-bold text-slate-800">{{ categoryLabel(cate) }}</text>
          <button
            class="m-0 flex items-center gap-1 rounded bg-slate-100 px-2 py-0.5 text-[20rpx] font-semibold leading-none text-slate-500"
            @click="
              store.dispatch({
                type: 'OPEN_MODAL',
                payload: { kind: 'item', itemContext: { space: creationSpaceFor(items), cate } }
              })
            "
          >
            <text>+</text>
            <text>创建物品</text>
          </button>
        </view>
        <ItemCard v-for="item in items" :key="item.id" :item="item" />
      </view>
      <view v-if="store.filteredItems.length && (store.loadingMoreItems || store.hasMoreItems)" class="list-status">
        <text>{{ store.loadingMoreItems ? '正在加载更多...' : '继续上滑加载更多' }}</text>
      </view>
    </view>
  </scroll-view>
</template>

<style scoped>
.item-list-scroll {
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.item-list-content {
  padding-bottom: calc(48rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(48rpx + env(safe-area-inset-bottom));
}

.list-status {
  display: flex;
  min-height: 80rpx;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 22rpx;
}

.list-error {
  display: flex;
  min-height: 300rpx;
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
