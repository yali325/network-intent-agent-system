<template>
  <div
    ref="trackRef"
    class="slide-capsule"
    :class="{ approved: isApproved, disabled }"
    role="slider"
    :aria-valuenow="Math.round(percent)"
    aria-valuemin="0"
    aria-valuemax="100"
    tabindex="0"
    @pointerdown="beginDrag"
  >
    <div class="slide-fill" :style="{ width: `${percent}%` }"></div>
    <span class="slide-label">{{ isApproved ? '已授权，允许 Apply' : '滑动授权修复动作' }}</span>
    <span class="slide-threshold">85%</span>
    <span class="slide-thumb" :style="{ transform: `translateX(${thumbOffset}px)` }">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M7.5 10V8.2C7.5 5.8 9.4 4 12 4s4.5 1.8 4.5 4.2V10" />
        <rect x="6" y="10" width="12" height="9" rx="2.2" />
        <path d="M12 13.2v2.6" />
      </svg>
    </span>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';

const props = defineProps<{ approved: boolean; disabled?: boolean }>();
const emit = defineEmits<{ approved: [] }>();

const trackRef = ref<HTMLElement | null>(null);
const percent = ref(props.approved ? 100 : 0);
const dragging = ref(false);
const localApproved = ref(false);
const isApproved = computed(() => props.approved || localApproved.value);
const thumbOffset = computed(() => {
  const trackWidth = 320;
  const thumbSize = 36;
  return (trackWidth - thumbSize - 8) * (percent.value / 100);
});

watch(
  () => props.approved,
  (approved) => {
    if (approved) {
      localApproved.value = true;
      percent.value = 100;
    }
  }
);

function beginDrag(event: PointerEvent): void {
  if (props.disabled || isApproved.value) return;
  dragging.value = true;
  updateFromPointer(event);
  window.addEventListener('pointermove', updateFromPointer);
  window.addEventListener('pointerup', endDrag, { once: true });
}

function updateFromPointer(event: PointerEvent): void {
  if (!dragging.value || !trackRef.value) return;
  const rect = trackRef.value.getBoundingClientRect();
  const usableWidth = Math.max(1, rect.width - 44);
  const next = ((event.clientX - rect.left - 22) / usableWidth) * 100;
  percent.value = Math.min(100, Math.max(0, next));
}

function endDrag(): void {
  if (!dragging.value) return;
  dragging.value = false;
  window.removeEventListener('pointermove', updateFromPointer);
  if (percent.value >= 85) {
    percent.value = 100;
    localApproved.value = true;
    emit('approved');
  } else {
    percent.value = 0;
  }
}

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', updateFromPointer);
  window.removeEventListener('pointerup', endDrag);
});
</script>

<style scoped>
.slide-capsule {
  position: relative;
  width: min(320px, 100%);
  height: 44px;
  overflow: hidden;
  border: 1px solid rgba(0, 217, 192, 0.34);
  border-radius: 999px;
  color: var(--mactav-text-main);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.78), rgba(226, 246, 255, 0.62)),
    radial-gradient(circle at 100% 50%, rgba(0, 217, 192, 0.16), transparent 42%);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.62), 0 12px 26px rgba(31, 91, 180, 0.1);
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.slide-capsule:active {
  cursor: grabbing;
}

.slide-capsule.disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.slide-capsule.approved {
  border-color: rgba(16, 185, 129, 0.42);
  cursor: default;
}

.slide-fill {
  position: absolute;
  inset: 0 auto 0 0;
  width: 0;
  border-radius: inherit;
  background:
    linear-gradient(90deg, rgba(0, 98, 255, 0.18), rgba(0, 217, 192, 0.42)),
    linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: width 120ms ease;
}

.slide-label {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  padding-left: 28px;
  color: #0f172a;
  font-size: 13px;
  font-weight: 950;
  letter-spacing: 0;
  pointer-events: none;
}

.slide-threshold {
  position: absolute;
  top: 50%;
  right: 18px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 10px;
  transform: translateY(-50%);
  pointer-events: none;
}

.slide-thumb {
  position: absolute;
  top: 3px;
  left: 4px;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(0, 98, 255, 0.22);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 8px 18px rgba(31, 91, 180, 0.18);
  transition: transform 120ms ease, border-color 160ms ease, box-shadow 160ms ease;
  pointer-events: none;
}

.slide-capsule.approved .slide-thumb {
  border-color: rgba(0, 217, 192, 0.58);
  box-shadow: 0 0 0 5px rgba(0, 217, 192, 0.12), 0 8px 18px rgba(31, 91, 180, 0.18);
}

svg {
  width: 18px;
  height: 18px;
}

path,
rect {
  fill: none;
  stroke: #0f172a;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
