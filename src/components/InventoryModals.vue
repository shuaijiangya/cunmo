<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';

import { useInventoryStore } from '@/stores/inventoryStore';
import type { CategoryKey, ItemInput, SpaceKey } from '@/types/inventory';

const store = useInventoryStore();

const spaceForm = reactive({ name: '' });
const categoryForm = reactive({ name: '' });
const selectedCategorySpaces = ref<SpaceKey[]>([]);
const submitting = ref(false);
const formError = ref('');
const itemForm = reactive({
  name: '',
  space: 'bedroom' as SpaceKey,
  cate: 'digital' as CategoryKey,
  smallCate: '',
  detailSpace: '',
  count: 1,
  minWarn: 1
});

const isItemOpen = computed(() => store.modal.kind === 'item');
const context = computed(() => store.modal.itemContext);
const nonAllSpaces = computed(() => Object.entries(store.spaces).filter(([key]) => key !== 'all') as [SpaceKey, string][]);
const spaceLabels = computed(() => nonAllSpaces.value.map(([, label]) => label));
const selectableCategories = computed(() => {
  const cates = store.spaceToCates[itemForm.space] ?? store.spaceToCates.all;
  return Object.entries(cates).filter(([key]) => key !== 'all') as [CategoryKey, string][];
});
const categoryLabels = computed(() => selectableCategories.value.map(([, label]) => label));
const lockedSpaceLabel = computed(() => {
  const space = context.value?.space;
  return space ? store.spaces[space] ?? space : '';
});
const lockedCateLabel = computed(() => {
  const space = context.value?.space;
  const cate = context.value?.cate;
  if (!space || !cate) return '';
  return store.spaceToCates[space]?.[cate] ?? store.spaceToCates.all[cate] ?? cate;
});
const selectedSpaceLabel = computed(() => store.spaces[itemForm.space] ?? itemForm.space);
const selectedCateLabel = computed(() => store.spaceToCates[itemForm.space]?.[itemForm.cate] ?? store.spaceToCates.all[itemForm.cate] ?? itemForm.cate);
const selectedCapacity = computed(() => {
  return store.categoriesBySpace[itemForm.space]?.find(
    (category) => category.code === itemForm.cate
  ) ?? null;
});
const quotaText = computed(() => {
  if (store.bootstrap?.unlimited || store.bootstrap?.itemLimitPerSpaceCategory == null) {
    return '当前账户不限物品数量';
  }
  return `每个空间与分类组合最多 ${store.bootstrap.itemLimitPerSpaceCategory} 个物品`;
});
const capacityText = computed(() => {
  const capacity = selectedCapacity.value;
  if (!capacity || capacity.remainingCount == null) return '当前组合不限量';
  return `当前组合已建 ${capacity.itemCount} 个，还可创建 ${capacity.remainingCount} 个`;
});

const firstCategoryFor = (space: SpaceKey): CategoryKey => {
  const cates = store.spaceToCates[space] ?? store.spaceToCates.all;
  return (Object.keys(cates).find((key) => key !== 'all') ?? 'general') as CategoryKey;
};

watch(
  () => store.modal,
  () => {
    formError.value = '';
    submitting.value = false;
    if (store.modal.kind === 'category') {
      const preferredSpace =
        store.currentFilter.space !== 'all'
          ? store.currentFilter.space
          : nonAllSpaces.value[0]?.[0];
      selectedCategorySpaces.value = preferredSpace ? [preferredSpace] : [];
    }
    if (!isItemOpen.value) return;
    const firstSpace = nonAllSpaces.value[0]?.[0] ?? 'bedroom';
    itemForm.space = context.value?.space ?? (store.currentFilter.space !== 'all' ? store.currentFilter.space : firstSpace);
    itemForm.cate = context.value?.cate ?? (store.currentFilter.cate !== 'all' ? store.currentFilter.cate : firstCategoryFor(itemForm.space));
    itemForm.name = context.value?.prefilledName ?? '';
    itemForm.smallCate = '';
    itemForm.detailSpace = '';
    itemForm.count = 1;
    itemForm.minWarn = 1;
  },
  { deep: true }
);

watch(
  () => itemForm.space,
  (space) => {
    if (!selectableCategories.value.some(([key]) => key === itemForm.cate)) {
      itemForm.cate = firstCategoryFor(space);
    }
  }
);

const close = () => {
  if (submitting.value) return;
  store.dispatch({ type: 'CLOSE_MODAL' });
};

const getErrorMessage = (error: unknown) => {
  return (
    (error as { data?: { message?: string } })?.data?.message ??
    (error as { message?: string })?.message ??
    '操作失败，请稍后重试'
  );
};

