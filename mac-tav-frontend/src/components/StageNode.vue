<template>
  <button class="stage-node" :class="[status, { selected }]" type="button" @click="$emit('select')">
    <span class="node-index">{{ index }}</span>
    <strong>{{ title }}</strong>
    <small>{{ caption }}</small>
  </button>
</template>

<script setup lang="ts">
defineProps<{
  index: number;
  title: string;
  caption: string;
  status: 'done' | 'running' | 'pending' | 'failed' | 'not_ready';
  selected: boolean;
}>();

defineEmits<{ select: [] }>();
</script>

<style scoped>
.stage-node {
  position: absolute;
  display: grid;
  width: 138px;
  min-height: 104px;
  align-content: center;
  justify-items: center;
  gap: 6px;
  padding: 14px;
  border: 1px solid rgba(110, 155, 215, 0.36);
  border-radius: 22px;
  color: var(--mactav-text-main);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 12px 30px rgba(31, 91, 180, 0.12);
  cursor: pointer;
  transform: translate(-50%, -50%);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.stage-node:hover,
.selected {
  border-color: rgba(0, 98, 255, 0.52);
  box-shadow: 0 18px 38px rgba(0, 98, 255, 0.18);
  transform: translate(-50%, -54%);
}

.node-index {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 999px;
  color: white;
  background: #94a3b8;
  font-size: 12px;
  font-weight: 950;
}

strong {
  font-size: 16px;
}

small {
  color: var(--mactav-text-muted);
  font-size: 11px;
}

.done .node-index {
  background: var(--mactav-success);
}

.running {
  background:
    radial-gradient(circle at 80% 10%, rgba(0, 217, 192, 0.23), transparent 34%),
    rgba(255, 255, 255, 0.82);
}

.running .node-index {
  background: linear-gradient(135deg, var(--mactav-cyber-blue), var(--mactav-neon-teal));
  animation: pulse-ring 1.45s ease-out infinite;
}

.failed {
  border-color: rgba(239, 68, 68, 0.34);
  background:
    radial-gradient(circle at 80% 10%, rgba(239, 68, 68, 0.16), transparent 34%),
    rgba(255, 255, 255, 0.82);
}

.failed .node-index {
  background: var(--mactav-danger);
}

.not_ready {
  opacity: 0.76;
}

.not_ready .node-index {
  background: #cbd5e1;
}

@media (max-width: 820px) {
  .stage-node {
    width: 118px;
    min-height: 94px;
  }
}
</style>
