<template>
  <button class="glow-button" :disabled="disabled || loading" type="button" @click="$emit('click', $event)">
    <span class="glow-track" />
    <span>{{ loading ? loadingText : label }}</span>
  </button>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string;
    loadingText?: string;
    loading?: boolean;
    disabled?: boolean;
  }>(),
  {
    loadingText: '正在启动闭环验证',
    loading: false,
    disabled: false
  }
);

defineEmits<{ click: [event: MouseEvent] }>();
</script>

<style scoped>
.glow-button {
  position: relative;
  display: inline-grid;
  min-height: 54px;
  place-items: center;
  overflow: hidden;
  padding: 0 28px;
  border: 0;
  border-radius: 999px;
  color: white;
  background: linear-gradient(135deg, #0058f1, #00bfae);
  box-shadow: 0 18px 36px rgba(0, 98, 255, 0.28);
  cursor: pointer;
  font-weight: 900;
}

.glow-button:disabled {
  cursor: not-allowed;
  filter: grayscale(0.35);
  opacity: 0.62;
}

.glow-track {
  position: absolute;
  inset: -2px;
  background: linear-gradient(100deg, transparent 0%, rgba(255, 255, 255, 0.58) 18%, transparent 36%);
  transform: translateX(-80%);
  animation: sweep 2.4s ease-in-out infinite;
}

.glow-button span:last-child {
  position: relative;
}

@keyframes sweep {
  0%,
  35% {
    transform: translateX(-90%);
  }

  100% {
    transform: translateX(92%);
  }
}
</style>
