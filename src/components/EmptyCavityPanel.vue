<script setup lang="ts">
import { useInventoryStore } from '@/stores/inventoryStore';
import type { EmptyCavityContext } from '@/types/inventory';

defineProps<{
  context: EmptyCavityContext;
}>();

const store = useInventoryStore();
</script>

<template>
  <view class="empty-cavity-card">
    <view class="empty-cavity-icon">
      <text>{{ context.isSearchEmpty ? '🔍' : '📦' }}</text>
    </view>
    <view class="empty-cavity-copy">
      <text class="empty-cavity-title">
        {{ context.isSearchEmpty ? '未搜到任何相符存量' : '当前魔方空腔内空无一物' }}
      </text>
      <text class="empty-cavity-meta">
        对焦：{{ context.spaceLabel }} ➔ {{ context.categoryLabel }}
      </text>
    </view>
    <button
      class="empty-cavity-button"
      @click="store.dispatch({ type: 'OPEN_MODAL', payload: { kind: 'item', itemContext: context } })"
    >
      <text class="empty-cavity-plus">+</text>
      <text>在此空腔下建货</text>
      <text v-if="context.prefilledName">"{{ context.prefilledName }}"</text>
    </button>
  </view>
</template>

<style scoped>
.empty-cavity-card {
  display: flex;
  min-height: 380rpx;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 30rpx;
  border: 1px dashed #dbe4ef;
  border-radius: 32rpx;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.82), rgba(255, 255, 255, 0.96));
  box-shadow:
    inset 0 2rpx 8rpx rgba(15, 23, 42, 0.025),
    0 24rpx 56rpx rgba(15, 23, 42, 0.035);
  padding: 62rpx 40rpx;
  text-align: center;
}

.empty-cavity-icon {
  display: grid;
  width: 86rpx;
  height: 86rpx;
  place-items: center;
  border: 1px solid #edf2f7;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 14rpx 30rpx rgba(15, 23, 42, 0.08);
  color: #334155;
  font-size: 34rpx;
}

.empty-cavity-copy {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  align-items: center;
}

.empty-cavity-title {
  color: #334155;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.25;
}

.empty-cavity-meta {
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 500;
  line-height: 1.35;
}

.empty-cavity-button {
  display: flex;
  min-width: 260rpx;
  height: 78rpx;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  margin: 0;
  border-radius: 26rpx;
  background: #0f172a;
  box-shadow: 0 16rpx 32rpx rgba(15, 23, 42, 0.18);
  color: #fff;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 1;
  padding: 0 34rpx;
}

.empty-cavity-button:active {
  transform: scale(0.98);
}

.empty-cavity-plus {
  font-size: 34rpx;
  line-height: 1;
}
</style>
