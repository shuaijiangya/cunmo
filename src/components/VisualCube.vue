<script setup lang="ts">
import { computed } from 'vue';

import type { CategoryKey, SpaceKey } from '@/types/inventory';

const props = defineProps<{
  space: SpaceKey;
  cate: CategoryKey;
  order: number;
}>();

const faces = ['front', 'back', 'left', 'right', 'top', 'bottom'] as const;
const colorMaps: Record<string, [string, string]> = {
  all: ['#cbd5e1', '#94a3b8'],
  bedroom: ['#22d3ee', '#0891b2'],
  living: ['#f87171', '#dc2626'],
  office: ['#818cf8', '#3730a3']
};

const transform = computed(() => {
  const rotateX = props.space === 'all' ? -25 : -115;
  const rotateY = props.cate === 'all' ? 45 : 135;
  return `rotateX(${rotateX}deg) rotateY(${rotateY}deg)`;
});

const cells = computed(() => Array.from({ length: props.order * props.order }, (_, index) => index));
const activeColors = computed(() => colorMaps[props.space] ?? ['#94a3b8', '#475569']);
const faceGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${props.order}, 1fr)`,
  gridTemplateRows: `repeat(${props.order}, 1fr)`
}));

const cellColor = (face: (typeof faces)[number], index: number) => {
  if (face !== 'front' && face !== 'top') return '#cbd5e1';
  return index % 2 === 0 ? activeColors.value[0] : activeColors.value[1];
};
</script>

<template>
  <view class="cube-scene">
    <view class="cube-entity" :style="{ transform }">
      <view v-for="face in faces" :key="face" class="cube-face" :class="`face-${face}`" :style="faceGridStyle">
        <view v-for="cell in cells" :key="`${face}-${cell}`" class="cube-cell" :style="{ backgroundColor: cellColor(face, cell) }" />
      </view>
    </view>
  </view>
</template>

<style scoped>
.cube-scene {
  width: 150rpx;
  height: 150rpx;
  perspective: 800rpx;
}

.cube-entity {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.8s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.cube-face {
  position: absolute;
  display: grid;
  width: 150rpx;
  height: 150rpx;
  gap: 3rpx;
  padding: 4rpx;
  border: 2px solid #1e293b;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.5s ease;
}

.cube-cell {
  border-radius: 3rpx;
  transition: background-color 0.4s;
}

.face-front {
  transform: rotateY(0deg) translateZ(75rpx);
}

.face-back {
  transform: rotateY(180deg) translateZ(75rpx);
}

.face-left {
  transform: rotateY(-90deg) translateZ(75rpx);
}

.face-right {
  transform: rotateY(90deg) translateZ(75rpx);
}

.face-top {
  transform: rotateX(90deg) translateZ(75rpx);
}

.face-bottom {
  transform: rotateX(-90deg) translateZ(75rpx);
}
</style>
