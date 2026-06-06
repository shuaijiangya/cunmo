<script setup lang="ts">
import { useInventoryStore } from '@/stores/inventoryStore';
import type { CategoryKey, SpaceKey } from '@/types/inventory';

const store = useInventoryStore();

const entries = <T extends string>(record: Record<T, string>) => Object.entries(record) as [T, string][];

const manageSpace = (key: SpaceKey, label: string) => {
  if (key === 'all') return;
  store.openDeletionModal({
    scope: 'space',
    targetId: store.spaceIds[key],
    targetName: label
  });
};

const manageCategory = (key: CategoryKey, label: string) => {
  if (key === 'all' || store.currentFilter.space === 'all') return;
  store.openDeletionModal({
    scope: 'category',
    targetId: store.categoryIds[key],
    targetName: label,
    spaceId: store.spaceIds[store.currentFilter.space]
  });
};
</script>

<template>
  <view class="shrink-0 space-y-3 border-b border-slate-100 bg-white px-6 py-3">
    <view class="flex items-center gap-3 text-xs">
      <text class="shrink-0 font-medium text-slate-400">所属空间</text>
      <scroll-view scroll-x class="no-scrollbar min-w-0 flex-1 whitespace-nowrap">
        <view class="inline-flex items-center gap-1.5">
          <button
            v-for="[key, label] in entries<SpaceKey>(store.spaces)"
            :key="key"
            class="m-0 whitespace-nowrap rounded-md px-3 py-1 text-xs leading-none transition"
            :class="
              store.currentFilter.space === key
                ? 'bg-slate-900 font-medium text-white'
                : 'bg-slate-50 text-slate-600'
            "
            @click="store.setSpace(key)"
            @longpress="manageSpace(key, label)"
          >
            {{ label }}
          </button>
        </view>
      </scroll-view>
      <button
        v-if="store.currentFilter.space !== 'all'"
        class="m-0 bg-transparent px-1 text-sm leading-none text-slate-400"
        aria-label="管理当前空间"
        @click="
          manageSpace(
            store.currentFilter.space,
            store.spaces[store.currentFilter.space]
          )
        "
      >
        ⋯
      </button>
      <button
        class="m-0 bg-transparent px-1 text-base leading-none text-slate-400"
        @click="store.dispatch({ type: 'OPEN_MODAL', payload: { kind: 'space', itemContext: null } })"
      >
        +
      </button>
    </view>

    <view class="flex items-center gap-3 text-xs">
      <text class="shrink-0 font-medium text-slate-400">专属分类</text>
      <scroll-view scroll-x class="no-scrollbar min-w-0 flex-1 whitespace-nowrap">
        <view class="inline-flex items-center gap-1.5">
          <button
            v-for="[key, label] in entries<CategoryKey>(store.activeCategories)"
            :key="key"
            class="m-0 whitespace-nowrap rounded-md px-3 py-1 text-xs leading-none transition"
            :class="
              store.currentFilter.cate === key
                ? 'bg-slate-800 font-medium text-white'
                : 'bg-slate-50 text-slate-600'
            "
            @click="store.setCategory(key)"
            @longpress="manageCategory(key, label)"
          >
            {{ label }}
          </button>
        </view>
      </scroll-view>
      <button
        v-if="store.currentFilter.cate !== 'all'"
        class="m-0 bg-transparent px-1 text-sm leading-none text-slate-400"
        aria-label="管理当前分类"
        @click="
          manageCategory(
            store.currentFilter.cate,
            store.activeCategories[store.currentFilter.cate]
          )
        "
      >
        ⋯
      </button>
      <button
        class="m-0 bg-transparent px-1 text-base leading-none text-slate-400 disabled:opacity-35"
        :disabled="store.currentFilter.space === 'all'"
        @click="store.dispatch({ type: 'OPEN_MODAL', payload: { kind: 'category', itemContext: null } })"
      >
        +
      </button>
    </view>
  </view>
</template>
