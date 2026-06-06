<script setup lang="ts">
import { useInventoryStore } from '@/stores/inventoryStore';
import type { AppView } from '@/types/inventory';

const store = useInventoryStore();

const tabs: Array<{ key: AppView; label: string }> = [
  { key: 'home', label: '存魔方' },
  { key: 'lens', label: '透视镜' },
  { key: 'axis', label: '流转轴' },
  { key: 'profile', label: '我的' }
];
</script>

<template>
  <view class="safe-bottom grid shrink-0 select-none grid-cols-4 items-center border-t border-slate-100 bg-white px-3 pt-3 text-[20rpx] text-slate-400">
    <button
      v-for="tab in tabs"
      :key="tab.key"
      class="m-0 flex flex-col items-center justify-center gap-1 bg-transparent p-0 leading-none transition-all"
      :class="store.activeView === tab.key ? 'font-bold text-slate-900' : ''"
      @click="store.dispatch({ type: 'SET_VIEW', payload: tab.key })"
    >
      <view class="nav-icon" :class="[`nav-icon-${tab.key}`, store.activeView === tab.key ? 'is-active' : '']">
        <view v-if="tab.key === 'home'" class="home-grid">
          <view />
          <view />
          <view />
          <view />
        </view>
        <view v-else-if="tab.key === 'lens'" class="lens-glyph" />
        <view v-else-if="tab.key === 'axis'" class="axis-glyph">
          <view />
          <view />
          <view />
          <view />
        </view>
        <view v-else class="profile-glyph">
          <view />
          <view />
        </view>
      </view>
      <text>{{ tab.label }}</text>
    </button>
  </view>
</template>

<style scoped>
.nav-icon {
  position: relative;
  width: 34rpx;
  height: 34rpx;
  color: currentColor;
}

.home-grid {
  display: grid;
  width: 28rpx;
  height: 28rpx;
  grid-template-columns: repeat(2, 1fr);
  gap: 3rpx;
  margin: 3rpx auto 0;
  transform: rotate(2deg);
}

.home-grid view {
  border: 1px solid currentColor;
  background: currentColor;
  opacity: 0.9;
}

.home-grid view:nth-child(2),
.home-grid view:nth-child(3) {
  background: transparent;
  opacity: 0.55;
}

.lens-glyph {
  position: absolute;
  left: 7rpx;
  top: 6rpx;
  width: 20rpx;
  height: 22rpx;
  border-left: 4rpx solid currentColor;
  border-right: 4rpx solid currentColor;
  transform: skewX(-14deg);
}

.lens-glyph::after {
  position: absolute;
  left: 4rpx;
  top: -2rpx;
  width: 4rpx;
  height: 26rpx;
  background: currentColor;
  content: "";
}

.axis-glyph {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 3rpx;
  height: 30rpx;
  padding-top: 3rpx;
}

.axis-glyph view {
  width: 4rpx;
  border-radius: 999rpx;
  background: currentColor;
}

.axis-glyph view:nth-child(1) {
  height: 24rpx;
  opacity: 0.45;
}

.axis-glyph view:nth-child(2) {
  height: 28rpx;
}

.axis-glyph view:nth-child(3) {
  height: 22rpx;
  opacity: 0.65;
}

.axis-glyph view:nth-child(4) {
  height: 26rpx;
  opacity: 0.8;
}

.profile-glyph {
  position: relative;
  width: 30rpx;
  height: 30rpx;
  margin: 1rpx auto 0;
}

.profile-glyph view:first-child {
  position: absolute;
  left: 10rpx;
  top: 3rpx;
  width: 10rpx;
  height: 10rpx;
  border: 3rpx solid currentColor;
  border-radius: 999rpx;
}

.profile-glyph view:last-child {
  position: absolute;
  left: 7rpx;
  bottom: 3rpx;
  width: 16rpx;
  height: 10rpx;
  border: 3rpx solid currentColor;
  border-top-left-radius: 999rpx;
  border-top-right-radius: 999rpx;
  border-bottom: 0;
}
</style>
