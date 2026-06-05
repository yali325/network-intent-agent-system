<template>
  <span class="pill" :class="tone">
    <i />
    <slot />
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ tone?: 'running' | 'ok' | 'bad' | 'warn' | 'pending' | 'signal' }>(), {
  tone: 'pending'
});
</script>

<style scoped>
.pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 30px;
  padding: 6px 10px;
  border: 1px solid rgba(110, 155, 215, 0.3);
  border-radius: 999px;
  color: var(--mactav-text-soft);
  background: rgba(255, 255, 255, 0.66);
  font-size: 12px;
  font-weight: 850;
}

.pill i {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #94a3b8;
}

.running i,
.signal i {
  background: var(--mactav-cyber-blue);
  animation: pulse-ring 1.5s ease-out infinite;
}

.ok i {
  background: var(--mactav-success);
}

.bad i {
  background: var(--mactav-danger);
}

.warn i {
  background: var(--mactav-warning);
}
</style>
