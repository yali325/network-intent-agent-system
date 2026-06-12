<template>
  <div v-if="apiModeStore.isReal || task" class="task-page page-frame">
    <GlassPanel>
      <div v-if="!apiModeStore.isReal && task" class="mission-head">
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

      <div v-else-if="apiModeStore.isReal" class="mission-head">
        <div>
          <div class="eyebrow">Real Mission Control</div>
          <h1 class="page-title">当前任务: {{ realTaskIdLabel }}</h1>
          <p class="intent">真实模式按 mock 指挥舱视觉呈现，仅使用后端真实 API 返回的数据；未就绪能力会在原位置显示明确占位。</p>
        </div>
        <div class="status-stack">
          <StatusPill tone="signal">real</StatusPill>
          <button class="glass-action" type="button" @click="confirmNewTask">新建意图任务</button>
        </div>
      </div>

      <div v-if="apiModeStore.isReal" class="real-status-bar">
        <StatusPill tone="signal">任务 ID: {{ realTaskIdLabel }}</StatusPill>
        <StatusPill v-if="store.realJobId" tone="pending">jobId: {{ store.realJobId }}</StatusPill>
        <StatusPill v-if="store.realJob" :tone="realJobTone">jobStatus: {{ store.realJob.jobStatus }}</StatusPill>
        <StatusPill v-if="store.realJob?.jobType" tone="pending">jobType: {{ store.realJob.jobType }}</StatusPill>
        <div v-if="store.realJob?.errorCode || store.realJob?.errorMessage" class="real-error">
          <strong>{{ store.realJob.errorCode }}</strong>
          <p>{{ store.realJob.errorMessage }}</p>
        </div>
        <div v-if="store.realError" class="real-error">
          <p>{{ store.realError }}</p>
        </div>
      </div>
    </GlassPanel>

    <template v-if="!apiModeStore.isReal && task">
      <GlassPanel compact>
        <div class="view-switcher" :class="{ 'with-repair': store.activeView === 'repair' }">
          <button
            v-for="tab in topLevelViews"
            :key="tab.key"
            type="button"
            :class="{ active: store.activeView === tab.key }"
            @click="store.activeView = tab.key"
          >
            <span>{{ tab.title }}</span>
            <small>{{ tab.caption }}</small>
          </button>
          <button v-if="store.activeView === 'repair'" class="repair-chip active" type="button">
            <span>AI 自愈诊断舱</span>
            <small>验证失败触发的临时视图</small>
          </button>
        </div>
      </GlassPanel>

      <template v-if="store.activeView === 'control'">
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

        <NetworkTopologyLiveBoard
          :task="task"
          :healing-state="store.topologyHealingState"
          :policy-state="store.topologyPolicyState"
        />
      </template>

      <ValidationBoard v-else-if="store.activeView === 'validation'" :assertions="store.validationAssertions" />
      <RepairCockpit v-else :plan="store.repairPlan" />

      <button class="append-tab" type="button" @click="drawerOpen = true">追加新指令</button>

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
            在当前拓扑、配置和验证结果基础上继续提问。这里先触发 mock 的 appendIntent，后续可对接真实多轮 Agent 链路。
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
    </template>

    <RealTaskMissionView v-else-if="apiModeStore.isReal" :task-id="realTaskIdLabel" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { storeToRefs } from "pinia";
import { message, Modal } from "ant-design-vue";
import { useRouter } from "vue-router";
import GlassPanel from "@/components/GlassPanel.vue";
import NetworkTopologyLiveBoard from "@/components/NetworkTopologyLiveBoard.vue";
import RealTaskMissionView from "@/components/RealTaskMissionView.vue";
import RepairCockpit from "@/components/RepairCockpit.vue";
import StatusPill from "@/components/StatusPill.vue";
import ValidationBoard from "@/components/ValidationBoard.vue";
import WorkflowTopology from "@/components/WorkflowTopology.vue";
import type { MissionView } from "@/api/futureContracts";
import { useApiModeStore } from "@/stores/apiModeStore";
import { useTaskStore } from "@/stores/taskStore";

