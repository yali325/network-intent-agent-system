<template>
  <aside class="evidence-panel">
    <div class="eyebrow">Artifact / Evidence Inspector</div>
    <h3>{{ evidence.relatedNode }}</h3>
    <p>{{ evidence.relatedConfigSummary }}</p>
    <div class="version-stack">
      <article v-for="item in evidence.versionDiff" :key="item.version" :class="item.tone">
        <span>{{ item.version }}</span>
        <strong>{{ item.summary }}</strong>
      </article>
    </div>
    <div class="conflict-hint">{{ evidence.conflictHint }}</div>
    <slot name="action"></slot>
  </aside>
</template>

<script setup lang="ts">
import type { EvidenceInspectorDemo } from '@/api/futureContracts';

defineProps<{ evidence: EvidenceInspectorDemo }>();
</script>

<style scoped>
.evidence-panel {
  padding: 18px;
  border: 1px solid rgba(0, 98, 255, 0.18);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.62);
}

h3 {
  margin: 8px 0 10px;
  color: var(--mactav-text-main);
}

p,
.conflict-hint {
  color: var(--mactav-text-muted);
  line-height: 1.65;
}

.version-stack {
  display: grid;
  gap: 10px;
  margin: 14px 0;
}

.version-stack article {
  padding: 12px;
  border-radius: 16px;
  background: rgba(0, 98, 255, 0.06);
}

.version-stack .old {
  background: rgba(245, 158, 11, 0.1);
}

.version-stack span {
  color: var(--mactav-cyber-blue);
  font-weight: 950;
}

.version-stack strong {
  display: block;
  margin-top: 5px;
  color: var(--mactav-text-soft);
  font-size: 12px;
  line-height: 1.5;
}

.conflict-hint {
  padding: 12px;
  border: 1px solid rgba(239, 68, 68, 0.18);
  border-radius: 16px;
  background: rgba(254, 242, 242, 0.66);
}
</style>
