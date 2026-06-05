<template>
  <div v-if="task" class="task-page page-frame">
    <GlassPanel>
      <div class="mission-head">
        <div>
          <div class="eyebrow">Mission Control</div>
          <h1 class="page-title">当前任务: {{ task.task.taskId }}</h1>
          <p class="intent">{{ task.task.rawText }}</p>
        </div>
        <div class="status-stack">
          <StatusPill tone="running">状态: {{ task.task.taskStatus }}</StatusPill>
          <StatusPill tone="signal">latest jobId: {{ task.latestJob.jobId }}</StatusPill>
          <StatusPill tone="pending">耗时: {{ task.elapsedSeconds.toFixed(1) }}s</StatusPill>
          <StatusPill tone="pending">mock</StatusPill>
          <button class="glass-action" type="button" @click="confirmNewTask">新建意图任务</button>
        </div>
      </div>
    </GlassPanel>

    <GlassPanel>
      <WorkflowTopology
        :current-stage="task.task.currentStage"
        :selected-stage="store.selectedStage"
        @select-stage="store.selectedStage = $event"
      />
    </GlassPanel>

    <div class="mission-grid">
      <GlassPanel compact>
        <div class="panel-head">
          <div>
            <div class="eyebrow">Telemetry Preview</div>
            <h2>最近事件</h2>
          </div>
        </div>
        <div class="telemetry-list">
          <article v-for="event in task.telemetry" :key="event.eventId" class="telemetry-item">
            <span class="mono">{{ event.eventType }}</span>
            <strong>{{ event.title }}</strong>
            <p>{{ event.message }}</p>
          </article>
        </div>
      </GlassPanel>

      <GlassPanel compact>
        <div class="panel-head">
          <div>
            <div class="eyebrow">当前阶段产物速览</div>
            <h2>{{ store.currentSummary?.agentName }}</h2>
          </div>
          <StatusPill tone="signal">{{ store.selectedStage }}</StatusPill>
        </div>
        <div class="summary-card">
          <span>产物: {{ store.currentSummary?.artifactName }} v{{ store.currentSummary?.version }}</span>
          <p>{{ store.currentSummary?.summary }}</p>
          <ul>
            <li v-for="item in store.currentSummary?.commandDigest" :key="item" class="mono">{{ item }}</li>
          </ul>
        </div>
      </GlassPanel>
    </div>

    <NetworkTopologyLiveBoard :task="task" />

    <button class="append-tab" type="button" @click="drawerOpen = true">
      追加新指令
    </button>

    <a-drawer
      v-model:open="drawerOpen"
      class="intent-drawer"
      width="420"
      placement="right"
      :body-style="{ padding: 0 }"
      :header-style="{ display: 'none' }"
    >
      <div class="drawer-shell">
        <div class="drawer-head">
          <div>
            <div class="eyebrow">Intent Dialog Drawer</div>
            <h2>追加新指令</h2>
          </div>
          <button class="drawer-close" type="button" @click="drawerOpen = false">关闭</button>
        </div>
        <p class="drawer-copy">
          在当前拓扑、配置和验证结果基础上继续提问。这里先触发 mock 的 `appendIntent`，后续可接真实多轮 Agent 链路。
        </p>
        <a-textarea
          v-model:value="appendText"
          class="append-input"
          :auto-size="{ minRows: 7, maxRows: 10 }"
          placeholder="例如：将办公区的 VLAN 改为 VLAN 40，并重新生成核心交换机配置。"
        />
        <div class="drawer-actions">
          <a-button @click="appendText = ''">清空</a-button>
          <a-button type="primary" :loading="appendLoading" :disabled="!appendText.trim()" @click="submitAppendIntent">
            发送追加指令
          </a-button>
        </div>
        <div v-if="store.appendedIntents.length" class="append-history">
          <strong>最近追加</strong>
          <article v-for="item in store.appendedIntents.slice(0, 3)" :key="item.id">
            <span class="mono">{{ item.createTime }}</span>
            <p>{{ item.text }}</p>
          </article>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { message, Modal } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import GlassPanel from '@/components/GlassPanel.vue';
import NetworkTopologyLiveBoard from '@/components/NetworkTopologyLiveBoard.vue';
import StatusPill from '@/components/StatusPill.vue';
import WorkflowTopology from '@/components/WorkflowTopology.vue';
import { useTaskStore } from '@/stores/taskStore';

const props = defineProps<{ taskId: string }>();
const router = useRouter();
const store = useTaskStore();
const { activeTask: task } = storeToRefs(store);
const drawerOpen = ref(false);
const appendText = ref('');
const appendLoading = ref(false);

onMounted(() => {
  store.loadTask(props.taskId);
});

function confirmNewTask(): void {
  Modal.confirm({
    title: '确认新建意图任务？',
    content: '离开当前任务指挥舱后，当前 mock 拓扑与命令视窗会保留在状态中，但输入页将准备接收新的自然语言意图。',
    okText: '回到输入页',
    cancelText: '继续查看当前任务',
    centered: true,
    async onOk() {
      store.prepareNewIntent();
      await router.push('/console');
    }
  });
}

