<template>
  <article class="assertion-card" :class="assertion.status.toLowerCase()">
    <div class="assertion-head">
      <div>
        <span class="route">{{ assertion.source }} -> {{ assertion.destination }}</span>
        <strong>{{ assertion.expectationLabel }}</strong>
      </div>
      <span class="stamp">{{ assertion.status }}</span>
    </div>
    <div class="assertion-grid">
      <span>实际探测</span><b>{{ assertion.actual }}</b>
      <span>测试 ID</span><b class="mono">{{ assertion.testId }}</b>
      <span>配置块</span><b class="mono">{{ assertion.configBlockId }}</b>
    </div>
    <p>{{ assertion.message }}</p>
    <div class="trace-list">
      <i v-for="trace in assertion.traceRefs" :key="trace" class="mono">{{ trace }}</i>
    </div>
  </article>
</template>

<script setup lang="ts">
import type { ValidationAssertionDemo } from '@/api/futureContracts';

defineProps<{ assertion: ValidationAssertionDemo }>();
</script>

<style scoped>
.assertion-card {
  position: relative;
  overflow: hidden;
  padding: 16px;
  border: 1px solid rgba(110, 155, 215, 0.24);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.64);
  box-shadow: 0 14px 30px rgba(31, 91, 180, 0.08);
}

.assertion-card.failed {
  border-color: rgba(239, 68, 68, 0.34);
  box-shadow: 0 0 0 1px rgba(239, 68, 68, 0.1), 0 18px 40px rgba(239, 68, 68, 0.12);
  animation: failed-pulse 1.8s ease-in-out infinite;
}

.assertion-card.pending {
  border-style: dashed;
}

.assertion-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.route,
.assertion-head strong,
.assertion-grid span,
.assertion-grid b {
  display: block;
}

.route {
  color: var(--mactav-cyber-blue);
  font-size: 13px;
  font-weight: 950;
}

.assertion-head strong {
  margin-top: 5px;
  color: var(--mactav-text-main);
}

.stamp {
  padding: 5px 9px;
  border-radius: 999px;
  color: #047857;
  background: rgba(16, 185, 129, 0.12);
  font-size: 12px;
  font-weight: 950;
}

.failed .stamp {
  color: #dc2626;
  background: rgba(239, 68, 68, 0.12);
  box-shadow: 0 0 18px rgba(239, 68, 68, 0.16);
}

.pending .stamp {
  color: #64748b;
  background: rgba(148, 163, 184, 0.14);
}

.assertion-grid {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 8px 12px;
  margin-top: 14px;
}

.assertion-grid span {
  color: var(--mactav-text-muted);
  font-size: 12px;
}

.assertion-grid b {
  color: var(--mactav-text-soft);
  font-size: 12px;
}

p {
  margin: 14px 0 0;
  color: var(--mactav-text-muted);
  line-height: 1.6;
}

.failed p {
  color: #b91c1c;
}

.trace-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}

.trace-list i {
  padding: 5px 7px;
  border-radius: 999px;
  color: #64748b;
  background: rgba(0, 98, 255, 0.06);
  font-size: 11px;
  font-style: normal;
}

@keyframes failed-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 1px rgba(239, 68, 68, 0.1), 0 18px 40px rgba(239, 68, 68, 0.1);
  }

  50% {
    box-shadow: 0 0 0 1px rgba(239, 68, 68, 0.24), 0 20px 46px rgba(239, 68, 68, 0.18);
  }
}
</style>
