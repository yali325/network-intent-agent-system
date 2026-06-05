<template>
  <button class="repair-card" :class="{ selected: selected, approved: action.status === 'APPROVED', applied: action.status === 'APPLIED' }" type="button" @click="$emit('select')">
    <div class="repair-head">
      <span class="mono">{{ action.actionId }}</span>
      <b>{{ action.status }}</b>
    </div>
    <h4>{{ action.actionType }}</h4>
    <div class="repair-grid">
      <span>Stage</span><strong>{{ action.targetStage }}</strong>
      <span>Risk</span><strong>{{ action.riskLevel }}</strong>
      <span>Approval</span><strong>{{ action.requiresApproval ? 'Required' : 'Optional' }}</strong>
    </div>
    <p>{{ action.reason }}</p>
    <small>{{ action.guidance }}</small>
    <pre v-if="action.candidateSnippet?.length" class="snippet">{{ action.candidateSnippet.join('\n') }}</pre>
  </button>
</template>

<script setup lang="ts">
import type { RepairActionDemo } from '@/api/futureContracts';

defineProps<{ action: RepairActionDemo; selected: boolean }>();
defineEmits<{ select: [] }>();
</script>

<style scoped>
.repair-card {
  width: 100%;
  padding: 14px;
  border: 1px solid rgba(110, 155, 215, 0.24);
  border-radius: 18px;
  color: var(--mactav-text-main);
  text-align: left;
  background: rgba(255, 255, 255, 0.58);
  cursor: pointer;
}

.repair-card.selected {
  border-color: rgba(0, 98, 255, 0.46);
  box-shadow: 0 16px 34px rgba(31, 91, 180, 0.14);
}

.repair-card.approved {
  border-color: rgba(16, 185, 129, 0.38);
}

.repair-card.applied {
  border-color: rgba(0, 217, 192, 0.45);
  background: rgba(236, 253, 245, 0.72);
}

.repair-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.repair-head span {
  color: var(--mactav-cyber-blue);
  font-size: 11px;
}

.repair-head b {
  color: var(--mactav-text-muted);
  font-size: 11px;
}

h4 {
  margin: 10px 0;
}

.repair-grid {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 6px 10px;
}

.repair-grid span,
small {
  color: var(--mactav-text-muted);
  font-size: 12px;
}

.repair-grid strong {
  color: var(--mactav-text-soft);
  font-size: 12px;
}

p {
  margin: 12px 0 8px;
  color: var(--mactav-text-muted);
  line-height: 1.55;
}

.snippet {
  margin: 10px 0 0;
  overflow: auto;
  padding: 10px;
  border-radius: 12px;
  color: #0f172a;
  background: rgba(0, 98, 255, 0.06);
  font-size: 11px;
  line-height: 1.55;
}
</style>
