<script setup lang="ts">
import type { NetworkWorkspace } from '@/types';

defineProps<{
  workspace: NetworkWorkspace;
}>();

const statusColor = (status?: string) => {
  if (status === 'PASSED') return 'success';
  if (status === 'FAILED' || status === 'ERROR') return 'error';
  if (status === 'EXECUTED' || status === 'GENERATED') return 'processing';
  return 'default';
};
</script>

<template>
  <section class="panel">
    <a-descriptions bordered size="small" :column="{ xs: 1, sm: 1, md: 2, lg: 2 }">
      <a-descriptions-item label="Task ID">
        <a-typography-text copyable>{{ workspace.task?.taskId || '-' }}</a-typography-text>
      </a-descriptions-item>
      <a-descriptions-item label="任务状态">
        <a-tag :color="statusColor(workspace.task?.taskStatus)">
          {{ workspace.task?.taskStatus || '-' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="当前阶段">
        {{ workspace.task?.currentStage || '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="创建时间">
        {{ workspace.task?.createdAt || '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="版本">
        Intent {{ workspace.currentIntentVersion ?? 0 }} / Plan {{ workspace.currentPlanVersion ?? 0 }} /
        Config {{ workspace.currentConfigVersion ?? 0 }} / Execution {{ workspace.currentExecutionVersion ?? 0 }} /
        Validation {{ workspace.currentValidationVersion ?? 0 }}
      </a-descriptions-item>
      <a-descriptions-item label="更新时间">
        {{ workspace.task?.updatedAt || '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="原始输入" :span="2">
        <a-typography-paragraph class="raw-text">
          {{ workspace.task?.rawText || '-' }}
        </a-typography-paragraph>
      </a-descriptions-item>
    </a-descriptions>
  </section>
</template>
