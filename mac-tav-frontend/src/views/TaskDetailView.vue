<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { ArrowLeftOutlined, ReloadOutlined } from '@ant-design/icons-vue';
import AgentLogPanel from '@/components/AgentLogPanel.vue';
import ConfigPanel from '@/components/ConfigPanel.vue';
import ExecutionPanel from '@/components/ExecutionPanel.vue';
import IntentPanel from '@/components/IntentPanel.vue';
import TaskOverview from '@/components/TaskOverview.vue';
import TopologyPanel from '@/components/TopologyPanel.vue';
import ValidationPanel from '@/components/ValidationPanel.vue';
import { useTaskStore } from '@/stores/taskStore';

const route = useRoute();
const router = useRouter();
const taskStore = useTaskStore();
const activeKey = ref('overview');
const errorMessage = ref('');

const taskId = computed(() => String(route.params.taskId || ''));
const workspace = computed(() => taskStore.currentWorkspace);

const loadWorkspace = async () => {
  if (!taskId.value) {
    errorMessage.value = '缺少 taskId';
    return;
  }

  errorMessage.value = '';
  try {
    await taskStore.fetch(taskId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '查询任务失败';
    message.error(errorMessage.value);
  }
};

onMounted(loadWorkspace);
watch(taskId, loadWorkspace);
</script>

<template>
  <main class="detail-page">
    <section class="detail-toolbar">
      <div>
        <p class="eyebrow">Workspace</p>
        <h1>{{ workspace?.task?.taskId || taskId }}</h1>
      </div>
      <a-space>
        <a-button @click="router.push('/')">
          <template #icon><ArrowLeftOutlined /></template>
          返回
        </a-button>
        <a-button :loading="taskStore.loading" @click="loadWorkspace">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </section>

    <a-alert v-if="errorMessage" class="page-alert" type="error" :message="errorMessage" show-icon />
    <a-skeleton v-else-if="taskStore.loading && !workspace" active />

    <section v-else-if="workspace" class="detail-tabs">
      <a-tabs v-model:active-key="activeKey" size="large">
        <a-tab-pane key="overview" tab="任务概览">
          <TaskOverview :workspace="workspace" />
        </a-tab-pane>
        <a-tab-pane key="intent" tab="意图解析">
          <IntentPanel :intent="workspace.intent" />
        </a-tab-pane>
        <a-tab-pane key="topology" tab="网络规划 / 拓扑图">
          <TopologyPanel :plan="workspace.plan" />
        </a-tab-pane>
        <a-tab-pane key="config" tab="配置生成">
          <ConfigPanel :config-set="workspace.configSet" />
        </a-tab-pane>
        <a-tab-pane key="execution" tab="执行结果">
          <ExecutionPanel :report="workspace.executionReport" />
        </a-tab-pane>
        <a-tab-pane key="validation" tab="验证报告">
          <ValidationPanel :report="workspace.validationReport" />
        </a-tab-pane>
        <a-tab-pane key="logs" tab="Agent 执行日志">
          <AgentLogPanel :logs="workspace.agentLogs" />
        </a-tab-pane>
      </a-tabs>
    </section>
  </main>
</template>
