<script setup lang="ts">
import { computed } from 'vue';
import type { IntentNode, IntentRelation, NetworkIntent } from '@/types';
import JsonViewer from './JsonViewer.vue';

const props = defineProps<{
  intent?: NetworkIntent;
}>();

const nodes = computed(() => props.intent?.semanticIntentGraph?.nodes ?? []);
const relations = computed(() => props.intent?.semanticIntentGraph?.relations ?? []);

const nodeColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 160 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 140 },
  { title: '描述', dataIndex: 'description', key: 'description' }
];

const relationColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 170 },
  { title: '类型', dataIndex: 'type', key: 'type', width: 150 },
  { title: '源对象', dataIndex: 'source', key: 'source' },
  { title: '目标对象', dataIndex: 'target', key: 'target' },
  { title: '动作', dataIndex: 'action', key: 'action', width: 110 },
  { title: '服务', dataIndex: 'service', key: 'service', width: 120 },
  { title: '描述', dataIndex: 'description', key: 'description' }
];

const rowKey = (record: IntentNode | IntentRelation) => record.id;
</script>

<template>
  <a-empty v-if="!intent" description="意图解析尚未生成" />
  <section v-else class="panel-stack">
    <section class="panel">
      <div class="section-title">业务对象</div>
      <a-table :columns="nodeColumns" :data-source="nodes" :row-key="rowKey" :pagination="false" size="small" />
    </section>

    <section class="panel">
      <div class="section-title">业务关系</div>
      <a-table :columns="relationColumns" :data-source="relations" :row-key="rowKey" :pagination="false" size="small" />
    </section>

    <a-collapse ghost>
      <a-collapse-panel key="json" header="完整 JSON">
        <JsonViewer :value="intent" />
      </a-collapse-panel>
    </a-collapse>
  </section>
</template>
