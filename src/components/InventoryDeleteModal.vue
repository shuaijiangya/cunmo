<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import { inventoryApi } from '@/services/inventoryApi';
import { useInventoryStore } from '@/stores/inventoryStore';
import type {
  DeletionStrategy,
  InventoryDeletionPreview
} from '@/types/inventory';

const store = useInventoryStore();
const preview = ref<InventoryDeletionPreview | null>(null);
const loading = ref(false);
const submitting = ref(false);
const errorMessage = ref('');
const strategy = ref<DeletionStrategy>('MOVE');
const categoryAction = ref<'unbind' | 'global'>('unbind');
const targetSpaceId = ref<number | null>(null);
const targetCategoryId = ref<number | null>(null);
const destructiveConfirmed = ref(false);

const state = computed(() => store.deletionModal);
const spaces = computed(() =>
  Object.entries(store.spaceIds)
    .map(([code, id]) => ({
      id,
      code,
      name: store.spaces[code] ?? code
    }))
    .filter((entry) =>
      state.value?.scope === 'space'
        ? entry.id !== state.value.targetId
        : true
    )
);
const targetSpace = computed(() =>
  spaces.value.find((entry) => entry.id === targetSpaceId.value)
);
const categories = computed(() => {
  const modal = state.value;
  if (!modal) return [];
  if (modal.scope === 'item') {
    const code = targetSpace.value?.code;
    if (!code) return [];
    return (store.categoriesBySpace[code] ?? [])
      .filter(
        (category) =>
          targetSpaceId.value !== modal.spaceId ||
          category.id !== modal.categoryId
      );
  }
  if (modal.scope === 'category' && categoryAction.value === 'unbind') {
    const spaceCode = Object.entries(store.spaceIds).find(
      ([, id]) => id === modal.spaceId
    )?.[0];
    return (spaceCode ? store.categoriesBySpace[spaceCode] : [])
      ?.filter((category) => category.id !== modal.targetId) ?? [];
  }
  const unique = new Map<number, { id: number; name: string }>();
  for (const list of Object.values(store.categoriesBySpace)) {
    for (const category of list) {
      if (category.id !== modal.targetId) {
        unique.set(category.id, {
          id: category.id,
          name: category.name
        });
      }
    }
  }
  return [...unique.values()];
});
const spaceLabels = computed(() =>
  spaces.value.map((entry) => entry.name)
);
const categoryLabels = computed(() =>
  categories.value.map((entry) => entry.name)
);
const selectedSpaceLabel = computed(
  () => targetSpace.value?.name ?? '请选择目标空间'
);
const selectedCategoryLabel = computed(
  () =>
    categories.value.find(
      (entry) => entry.id === targetCategoryId.value
    )?.name ?? '请选择目标分类'
);
const scopeLabel = computed(() => {
  if (state.value?.scope === 'space') return '空间';
  if (state.value?.scope === 'category') return '分类';
  return '物品';
});

const resetTargets = () => {
  targetSpaceId.value = spaces.value[0]?.id ?? null;
  targetCategoryId.value = categories.value[0]?.id ?? null;
};

const loadPreview = async () => {
  const modal = state.value;
  if (!modal) return;
  loading.value = true;
  errorMessage.value = '';
  preview.value = null;
  try {
    if (modal.scope === 'item') {
      preview.value = await inventoryApi.getItemDeletionPreview(
        modal.targetId
      );
    } else if (modal.scope === 'space') {
      preview.value = await inventoryApi.getSpaceDeletionPreview(
        modal.targetId
      );
    } else {
      preview.value = await inventoryApi.getCategoryDeletionPreview(
        modal.targetId,
        categoryAction.value === 'unbind'
          ? modal.spaceId
          : undefined
      );
    }
  } catch (error) {
    errorMessage.value = store.requestErrorMessage(
      error,
      '影响范围加载失败'
    );
  } finally {
    loading.value = false;
  }
};

