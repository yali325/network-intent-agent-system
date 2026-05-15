<script setup lang="ts">
import { computed } from 'vue';
import type { AgentStepLog } from '@/types';

const props = defineProps<{
  logs?: AgentStepLog[];
}>();

const sortedLogs = computed(() =>
  [...(props.logs ?? [])].sort((a, b) => (a.startedAt || '').localeCompare(b.startedAt || ''))
);

const colorFor = (status?: string) => {
  if (status === 'COMPLETED' || status === 'SUCCESS') return 'green';
  if (status === 'FAILED' || status === 'ERROR') return 'red';
  if (status === 'RUNNING') return 'blue';
  return 'gray';
};
</script>

<template>
  <a-empty v-if="!sortedLogs.length" description="暂无 Agent 执行日志" />
  <section v-else class="panel">
    <a-timeline>
      <a-timeline-item v-for="log in sortedLogs" :key="log.stepId || `${log.stage}-${log.startedAt}`" :color="colorFor(log.stageStatus)">
        <div class="log-line">
          <div class="log-title">
            <span>{{ log.agentName || 'Agent' }}</span>
            <a-tag>{{ log.stage || '-' }}</a-tag>
            <a-tag :color="colorFor(log.stageStatus)">{{ log.stageStatus || '-' }}</a-tag>
          </div>
          <div class="log-message">{{ log.message || '-' }}</div>
          <div class="log-time">
            {{ log.startedAt || '-' }}
            <span v-if="log.finishedAt"> -> {{ log.finishedAt }}</span>
          </div>
        </div>
      </a-timeline-item>
    </a-timeline>
  </section>
</template>
