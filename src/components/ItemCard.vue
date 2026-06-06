<script setup lang="ts">
import { computed, ref } from 'vue';

import { useInventoryStore } from '@/stores/inventoryStore';
import type { Item } from '@/types/inventory';

const props = defineProps<{
  item: Item;
}>();

const store = useInventoryStore();
const isWarn = computed(() => props.item.count <= props.item.minWarn);
const adjusting = ref(false);

const adjust = async (delta: number) => {
  if (adjusting.value || (delta < 0 && props.item.count <= 0)) return;
  adjusting.value = true;
  try {
    await store.adjustItemCount(props.item.id, delta);
  } catch (error) {
    uni.showToast({
      title: store.requestErrorMessage(error, '库存调整失败'),
      icon: 'none'
    });
  } finally {
    adjusting.value = false;
  }
};

const manageItem = () => {
  store.openDeletionModal({
    scope: 'item',
    targetId: props.item.id,
    targetName: props.item.name,
    spaceId: props.item.spaceId,
    categoryId: props.item.categoryId
  });
};
</script>

<template>
  <view
    class="flex items-center justify-between rounded-xl border border-slate-100 bg-white p-4 shadow-sm"
    :class="{ 'warn-active': isWarn }"
    @longpress="manageItem"
  >
    <view class="min-w-0 space-y-1">
      <text class="block truncate text-sm font-medium text-slate-900">{{ item.name }}</text>
      <text class="block truncate text-[20rpx] text-slate-400">
        {{ item.smallCate }} · {{ store.spaces[item.space] ?? item.space }} / {{ item.detailSpace }}
      </text>
    </view>
    <view class="ml-4 flex shrink-0 items-center gap-2">
      <button
        class="m-0 grid h-7 w-7 place-items-center rounded-md bg-transparent p-0 text-sm leading-none text-slate-400"
        aria-label="管理物品"
        @click.stop="manageItem"
      >
        ⋯
      </button>
      <view class="flex items-center gap-3 rounded-lg border border-slate-100 bg-slate-50 px-2 py-1">
      <button
        class="m-0 grid h-5 w-5 place-items-center bg-transparent p-0 text-slate-400"
        :disabled="adjusting || item.count <= 0"
        @click="adjust(-1)"
      >
        -
      </button>
      <text class="w-5 text-center text-xs font-bold" :class="isWarn ? 'text-red-500' : 'text-slate-900'">
        {{ item.count }}
      </text>
      <button
        class="m-0 grid h-5 w-5 place-items-center bg-transparent p-0 text-slate-400"
        :disabled="adjusting"
        @click="adjust(1)"
      >
        +
      </button>
      </view>
    </view>
  </view>
</template>