watch(
  () => store.deletionModal,
  (modal) => {
    if (!modal) return;
    strategy.value = 'MOVE';
    categoryAction.value = 'unbind';
    destructiveConfirmed.value = false;
    resetTargets();
    void loadPreview();
  },
  { deep: true }
);

watch([targetSpaceId, categoryAction], () => {
  targetCategoryId.value = categories.value[0]?.id ?? null;
  destructiveConfirmed.value = false;
});

watch(categoryAction, () => {
  if (state.value?.scope === 'category') {
    void loadPreview();
  }
});

watch(strategy, () => {
  destructiveConfirmed.value = false;
});

const pickSpace = (event: { detail: { value: string | number } }) => {
  targetSpaceId.value =
    spaces.value[Number(event.detail.value)]?.id ?? null;
};

const pickCategory = (
  event: { detail: { value: string | number } }
) => {
  targetCategoryId.value =
    categories.value[Number(event.detail.value)]?.id ?? null;
};

const submit = async () => {
  const modal = state.value;
  if (!modal || submitting.value) return;
  if (strategy.value === 'CLEAR_DELETE' && !destructiveConfirmed.value) {
    destructiveConfirmed.value = true;
    return;
  }
  if (strategy.value === 'MOVE') {
    if (modal.scope === 'space' && !targetSpaceId.value) {
      errorMessage.value = '没有可用的目标空间';
      return;
    }
    if (modal.scope !== 'space' && !targetCategoryId.value) {
      errorMessage.value = '没有可用的目标分类';
      return;
    }
  }

  submitting.value = true;
  errorMessage.value = '';
  try {
    if (modal.scope === 'item') {
      if (strategy.value === 'MOVE') {
        await store.moveInventoryItem(
          modal.targetId,
          targetSpaceId.value as number,
          targetCategoryId.value as number
        );
      } else {
        await store.deleteInventoryItem(modal.targetId);
      }
    } else if (modal.scope === 'space') {
      await store.deleteInventorySpace(
        modal.targetId,
        strategy.value,
        targetSpaceId.value ?? undefined
      );
    } else if (categoryAction.value === 'unbind') {
      await store.unbindInventoryCategory(
        modal.targetId,
        modal.spaceId as number,
        strategy.value,
        targetCategoryId.value ?? undefined
      );
    } else {
      await store.deleteInventoryCategory(
        modal.targetId,
        strategy.value,
        targetCategoryId.value ?? undefined
      );
    }
    store.closeDeletionModal();
    uni.showToast({ title: '操作完成', icon: 'success' });
  } catch (error) {
    errorMessage.value = store.requestErrorMessage(
      error,
      '删除操作失败'
    );
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <view
    v-if="state"
    class="absolute inset-0 z-[60] flex items-end bg-black/45"
    @click.self="store.closeDeletionModal()"
  >
    <view class="delete-modal-sheet w-full rounded-t-2xl bg-white px-6 pt-5 shadow-2xl">
      <view class="flex items-start justify-between border-b border-slate-100 pb-4">
        <view class="min-w-0">
          <text class="block text-sm font-bold text-slate-900">
            管理{{ scopeLabel }}：{{ state.targetName }}
          </text>
          <text class="mt-1 block text-[20rpx] text-slate-400">
            删除前请确认受影响库存和迁移目标
          </text>
        </view>
        <button
          class="m-0 bg-transparent p-1 text-lg leading-none text-slate-400"
          @click="store.closeDeletionModal()"
        >
          ×
        </button>
      </view>

      <view class="space-y-4 py-5">
        <view
          v-if="loading"
          class="rounded-xl bg-slate-50 px-4 py-5 text-center text-xs text-slate-400"
        >
          正在核对影响范围...
        </view>
        <view
          v-else-if="preview"
          class="grid grid-cols-3 gap-2 rounded-xl border border-slate-100 bg-slate-50 p-3 text-center"
        >
          <view>
            <text class="block text-base font-bold text-slate-900">{{ preview.affectedItemCount }}</text>
            <text class="text-[18rpx] text-slate-400">物品种类</text>
          </view>
          <view>
            <text class="block text-base font-bold text-slate-900">{{ preview.affectedQuantity }}</text>
            <text class="text-[18rpx] text-slate-400">库存数量</text>
          </view>
          <view>
            <text class="block text-base font-bold text-slate-900">{{ preview.bindingCount }}</text>
            <text class="text-[18rpx] text-slate-400">空间绑定</text>
          </view>
        </view>

        <view v-if="state.scope === 'category'" class="grid grid-cols-2 rounded-lg bg-slate-100 p-1">
          <button
            class="m-0 rounded-md py-2 text-xs leading-none"
            :class="categoryAction === 'unbind' ? 'bg-white font-bold text-slate-900 shadow-sm' : 'bg-transparent text-slate-400'"
            @click="categoryAction = 'unbind'"
          >
            仅解绑当前空间
          </button>
          <button
            class="m-0 rounded-md py-2 text-xs leading-none"
            :class="categoryAction === 'global' ? 'bg-white font-bold text-slate-900 shadow-sm' : 'bg-transparent text-slate-400'"
            @click="categoryAction = 'global'"
          >
            全局删除分类
          </button>
        </view>

        <view class="grid grid-cols-2 gap-2">
          <button
            class="m-0 rounded-lg border py-3 text-xs leading-none"
            :class="strategy === 'MOVE' ? 'border-slate-900 bg-slate-900 font-bold text-white' : 'border-slate-200 bg-white text-slate-500'"
            @click="strategy = 'MOVE'"
          >
            迁移后删除
          </button>
          <button
            class="m-0 rounded-lg border py-3 text-xs leading-none"
            :class="strategy === 'CLEAR_DELETE' ? 'border-red-500 bg-red-50 font-bold text-red-500' : 'border-slate-200 bg-white text-slate-500'"
            @click="strategy = 'CLEAR_DELETE'"
          >
            清空并删除
          </button>
        </view>

        <view v-if="strategy === 'MOVE'" class="space-y-3">
          <picker
            v-if="state.scope === 'space' || state.scope === 'item'"
            mode="selector"
            :range="spaceLabels"
            @change="pickSpace"
          >
            <view class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-xs text-slate-700">
              目标空间：{{ selectedSpaceLabel }}
            </view>
          </picker>
          <picker
            v-if="state.scope !== 'space'"
            mode="selector"
            :range="categoryLabels"
            @change="pickCategory"
          >
            <view class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-3 text-xs text-slate-700">
              目标分类：{{ selectedCategoryLabel }}
            </view>
          </picker>
          <text class="block text-[20rpx] leading-relaxed text-slate-400">
            迁移后精准位置会统一标记为“待整理”，便于后续重新归位。
          </text>
        </view>

        <view
          v-else
          class="rounded-xl border border-red-100 bg-red-50 px-4 py-3"
        >
          <text class="block text-xs font-bold text-red-600">
            此操作会将受影响库存归零并永久隐藏对应结构。
          </text>
          <text class="mt-1 block text-[20rpx] text-red-400">
            流转记录会保留，本版本不提供回收站恢复。
          </text>
        </view>

        <text v-if="errorMessage" class="block text-xs text-red-500">
          {{ errorMessage }}
        </text>

        <button
          class="m-0 w-full rounded-xl py-3 text-xs font-bold leading-none text-white disabled:opacity-40"
          :class="
            strategy === 'CLEAR_DELETE'
              ? 'bg-red-500'
              : 'bg-slate-900'
          "
          :disabled="loading || submitting"
          @click="submit"
        >
          {{
            submitting
              ? '处理中...'
              : strategy === 'CLEAR_DELETE' && !destructiveConfirmed
                ? '继续清空删除'
                : strategy === 'CLEAR_DELETE'
                  ? '再次确认并删除'
                  : '确认迁移并删除'
          }}
        </button>
      </view>
    </view>
  </view>
</template>

<style scoped>
.delete-modal-sheet {
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
}
</style>
