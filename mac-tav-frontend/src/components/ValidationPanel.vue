<script setup lang="ts">
import type { ValidationItem, ValidationReport } from '@/types';
import JsonViewer from './JsonViewer.vue';

defineProps<{
  report?: ValidationReport;
}>();

const statusColor = (status?: string) => {
  if (status === 'PASSED') return 'success';
  if (status === 'FAILED') return 'error';
  return 'default';
};

const columns = [
  { title: '验证项', dataIndex: 'name', key: 'name', width: 210 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: '期望', dataIndex: 'expected', key: 'expected' },
  { title: '实际', dataIndex: 'actual', key: 'actual' },
  { title: '结果', dataIndex: 'passed', key: 'passed', width: 110 },
  { title: '关联测试', dataIndex: 'relatedTestId', key: 'relatedTestId', width: 190 },
  { title: '说明', dataIndex: 'message', key: 'message' }
];

const rowKey = (record: ValidationItem) => record.itemId;
</script>

<template>
  <a-empty v-if="!report" description="验证报告尚未生成" />
  <section v-else class="panel-stack">
    <section class="panel">
      <div class="section-title">验证结论</div>
      <a-descriptions bordered size="small" :column="{ xs: 1, md: 2 }">
        <a-descriptions-item label="Overall Status">
          <a-tag :color="statusColor(report.overallStatus)">
            {{ report.overallStatus || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="Stage Status">{{ report.stageStatus || '-' }}</a-descriptions-item>
        <a-descriptions-item label="Summary" :span="2">{{ report.summary || '-' }}</a-descriptions-item>
      </a-descriptions>
    </section>

    <section class="panel">
      <div class="section-title">验证项</div>
      <a-table :columns="columns" :data-source="report.items || []" :row-key="rowKey" :pagination="false" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'passed'">
            <a-tag :color="record.passed ? 'success' : 'error'">
              {{ record.passed ? '通过' : '失败' }}
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
