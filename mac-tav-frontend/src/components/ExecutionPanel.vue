<script setup lang="ts">
import { computed } from 'vue';
import type { ExecutionReport, TestCaseResult } from '@/types';
import JsonViewer from './JsonViewer.vue';

const props = defineProps<{
  report?: ExecutionReport;
}>();

const tests = computed(() => {
  const connectivity = props.report?.testResult?.connectivityTests ?? [];
  const policy = props.report?.testResult?.policyTests ?? [];
  return [
    ...connectivity.map((item) => ({ ...item, category: '连通性' })),
    ...policy.map((item) => ({ ...item, category: '策略' }))
  ];
});

const columns = [
  { title: '类型', dataIndex: 'category', key: 'category', width: 110 },
  { title: 'Test ID', dataIndex: 'testId', key: 'testId', width: 210 },
  { title: '源', dataIndex: 'source', key: 'source' },
  { title: '目标', dataIndex: 'target', key: 'target' },
  { title: '期望', dataIndex: 'expected', key: 'expected', width: 130 },
  { title: '实际', dataIndex: 'actual', key: 'actual', width: 130 },
  { title: '结果', dataIndex: 'success', key: 'success', width: 110 }
];

const rowKey = (record: TestCaseResult & { category: string }) => record.testId;
</script>

<template>
  <a-empty v-if="!report" description="执行结果尚未生成" />
  <section v-else class="panel-stack">
    <section class="panel">
      <div class="section-title">执行状态</div>
      <a-descriptions bordered size="small" :column="{ xs: 1, md: 2 }">
        <a-descriptions-item label="Execution Mode">
          <a-tag color="processing">{{ report.executionMode || '-' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="Stage Status">{{ report.stageStatus || '-' }}</a-descriptions-item>
        <a-descriptions-item label="Environment">{{ report.runtimeState?.environmentStatus || '-' }}</a-descriptions-item>
        <a-descriptions-item label="Controller">
          <a-tag :color="report.runtimeState?.controllerConnected ? 'success' : 'default'">
            {{ report.runtimeState?.controllerConnected ? 'CONNECTED' : 'N/A' }}
          </a-tag>
        </a-descriptions-item>
      </a-descriptions>
    </section>

    <section class="panel">
      <div class="section-title">测试结果</div>
      <a-table :columns="columns" :data-source="tests" :row-key="rowKey" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'success'">
            <a-tag :color="record.success ? 'success' : 'error'">
              {{ record.success ? 'PASS' : 'FAIL' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </section>

    <a-collapse ghost>
      <a-collapse-panel key="json" header="完整 JSON">
        <JsonViewer :value="report" />
      </a-collapse-panel>
    </a-collapse>
  </section>
</template>
