<template>
  <button class="scene-card" type="button" @click="$emit('select', prompt)">
    <span>{{ subtitle }}</span>
    <strong>{{ title }}</strong>
    <small>点击填充意图</small>
  </button>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  subtitle: string;
  prompt: string;
}>();

defineEmits<{
  select: [prompt: string];
}>();
</script>

<style scoped>
.scene-card {
  min-height: 136px;
  padding: 18px;
  border: 1px solid rgba(0, 98, 255, 0.18);
  border-radius: 20px;
  color: var(--mactav-text-main);
  text-align: left;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.82), rgba(234, 245, 255, 0.68)),
    radial-gradient(circle at 88% 18%, rgba(0, 217, 192, 0.15), transparent 32%);
  box-shadow: 0 12px 28px rgba(31, 91, 180, 0.09);
  cursor: pointer;
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.scene-card:hover {
  border-color: rgba(0, 98, 255, 0.42);
  box-shadow: 0 18px 38px rgba(31, 91, 180, 0.15);
  transform: translateY(-4px);
}

.scene-card span,
.scene-card strong,
.scene-card small {
  display: block;
}

.scene-card span {
  color: var(--mactav-cyber-blue);
  font-size: 12px;
  font-weight: 900;
}

.scene-card strong {
  margin-top: 16px;
  font-size: 18px;
}

.scene-card small {
  margin-top: 20px;
  color: var(--mactav-text-muted);
}
</style>
