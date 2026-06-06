<script setup lang="ts">
import { useInventoryStore } from '@/stores/inventoryStore';

const store = useInventoryStore();

const loadMore = () => {
  if (store.hasMoreLogs && !store.loadingMoreLogs) {
    void store.loadTransactions(false);
  }
};
</script>

<template>
  <view class="axis-shell bg-[#fcfcfc]">
    <view class="axis-header">
      <text class="axis-heading">流转时空轴线</text>
      <text class="axis-badge">秒级热更新</text>
    </view>

    <scroll-view
      scroll-y
      enable-flex
      :show-scrollbar="false"
      :lower-threshold="120"
      class="axis-scroll"
      @scrolltolower="loadMore"
    >
      <view class="axis-list">
        <view v-if="store.logError && !store.logs.length" class="axis-error">
          <text>{{ store.logError }}</text>
          <button class="axis-retry" @click="store.loadTransactions(true)">重新加载</button>
        </view>
        <view class="axis-rail" />
        <view v-for="log in store.logs" :key="log.id" class="axis-row">
          <view class="axis-time">
            <text class="axis-time-main">{{ log.time }}</text>
            <text class="axis-time-sub">{{ log.date }}</text>
          </view>
          <view class="axis-dot" />
          <view class="axis-card">
            <view class="axis-card-head">
              <text class="axis-item-name">{{ log.itemName }}</text>
              <text class="axis-icon">{{ log.icon }}</text>
            </view>
            <text class="axis-desc">{{ log.desc }}</text>
            <view class="axis-path">
              <text class="axis-path-label">轨迹：</text>
              <text class="axis-path-value">{{ log.path }}</text>
            </view>
          </view>
        </view>
        <view v-if="store.loadingMoreLogs || store.hasMoreLogs" class="axis-load-more">
          <text>{{ store.loadingMoreLogs ? '正在加载...' : '继续上滑加载更多' }}</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
.axis-shell {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
}

.axis-header {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
  background: #fff;
  padding: 34rpx 48rpx;
}

.axis-heading {
  color: #94a3b8;
  font-size: 26rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.axis-badge {
  border-radius: 16rpx;
  background: #f1f5f9;
  color: #64748b;
  font-size: 22rpx;
  font-weight: 600;
  padding: 12rpx 18rpx;
}

.axis-scroll {
  position: relative;
  width: 100%;
  min-height: 0;
  flex: 1 1 0%;
  overflow: hidden;
}

.axis-list {
  position: relative;
  padding: 68rpx 48rpx 96rpx;
}

.axis-rail {
  position: absolute;
  top: 82rpx;
  bottom: 78rpx;
  left: 130rpx;
  width: 3rpx;
  border-radius: 999rpx;
  background: linear-gradient(180deg, #eef2f7, #f8fafc);
}

.axis-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  gap: 26rpx;
  margin-bottom: 42rpx;
}

.axis-time {
  width: 78rpx;
  flex-shrink: 0;
  padding-top: 4rpx;
}

.axis-time-main {
  display: block;
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 800;
  line-height: 1.1;
}

.axis-time-sub {
  display: block;
  margin-top: 10rpx;
  color: #94a3b8;
  font-size: 20rpx;
  font-weight: 600;
}

.axis-dot {
  width: 22rpx;
  height: 22rpx;
  flex-shrink: 0;
  margin-top: 10rpx;
  border: 6rpx solid #fff;
  border-radius: 999rpx;
  background: #0f172a;
  box-shadow: 0 0 0 1px #e2e8f0;
}

.axis-card {
  min-width: 0;
  flex: 1 1 0%;
  border: 1px solid #eef2f7;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 18rpx 42rpx rgba(15, 23, 42, 0.055);
  padding: 30rpx 32rpx;
}

.axis-card-head {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.axis-item-name {
  min-width: 0;
  flex: 1 1 0%;
  overflow: hidden;
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 800;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.axis-icon {
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 1;
}

.axis-desc {
  display: block;
  margin-top: 10rpx;
  color: #64748b;
  font-size: 24rpx;
  font-weight: 500;
  line-height: 1.35;
}

.axis-path {
  display: flex;
  flex-wrap: wrap;
  gap: 6rpx;
  margin-top: 18rpx;
  border-radius: 14rpx;
  background: #f8fafc;
  color: #94a3b8;
  font-size: 22rpx;
  line-height: 1.35;
  padding: 14rpx 18rpx;
}

.axis-path-label {
  font-weight: 700;
}

.axis-path-value {
  color: #64748b;
  font-weight: 600;
}

.axis-load-more {
  display: flex;
  min-height: 64rpx;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 22rpx;
}

.axis-error {
  position: relative;
  z-index: 2;
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

.axis-retry {
  margin: 0;
  border-radius: 16rpx;
  background: #0f172a;
  color: #fff;
  font-size: 22rpx;
  line-height: 1;
  padding: 20rpx 30rpx;
}
</style>
