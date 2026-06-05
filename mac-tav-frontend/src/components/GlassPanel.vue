<template>
  <section class="glass" :class="{ compact }">
    <slot />
  </section>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ compact?: boolean }>(), {
  compact: false
});
</script>

<style scoped>
.glass {
  position: relative;
  overflow: hidden;
  padding: 24px;
  border: 1px solid var(--mactav-panel-border);
  border-radius: 28px;
  background: var(--mactav-panel-bg);
  box-shadow: var(--mactav-panel-shadow);
  backdrop-filter: blur(18px);
}

.glass::before {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(120deg, rgba(255, 255, 255, 0.72), transparent 38%),
    radial-gradient(circle at 86% 8%, rgba(0, 217, 192, 0.16), transparent 28%);
  content: "";
}

.glass > :deep(*) {
  position: relative;
}

.compact {
  padding: 18px;
  border-radius: 22px;
}
</style>