async function submitAppendIntent(): Promise<void> {
  const text = appendText.value.trim();
  if (!text) return;
  appendLoading.value = true;
  try {
    await store.appendIntent(text);
    message.success('追加指令已进入 mock 多轮队列');
    appendText.value = '';
    drawerOpen.value = false;
  } finally {
    appendLoading.value = false;
  }
}
</script>

<style scoped>
.task-page {
  display: grid;
  gap: 18px;
}

.mission-head,
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.intent {
  max-width: 900px;
  margin: 12px 0 0;
  color: var(--mactav-text-soft);
  line-height: 1.7;
}

.status-stack {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.glass-action {
  min-height: 32px;
  padding: 6px 13px;
  border: 1px solid rgba(0, 98, 255, 0.24);
  border-radius: 999px;
  color: var(--mactav-cyber-blue);
  background: rgba(255, 255, 255, 0.64);
  box-shadow: 0 10px 24px rgba(31, 91, 180, 0.1);
  cursor: pointer;
  font-weight: 900;
  backdrop-filter: blur(14px);
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}

.glass-action:hover {
  border-color: rgba(0, 98, 255, 0.46);
  box-shadow: 0 16px 32px rgba(31, 91, 180, 0.16);
  transform: translateY(-1px);
}

.append-tab {
  position: fixed;
  top: 45%;
  right: 18px;
  z-index: 20;
  padding: 12px 10px;
  border: 1px solid rgba(0, 217, 192, 0.34);
  border-radius: 999px;
  color: var(--mactav-text-main);
  writing-mode: vertical-rl;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 36px rgba(31, 91, 180, 0.14);
  cursor: pointer;
  font-weight: 950;
  backdrop-filter: blur(16px);
}

.append-tab:hover {
  color: var(--mactav-cyber-blue);
}

:deep(.intent-drawer .ant-drawer-content) {
  background:
    radial-gradient(circle at 80% 10%, rgba(0, 217, 192, 0.15), transparent 30%),
    linear-gradient(155deg, rgba(255, 255, 255, 0.86), rgba(236, 248, 255, 0.82));
  border-left: 1px solid rgba(110, 155, 215, 0.28);
  box-shadow: -24px 0 60px rgba(31, 91, 180, 0.14);
  backdrop-filter: blur(20px);
}

.drawer-shell {
  display: grid;
  gap: 16px;
  min-height: 100%;
  padding: 24px;
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.drawer-head h2 {
  margin: 6px 0 0;
}

.drawer-close {
  border: 1px solid rgba(110, 155, 215, 0.26);
  border-radius: 999px;
  color: var(--mactav-text-muted);
  background: rgba(255, 255, 255, 0.58);
  cursor: pointer;
  font-weight: 800;
}

.drawer-copy {
  margin: 0;
  color: var(--mactav-text-muted);
  line-height: 1.7;
}

.append-input {
  border-radius: 18px;
  color: var(--mactav-text-main);
  background: rgba(255, 255, 255, 0.72);
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.append-history {
  display: grid;
  gap: 10px;
  margin-top: 4px;
}

.append-history strong {
  color: var(--mactav-text-main);
}

.append-history article {
  padding: 12px;
  border: 1px solid rgba(0, 98, 255, 0.14);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.56);
}

.append-history span {
  color: var(--mactav-cyber-blue);
  font-size: 11px;
}

.append-history p {
  margin: 6px 0 0;
  color: var(--mactav-text-soft);
  line-height: 1.55;
}

.mission-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 18px;
}

h2 {
  margin: 6px 0 0;
  color: var(--mactav-text-main);
}

.telemetry-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.telemetry-item {
  padding: 14px;
  border: 1px solid rgba(0, 98, 255, 0.15);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.58);
}

.telemetry-item span,
.telemetry-item strong {
  display: block;
}

.telemetry-item span {
  color: var(--mactav-cyber-blue);
  font-size: 12px;
}

.telemetry-item strong {
  margin: 6px 0;
}

.telemetry-item p,
.summary-card p {
  margin: 0;
  color: var(--mactav-text-muted);
  line-height: 1.65;
}

.summary-card {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid rgba(0, 217, 192, 0.22);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.62);
}

.summary-card span {
  display: block;
  margin-bottom: 10px;
  color: var(--mactav-cyber-blue);
  font-weight: 950;
}

ul {
  display: grid;
  gap: 6px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

li {
  padding: 8px 10px;
  border-radius: 12px;
  color: var(--mactav-text-soft);
  background: rgba(0, 98, 255, 0.06);
  font-size: 12px;
}

@media (max-width: 980px) {
  .mission-head,
  .panel-head {
    flex-direction: column;
  }

  .mission-grid {
    grid-template-columns: 1fr;
  }
}
</style>