const props = defineProps<{ taskId: string }>();
const router = useRouter();
const store = useTaskStore();
const apiModeStore = useApiModeStore();
const { activeTask: task } = storeToRefs(store);
const drawerOpen = ref(false);
const appendText = ref("");
const appendLoading = ref(false);

const topLevelViews: Array<{ key: Exclude<MissionView, "repair">; title: string; caption: string }> = [
  { key: "control", title: "Agent 协同指挥舱", caption: "拓扑 / VRP / 阶段流" },
  { key: "validation", title: "意图自动化验证", caption: "断言 / 证据 / 冲突" },
];

const realJobTone = computed<"running" | "signal" | "bad" | "pending">(() => {
  if (!store.realJob) return "pending";
  switch (store.realJob.jobStatus) {
    case "RUNNING":
      return "running";
    case "SUCCESS":
      return "signal";
    case "FAILED":
    case "INTERRUPTED":
    case "CANCELLED":
      return "bad";
    default:
      return "pending";
  }
});

const realTaskIdLabel = computed(() => store.realTaskId ?? props.taskId);

onMounted(async () => {
  try {
    await store.loadTask(props.taskId);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    void message.error(msg);
  }
});

function confirmNewTask(): void {
  Modal.confirm({
    title: "确认新建意图任务？",
    content: "离开当前任务指挥舱后，当前页面状态会保留在 store 中；输入页将准备接收新的自然语言意图。",
    okText: "回到输入页",
    cancelText: "继续查看当前任务",
    centered: true,
    async onOk() {
      store.prepareNewIntent();
      await router.push("/console");
    },
  });
}

async function submitAppendIntent(): Promise<void> {
  const text = appendText.value.trim();
  if (!text) return;
  appendLoading.value = true;
  try {
    await store.appendIntent(text);
    void message.success("追加指令已进入 mock 多轮队列");
    appendText.value = "";
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
  white-space: pre-line;
}

.status-stack,
.real-status-bar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.real-status-bar {
  justify-content: flex-start;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(0, 217, 192, 0.18);
}

.real-error {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid rgba(220, 38, 38, 0.28);
  border-radius: 12px;
  color: #dc2626;
  background: rgba(254, 226, 226, 0.42);
}

.real-error strong {
  display: block;
  color: #b91c1c;
  font-size: 12px;
}

.real-error p {
  margin: 4px 0 0;
  line-height: 1.5;
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

.view-switcher {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.view-switcher.with-repair {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.view-switcher button {
  position: relative;
  min-height: 68px;
  padding: 13px 16px;
  overflow: hidden;
  border: 1px solid rgba(110, 155, 215, 0.22);
  border-radius: 18px;
  color: var(--mactav-text-main);
  text-align: left;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.68), rgba(235, 247, 255, 0.54)),
    radial-gradient(circle at 100% 0%, rgba(0, 217, 192, 0.12), transparent 34%);
  cursor: pointer;
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.view-switcher button::after {
  position: absolute;
  inset: auto 14px 10px 14px;
  height: 2px;
  content: "";
  background: linear-gradient(90deg, transparent, rgba(0, 98, 255, 0.74), rgba(0, 217, 192, 0.72), transparent);
  opacity: 0;
  transform: translateX(-18%);
  transition: opacity 160ms ease, transform 160ms ease;
}

.view-switcher button.active {
  border-color: rgba(0, 98, 255, 0.44);
  box-shadow: 0 16px 34px rgba(31, 91, 180, 0.13);
  transform: translateY(-1px);
}

.view-switcher button.active::after {
  opacity: 1;
  transform: translateX(0);
}

.view-switcher span,
.view-switcher small {
  display: block;
}

.view-switcher span {
  font-weight: 950;
}

.view-switcher small {
  margin-top: 4px;
  color: var(--mactav-text-muted);
}

.repair-chip {
  border-color: rgba(245, 158, 11, 0.28) !important;
  background:
    radial-gradient(circle at 88% 16%, rgba(245, 158, 11, 0.17), transparent 34%),
    rgba(255, 255, 255, 0.62) !important;
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
  padding: 5px 12px;
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

  .mission-grid,
  .view-switcher {
    grid-template-columns: 1fr;
  }
}
</style>
