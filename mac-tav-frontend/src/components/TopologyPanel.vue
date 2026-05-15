<script setup lang="ts">
import { computed } from 'vue';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { GraphChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import VChart from 'vue-echarts';
import type { NetworkPlan, TopologyNode } from '@/types';
import JsonViewer from './JsonViewer.vue';

use([CanvasRenderer, GraphChart, GridComponent, LegendComponent, TooltipComponent]);

const props = defineProps<{
  plan?: NetworkPlan;
}>();

const categoryName = (node: TopologyNode) => {
  if (node.nodeType === 'HOST') return '终端';
  if (node.role === 'internet' || node.id === 'internet') return '外部网络';
  if (node.deviceType === 'ROUTER') return '路由器';
  if (node.deviceType === 'SWITCH') return '交换机';
  return '设备';
};

const symbolSize = (node: TopologyNode) => {
  if (node.id === 'internet') return 58;
  if (node.deviceType === 'ROUTER') return 64;
  if (node.deviceType === 'SWITCH') return 56;
  return 48;
};

const topologyOption = computed(() => {
  const topology = props.plan?.topology;
  const nodes = topology?.nodes ?? [];
  const links = topology?.links ?? [];
  const categories = Array.from(new Set(nodes.map(categoryName))).map((name) => ({ name }));

  return {
    color: ['#1677ff', '#13a8a8', '#52c41a', '#faad14', '#722ed1'],
    tooltip: {
      trigger: 'item'
    },
    legend: {
      top: 8,
      left: 12
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        top: 46,
        bottom: 24,
        left: 24,
        right: 24,
        categories,
        force: {
          repulsion: 680,
          edgeLength: [92, 150],
          gravity: 0.06
        },
        label: {
          show: true,
          position: 'bottom',
          formatter: '{b}'
        },
        edgeLabel: {
          show: true,
          color: '#667085',
          fontSize: 11,
          formatter: '{c}'
        },
        lineStyle: {
          color: '#98a2b3',
          width: 2,
          curveness: 0.05
        },
        data: nodes.map((node) => ({
          id: node.id,
          name: node.name || node.id,
          category: categoryName(node),
          symbolSize: symbolSize(node),
          value: node.role || node.deviceType || node.hostType || node.nodeType,
          tooltip: {
            formatter: [
              `<b>${node.name || node.id}</b>`,
              `ID: ${node.id}`,
              node.deviceType ? `类型: ${node.deviceType}` : '',
              node.hostType ? `终端: ${node.hostType}` : '',
              node.zoneId ? `区域: ${node.zoneId}` : ''
            ]
              .filter(Boolean)
              .join('<br />')
          }
        })),
        links: links.map((link) => ({
          id: link.id,
          source: link.sourceNode,
          target: link.targetNode,
          value: link.linkType || '',
          tooltip: {
            formatter: [
              `<b>${link.id}</b>`,
              `${link.sourceNode}${link.sourceInterface ? ` / ${link.sourceInterface}` : ''}`,
              `${link.targetNode}${link.targetInterface ? ` / ${link.targetInterface}` : ''}`,
              link.linkType ? `链路: ${link.linkType}` : ''
            ]
              .filter(Boolean)
              .join('<br />')
          }
        }))
      }
    ]
  };
});

const zoneColumns = [
  { title: '区域 ID', dataIndex: 'zoneId', key: 'zoneId' },
  { title: '名称', dataIndex: 'zoneName', key: 'zoneName' },
  { title: '类型', dataIndex: 'zoneType', key: 'zoneType' },
  { title: '描述', dataIndex: 'description', key: 'description' }
];
</script>

<template>
  <a-empty v-if="!plan" description="网络规划尚未生成" />
  <section v-else class="panel-stack">
    <section class="panel">
      <div class="section-title">拓扑图</div>
      <div class="topology-canvas">
        <VChart :option="topologyOption" autoresize />
      </div>
    </section>

    <section class="panel">
      <div class="section-title">规划摘要</div>
      <a-typography-paragraph>{{ plan.planSummary || '-' }}</a-typography-paragraph>
      <a-table
        v-if="plan.zones?.length"
        :columns="zoneColumns"
        :data-source="plan.zones"
        :pagination="false"
        row-key="zoneId"
        size="small"
      />
    </section>

    <a-collapse ghost>
      <a-collapse-panel key="json" header="完整 JSON">
        <JsonViewer :value="plan" />
      </a-collapse-panel>
    </a-collapse>
  </section>
</template>
