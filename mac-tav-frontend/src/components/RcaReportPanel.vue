<template>
  <section class="rca-panel">
    <div class="eyebrow">HealingAgent 诊断报告</div>
    <h3>{{ rca.category }}</h3>
    <div class="rca-meta">
      <span>Severity: {{ rca.severity }}</span>
      <span>Scope: {{ rca.scope }}</span>
    </div>
    <p>{{ rca.recommendation }}</p>
    <div class="evidence-list">
      <i v-for="ref in rca.evidenceRefs" :key="ref" class="mono">{{ ref }}</i>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { RcaReportDemo } from '@/api/futureContracts';

defineProps<{ rca: RcaReportDemo }>();
</script>

<style scoped>
.rca-panel {
  padding: 18px;
  border: 1px solid rgba(0, 98, 255, 0.18);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.62);
}

h3 {
  margin: 8px 0 12px;
  color: var(--mactav-text-main);
}

.rca-meta,
.evidence-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rca-meta span,
.evidence-list i {
  padding: 6px 9px;
  border-radius: 999px;
  color: var(--mactav-text-soft);
  background: rgba(0, 98, 255, 0.07);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

p {
  margin: 16px 0;
  color: var(--mactav-text-muted);
  line-height: 1.75;
}
</style>