const submitSpace = async () => {
  const name = spaceForm.name.trim();
  if (!name || submitting.value) return;
  submitting.value = true;
  formError.value = '';
  try {
    await store.createSpace(name);
    spaceForm.name = '';
  } catch (error) {
    formError.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
};

const toggleCategorySpace = (space: SpaceKey) => {
  selectedCategorySpaces.value = selectedCategorySpaces.value.includes(space)
    ? selectedCategorySpaces.value.filter((entry) => entry !== space)
    : [...selectedCategorySpaces.value, space];
};

const submitCategory = async () => {
  const name = categoryForm.name.trim();
  if (!name || !selectedCategorySpaces.value.length || submitting.value) return;
  submitting.value = true;
  formError.value = '';
  try {
    await store.createCategory(name, selectedCategorySpaces.value);
    categoryForm.name = '';
  } catch (error) {
    formError.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
};

const onSpacePick = (event: { detail: { value: string | number } }) => {
  const index = Number(event.detail.value);
  const picked = nonAllSpaces.value[index];
  if (!picked) return;
  itemForm.space = picked[0];
};

const onCategoryPick = (event: { detail: { value: string | number } }) => {
  const index = Number(event.detail.value);
  const picked = selectableCategories.value[index];
  if (!picked) return;
  itemForm.cate = picked[0];
};

const submitItem = async () => {
  const name = itemForm.name.trim();
  if (!name || submitting.value || selectedCapacity.value?.capacityReached) return;

  const payload: ItemInput = {
    name,
    bigCate: itemForm.cate,
    smallCate: itemForm.smallCate.trim() || '常规',
    space: itemForm.space,
    detailSpace: itemForm.detailSpace.trim() || '默认',
    count: Number(itemForm.count) || 0,
    minWarn: Number(itemForm.minWarn) || 0
  };

  submitting.value = true;
  formError.value = '';
  try {
    await store.createItem(payload);
  } catch (error) {
    formError.value = getErrorMessage(error);
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <view v-if="store.modal.kind" class="absolute inset-0 z-50 flex items-end bg-black/40 backdrop-blur-sm">
    <view v-if="store.modal.kind === 'space'" class="w-full space-y-4 rounded-t-2xl bg-white p-6 shadow-2xl">
      <view class="flex items-center justify-between border-b border-slate-100 pb-3">
        <text class="text-sm font-bold text-slate-900">拧入新空间轴</text>
        <button class="m-0 bg-transparent text-xs leading-none text-slate-400" @click="close">取消</button>
      </view>
      <input
        v-model="spaceForm.name"
        class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-2.5 text-sm"
        placeholder="如：厨房、储藏室"
        type="text"
      />
      <text v-if="formError" class="block text-xs text-red-500">{{ formError }}</text>
      <button
        class="m-0 w-full rounded-lg bg-slate-900 py-2.5 text-xs font-semibold leading-none text-white disabled:opacity-40"
        :disabled="!spaceForm.name.trim() || submitting"
        @click="submitSpace"
      >
        {{ submitting ? '创建中...' : '确认创建' }}
      </button>
    </view>

    <view v-else-if="store.modal.kind === 'category'" class="w-full space-y-4 rounded-t-2xl bg-white p-6 shadow-2xl">
      <view class="flex items-center justify-between border-b border-slate-100 pb-3">
        <text class="text-sm font-bold text-slate-900">切割并追加专属分类</text>
        <button class="m-0 bg-transparent text-xs leading-none text-slate-400" @click="close">取消</button>
      </view>
      <input
        v-model="categoryForm.name"
        class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-2.5 text-sm"
        placeholder="如：常备药"
        type="text"
      />
      <view class="space-y-2">
        <text class="block text-[20rpx] font-bold uppercase text-slate-400">选择绑定空间（可多选）</text>
        <view class="flex flex-wrap gap-2">
          <button
            v-for="[key, label] in nonAllSpaces"
            :key="key"
            class="m-0 rounded-lg border px-3 py-2 text-xs leading-none"
            :class="
              selectedCategorySpaces.includes(key)
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-200 bg-slate-50 text-slate-600'
            "
            @click="toggleCategorySpace(key)"
          >
            {{ label }}
          </button>
        </view>
      </view>
      <view class="rounded-lg bg-slate-50 px-3 py-2">
        <text class="text-[20rpx] text-slate-500">{{ quotaText }}</text>
      </view>
      <text v-if="formError" class="block text-xs text-red-500">{{ formError }}</text>
      <button
        class="m-0 w-full rounded-lg bg-slate-900 py-2.5 text-xs font-semibold leading-none text-white disabled:opacity-40"
        :disabled="!categoryForm.name.trim() || !selectedCategorySpaces.length || submitting"
        @click="submitCategory"
      >
        {{ submitting ? '创建中...' : `创建并绑定 ${selectedCategorySpaces.length} 个空间` }}
      </button>
    </view>

    <scroll-view
      v-else
      scroll-y
      enable-flex
      :show-scrollbar="false"
      class="item-modal-scroll w-full rounded-t-2xl bg-white shadow-2xl"
    >
      <view class="space-y-4 p-6">
        <view class="flex items-center justify-between border-b border-slate-100 pb-3">
          <text class="text-sm font-bold text-slate-900">
            {{ context ? `原地追加：[${lockedSpaceLabel}] 专属物品` : '闪电添加存量物品' }}
          </text>
          <button class="m-0 bg-transparent text-xs leading-none text-slate-400" @click="close">取消</button>
        </view>

        <view v-if="context" class="flex items-center justify-between rounded-xl border border-slate-100 bg-slate-50 p-3 text-xs text-slate-500">
          <text>定位空间：<text class="font-bold text-slate-800">{{ lockedSpaceLabel }}</text></text>
          <text>绑定分类：<text class="font-bold text-slate-800">{{ lockedCateLabel }}</text></text>
        </view>

        <view
          class="rounded-xl border px-3 py-2"
          :class="selectedCapacity?.capacityReached ? 'border-red-100 bg-red-50' : 'border-slate-100 bg-slate-50'"
        >
          <text
            class="block text-[20rpx]"
            :class="selectedCapacity?.capacityReached ? 'text-red-500' : 'text-slate-500'"
          >
            {{ capacityText }}
          </text>
        </view>

        <view class="space-y-1">
          <text class="text-[20rpx] font-bold uppercase tracking-wider text-slate-400">具体物品名称 *</text>
          <input
            v-model="itemForm.name"
            class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-2.5 text-sm"
            placeholder="如：黑胡椒粒"
            type="text"
          />
        </view>

        <view v-if="!context" class="grid grid-cols-2 gap-3 text-xs">
          <view>
            <text class="mb-1 block text-slate-400">所属空间</text>
            <picker mode="selector" :range="spaceLabels" @change="onSpacePick">
              <view class="rounded-lg border border-slate-200 bg-slate-50 p-2">{{ selectedSpaceLabel }}</view>
            </picker>
          </view>
          <view>
            <text class="mb-1 block text-slate-400">专属分类</text>
            <picker mode="selector" :range="categoryLabels" @change="onCategoryPick">
              <view class="rounded-lg border border-slate-200 bg-slate-50 p-2">{{ selectedCateLabel }}</view>
            </picker>
          </view>
        </view>

        <view class="grid grid-cols-2 gap-3 text-xs">
          <input v-model="itemForm.smallCate" class="h-10 rounded-lg border border-slate-200 bg-slate-50 px-2" placeholder="细分类(可选)" type="text" />
          <input v-model="itemForm.detailSpace" class="h-10 rounded-lg border border-slate-200 bg-slate-50 px-2" placeholder="精准位置(可选)" type="text" />
        </view>

        <view class="grid grid-cols-2 gap-3 text-xs">
          <view>
            <text class="mb-1 block text-[20rpx] font-bold uppercase text-slate-400">初始存量</text>
            <input v-model.number="itemForm.count" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-2" min="0" type="number" />
          </view>
          <view>
            <text class="mb-1 block text-[20rpx] font-bold uppercase text-slate-400">预警线</text>
            <input v-model.number="itemForm.minWarn" class="h-10 w-full rounded-lg border border-slate-200 bg-slate-50 px-2" min="0" type="number" />
          </view>
        </view>

        <text v-if="formError" class="block text-xs text-red-500">{{ formError }}</text>
        <button
          class="item-submit-button w-full rounded-lg bg-slate-900 py-3 text-xs font-semibold leading-none text-white shadow-md disabled:opacity-40"
          :disabled="!itemForm.name.trim() || submitting || selectedCapacity?.capacityReached"
          @click="submitItem"
        >
          {{ submitting ? '入库中...' : selectedCapacity?.capacityReached ? '当前组合已达上限' : '确认入库' }}
        </button>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
.item-modal-scroll {
  height: 85%;
  min-height: 0;
  overflow: hidden;
}

.item-submit-button {
  margin: 24rpx 0 0;
}
</style>
