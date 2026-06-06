<script setup lang="ts">
import { ref } from 'vue';

import { useInventoryStore } from '@/stores/inventoryStore';

const store = useInventoryStore();
const debounceTimer = ref<ReturnType<typeof setTimeout> | null>(null);

const onInput = (event: { detail: { value: string } }) => {
  const value = event.detail.value;
  store.dispatch({ type: 'SET_SEARCH', payload: value });
  if (debounceTimer.value) clearTimeout(debounceTimer.value);
  debounceTimer.value = setTimeout(() => {
    void store.searchItems(value);
  }, 350);
};

const clearSearch = () => {
  if (debounceTimer.value) clearTimeout(debounceTimer.value);
  void store.searchItems('');
};
</script>

<template>
  <view class="shrink-0 bg-white px-6 pt-4">
    <view
      class="relative flex items-center rounded-xl border border-slate-100 bg-slate-50 px-3 py-2 transition-all focus-within:border-slate-400 focus-within:bg-white focus-within:shadow-sm"
    >
      <text class="mr-2 text-sm text-slate-400">🔍</text>
      <input
        :value="store.searchQuery"
        class="h-6 flex-1 bg-transparent text-sm text-slate-800 placeholder-slate-400"
        placeholder="模糊搜索物品名、细分类、架位..."
        type="text"
        @input="onInput"
      />
      <button
        v-if="store.searchQuery"
        class="m-0 px-1 text-xs leading-none text-slate-300"
        @click="clearSearch"
      >
        ×
      </button>
    </view>
  </view>
</template>
