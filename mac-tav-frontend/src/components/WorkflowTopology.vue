<template>
  <div class="workflow-canvas">
    <svg class="flow-svg" viewBox="0 0 1000 420" role="img" aria-label="MAC-TAV six stage workflow">
      <defs>
        <linearGradient id="flowGradient" x1="0" x2="1">
          <stop offset="0%" stop-color="#0062ff" />
          <stop offset="100%" stop-color="#00d9c0" />
        </linearGradient>
        <filter id="flowGlow">
          <feGaussianBlur stdDeviation="5" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>
      <path class="flow-base" d="M140 120 H355 H585 H810 V270 H585 H355 H140" />
      <path class="flow-active" d="M140 120 H355 H585 H810 V270 H585 H355 H140" />
      <path class="flow-return" d="M810 270 C760 344 650 360 560 322" />
      <text x="804" y="207" class="flow-label">Execution Evidence</text>
      <text x="196" y="338" class="flow-label">Healing Loop</text>
    </svg>

    <StageNode
      v-for="(stage, index) in nodes"
      :key="stage.key"
      :style="{ left: `${stage.x}%`, top: `${stage.y}%` }"
      :index="index + 1"
      :title="stage.title"
      :caption="stage.caption"
      :status="stageStatus(stage.key)"
      :selected="stage.key === selectedStage"
      @select="$emit('selectStage', stage.key)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { WorkflowStage } from '@/api/types';
import { stageLabels, stageOrder } from '@/fixtures/demoTask';
import StageNode from '@/components/StageNode.vue';

const props = defineProps<{
  currentStage: WorkflowStage;
  selectedStage: WorkflowStage;
}>();

defineEmits<{ selectStage: [stage: WorkflowStage] }>();

const nodes = computed(() =>
  stageOrder.map((key, index) => ({
    key,
    ...stageLabels[key],
    x: [14, 35.5, 58.5, 81, 58.5, 14][index],
    y: [30, 30, 30, 30, 67, 67][index]
  }))
);

function stageStatus(stage: WorkflowStage): 'done' | 'running' | 'pending' {
  const currentIndex = stageOrder.indexOf(props.currentStage);
  const index = stageOrder.indexOf(stage);
  if (index < currentIndex) return 'done';
  if (index === currentIndex) return 'running';
  return 'pending';
}
</script>

<style scoped>
.workflow-canvas {
  position: relative;
  min-height: 420px;
  overflow: hidden;
  border: 1px solid rgba(110, 155, 215, 0.28);
  border-radius: 28px;
  background:
    radial-gradient(circle at 14% 24%, rgba(0, 217, 192, 0.13), transparent 24%),
    radial-gradient(circle at 80% 30%, rgba(0, 98, 255, 0.12), transparent 28%),
    rgba(255, 255, 255, 0.45);
}

.flow-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.flow-base,
.flow-active,
.flow-return {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.flow-base {
  stroke: rgba(100, 116, 139, 0.2);
  stroke-width: 16;
}

.flow-active {
  filter: url(#flowGlow);
  stroke: url(#flowGradient);
  stroke-dasharray: 18 14;
  stroke-width: 7;
  animation: flow-dash 1.7s linear infinite;
}

.flow-return {
  stroke: rgba(245, 158, 11, 0.6);
  stroke-dasharray: 9 11;
  stroke-width: 5;
  animation: flow-dash 1.9s linear infinite reverse;
}

.flow-label {
  fill: rgba(15, 23, 42, 0.45);
  font-size: 18px;
  font-weight: 800;
}
</style>
