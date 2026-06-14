<template>
  <div class="real-mission">
    <GlassPanel compact>
      <div class="view-switcher" :class="{ 'with-repair': activeRealView === 'repair' }">
        <button
          v-for="tab in topLevelViews"
          :key="tab.key"
          type="button"
          :class="{ active: activeRealView === tab.key }"
          @click="activeRealView = tab.key"
        >
          <span>{{ tab.title }}</span>
          <small>{{ tab.caption }}</small>
        </button>
        <button v-if="activeRealView === 'repair'" class="repair-chip active" type="button">
          <span>AI 自愈诊断舱</span>
          <small>真实 RepairPlan 未就绪时显示占位</small>
        </button>
      </div>
      <div class="sync-strip">
        <StatusPill :tone="store.isRealRefreshing ? 'running' : 'pending'">
          {{ store.isRealRefreshing ? "实时同步中" : "实时观察" }}
        </StatusPill>
        <span>最后更新：{{ lastRefreshLabel }}</span>
        <span v-if="store.realRefreshError" class="sync-error">刷新失败，稍后重试：{{ store.realRefreshError }}</span>
      </div>
    </GlassPanel>

    <template v-if="activeRealView === 'control'">
      <GlassPanel>
        <WorkflowTopology
          :current-stage="currentStage"
          :selected-stage="selectedStage"
          :stage-nodes="stageNodes"
          @select-stage="selectedStage = $event"
        />
      </GlassPanel>

      <div class="mission-grid">
        <GlassPanel compact>
          <div class="panel-head">
            <div>
              <div class="eyebrow">Telemetry Preview</div>
              <h2>最近事件</h2>
            </div>
            <StatusPill tone="pending">{{ telemetryEvents.length }} events</StatusPill>
          </div>
          <PanelState :panel="store.realViewPanels.trace" />
          <div class="telemetry-list">
            <article v-for="event in telemetryEvents" :key="event.id" class="telemetry-item">
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
              <h2>{{ artifactPreview.agentName }}</h2>
            </div>
            <StatusPill :tone="artifactPreview.tone">{{ selectedStage }}</StatusPill>
          </div>
          <PanelState :panel="store.realViewPanels.summary" />
          <div class="summary-card">
            <span>产物: {{ artifactPreview.artifactName }} {{ artifactPreview.versionLabel }}</span>
            <p>{{ artifactPreview.summary }}</p>
            <ul>
              <li v-for="item in artifactPreview.commandDigest" :key="item" class="mono">{{ item }}</li>
            </ul>
          </div>
        </GlassPanel>
      </div>

      <NetworkTopologyLiveBoard
        :devices="topologyBoard.devices"
        :links="topologyBoard.links"
        :device-configs="topologyBoard.configs"
        :status-label="topologyBoard.statusLabel"
        :status-tone="topologyStatusTone"
        :empty-message="topologyBoard.emptyMessage"
        :show-demo-overlays="false"
        healing-state="normal"
        policy-state="approved"
      />

      <GlassPanel compact>
        <div class="panel-head">
          <div>
            <div class="eyebrow">Backend Required</div>
            <h2>仍需后端支撑的细粒度视图</h2>
          </div>
          <button class="glass-action" type="button" @click="refresh">刷新真实视图</button>
        </div>
        <div class="not-implemented-list">
          <span v-for="item in store.realNotImplemented" :key="item">该能力需要后端实现：{{ item }}</span>
        </div>
      </GlassPanel>
    </template>

    <template v-else-if="activeRealView === 'validation'">
      <GlassPanel>
        <div class="section-head">
          <div>
            <div class="eyebrow">Validation Board / Real Evidence</div>
            <h2>验证证据矩阵</h2>
            <p>这里沿用 mock 的验证看板结构，但断言和证据只来自真实 validation / artifact / execution 数据。</p>
          </div>
          <div class="board-actions">
            <a-button @click="refresh">刷新真实验证数据</a-button>
            <a-button type="primary" danger @click="activeRealView = 'repair'">进入修复分析视图</a-button>
          </div>
        </div>
        <PanelState :panel="store.realViewPanels.executionLogs" />
        <div class="validation-grid">
          <div class="assertion-grid-wrap">
            <ValidationAssertionCard v-for="assertion in validationAssertions" :key="assertion.id" :assertion="assertion" />
          </div>
          <EvidenceInspectorPanel :evidence="evidenceInspector">
            <template #action>
              <button class="repair-entry" type="button" @click="activeRealView = 'repair'">
                <span>真实修复视图</span>
                <strong>进入 AI 自愈诊断舱</strong>
              </button>
            </template>
          </EvidenceInspectorPanel>
        </div>
      </GlassPanel>
    </template>

    <template v-else>
      <GlassPanel>
        <div class="section-head">
          <div>
            <div class="eyebrow">Repair Cockpit / Real Self-Healing</div>
            <h2>AI 自愈诊断舱</h2>
            <p>真实模式只展示后端 RepairPlan；如果后端未生成修复计划，则在同款驾驶舱内显示 NOT_READY。</p>
          </div>
          <div class="repair-head-actions">
            <button class="back-validation" type="button" @click="activeRealView = 'validation'">返回验证证据矩阵</button>
            <StatusPill :tone="repairPlan.actions.length ? 'signal' : 'pending'">{{ repairPlan.actions.length ? 'READY' : 'REPAIR_PLAN_NOT_READY' }}</StatusPill>
          </div>
        </div>

        <div class="repair-layout">
          <RcaReportPanel :rca="repairPlan.rca" />
          <section class="pipeline">
            <div class="eyebrow">RepairAction Pipeline</div>
            <RepairActionPipeline
              v-if="repairPlan.actions.length"
              :actions="repairPlan.actions"
              :selected-action-id="selectedRepairActionId"
              @select="selectedRepairActionId = $event"
            />
            <div v-else class="empty-pipeline">
              <strong>REPAIR_PLAN_NOT_READY</strong>
              <p>后端尚未生成 RepairPlan，暂不展示任何推测修复动作。</p>
            </div>
          </section>
        </div>

        <div class="approval-zone">
          <button class="approval-placeholder" type="button" disabled>等待真实修复动作</button>
          <a-button type="primary" disabled>提交修复 Apply</a-button>
          <span class="approval-note">真实模式不会伪造修复成功；后续由后端 repair action API 返回状态。</span>
        </div>
      </GlassPanel>
    </template>

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
        <p class="drawer-copy">真实追加指令接口尚未实现，本入口只保留与 mock 一致的交互位置。</p>
        <a-textarea
          v-model:value="appendText"
          class="append-input"
          :auto-size="{ minRows: 7, maxRows: 10 }"
          placeholder="输入追加意图后会提示接口待实现，不会调用 mock appendIntent。"
        />
        <div class="drawer-actions">
          <a-button @click="appendText = ''">清空</a-button>
          <a-button type="primary" :disabled="!appendText.trim()" @click="submitRealAppend">发送追加指令</a-button>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, ref, watch } from "vue";
import { message } from "ant-design-vue";
import type { MissionView } from "@/api/futureContracts";
import type { WorkflowStage } from "@/api/types";
import {
  buildArtifactPreview,
  buildEvidenceInspector,
  buildRepairPlan,
  buildStageFlowNodes,
  buildTelemetryEvents,
  buildTopologyBoard,
  buildValidationAssertions,
  normalizeStage,
} from "@/api/realViewAdapters";
import EvidenceInspectorPanel from "@/components/EvidenceInspectorPanel.vue";
import GlassPanel from "@/components/GlassPanel.vue";
import NetworkTopologyLiveBoard from "@/components/NetworkTopologyLiveBoard.vue";
import RcaReportPanel from "@/components/RcaReportPanel.vue";
import RepairActionPipeline from "@/components/RepairActionPipeline.vue";
import StatusPill from "@/components/StatusPill.vue";
import ValidationAssertionCard from "@/components/ValidationAssertionCard.vue";
import WorkflowTopology from "@/components/WorkflowTopology.vue";
import { useTaskStore } from "@/stores/taskStore";

const props = defineProps<{ taskId: string }>();
const store = useTaskStore();
const activeRealView = ref<MissionView>("control");
const selectedStage = ref<WorkflowStage>("CONFIGURATION");
const selectedRepairActionId = ref("");
const drawerOpen = ref(false);
const appendText = ref("");

const topLevelViews: Array<{ key: Exclude<MissionView, "repair">; title: string; caption: string }> = [
  { key: "control", title: "Agent 协同指挥舱", caption: "拓扑 / VRP / 阶段流" },
  { key: "validation", title: "意图自动化验证", caption: "断言 / 证据 / 冲突" },
];

const currentStage = computed(() => normalizeStage(store.realWorkspaceSummary?.currentStage ?? store.realWorkspace?.currentStage ?? store.realWorkspace?.task?.currentStage));
const stageNodes = computed(() => buildStageFlowNodes(store.realWorkspaceSummary, store.realWorkflowTrace, store.realJob));
const telemetryEvents = computed(() => buildTelemetryEvents(store.realEvents, store.realWorkflowTrace, store.realExecutionLogs));
const artifactPreview = computed(() => buildArtifactPreview(selectedStage.value, store.realWorkspaceSummary, store.realArtifacts, store.realConfigBlocks));
const topologyBoard = computed(() => buildTopologyBoard(store.realTopology, store.realConfigBlocks));
const validationAssertions = computed(() => buildValidationAssertions(store.realValidation, store.realValidationItems));
const evidenceInspector = computed(() => buildEvidenceInspector(store.realArtifacts, store.realExecutionLogs));
const repairPlan = computed(() => buildRepairPlan(store.realRepairPlan));
const topologyStatusTone = computed<"running" | "ok" | "bad" | "warn" | "pending" | "signal">(() => {
  const status = store.realConfigBlocks?.status ?? store.realTopology?.status;
  if (status === "READY" || status === "SUCCESS") return "ok";
  if (status === "PARTIAL") return "warn";
  if (status === "FAILED") return "bad";
  return "pending";
});
const lastRefreshLabel = computed(() => {
  if (!store.lastRealRefreshAt) return "尚未刷新";
  const date = new Date(store.lastRealRefreshAt);
  if (Number.isNaN(date.getTime())) return store.lastRealRefreshAt;
  return date.toLocaleTimeString("zh-CN", { hour12: false });
});

watch(currentStage, (stage) => {
  selectedStage.value = stage;
}, { immediate: true });

watch(repairPlan, (plan) => {
  selectedRepairActionId.value = plan.actions[0]?.actionId ?? "";
}, { immediate: true });

const PanelState = defineComponent({
  props: {
    panel: {
      type: Object as () => { loading?: boolean; status?: string; error?: string | null },
      required: true,
    },
  },
  setup(panelProps) {
    return () => {
      if (panelProps.panel.loading) return h("p", { class: "panel-state" }, "加载中...");
      if (panelProps.panel.error) return h("p", { class: "panel-error" }, panelProps.panel.error);
      if (panelProps.panel.status === "NOT_READY") return h("p", { class: "panel-state" }, "后端数据未就绪");
      return null;
    };
  },
});

async function refresh(): Promise<void> {
  try {
    await store.refreshRealMissionOnce(props.taskId, { jobId: store.realJobId ?? undefined });
    if (store.realRefreshError) {
      void message.warning("部分真实视图刷新失败，请查看面板错误信息");
    } else {
      void message.success("真实视图已刷新");
    }
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    void message.error(msg);
  }
}

function submitRealAppend(): void {
  void message.info("real 追加指令接口待实现");
  drawerOpen.value = false;
  appendText.value = "";
}
</script>

<style scoped>
.real-mission {
  display: grid;
  gap: 18px;
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

.sync-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  color: var(--mactav-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.sync-error {
  max-width: 100%;
  padding: 5px 9px;
  border: 1px solid rgba(220, 38, 38, 0.22);
  border-radius: 999px;
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.5);
  overflow-wrap: anywhere;
}

.repair-chip {
  border-color: rgba(245, 158, 11, 0.28) !important;
  background:
    radial-gradient(circle at 88% 16%, rgba(245, 158, 11, 0.17), transparent 34%),
    rgba(255, 255, 255, 0.62) !important;
}

.mission-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: 18px;
}

.panel-head,
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

h2 {
  margin: 6px 0 0;
  color: var(--mactav-text-main);
}

.section-head p {
  max-width: 760px;
  margin: 10px 0 0;
  color: var(--mactav-text-muted);
  line-height: 1.6;
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

.validation-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.6fr);
  gap: 16px;
}

.assertion-grid-wrap {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.board-actions,
.repair-head-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.repair-entry {
  display: grid;
  gap: 3px;
  width: 100%;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid rgba(239, 68, 68, 0.28);
  border-radius: 16px;
  color: #991b1b;
  text-align: left;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(254, 242, 242, 0.72)),
    radial-gradient(circle at 100% 20%, rgba(239, 68, 68, 0.16), transparent 40%);
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(239, 68, 68, 0.1);
}

.repair-entry span {
  color: #ef4444;
  font-size: 12px;
  font-weight: 900;
}

.repair-entry strong {
  color: var(--mactav-text-main);
}

.repair-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(380px, 1.08fr);
  gap: 16px;
}

.pipeline {
  display: grid;
  gap: 12px;
}

.empty-pipeline {
  display: grid;
  gap: 8px;
  min-height: 180px;
  place-items: center;
  padding: 22px;
  border: 1px dashed rgba(0, 98, 255, 0.24);
  border-radius: 20px;
  color: var(--mactav-text-muted);
  text-align: center;
  background: rgba(255, 255, 255, 0.58);
}

.empty-pipeline strong {
  color: var(--mactav-cyber-blue);
  font-family: "Cascadia Code", Consolas, monospace;
}

.approval-zone {
  display: grid;
  grid-template-columns: minmax(320px, 0.42fr) auto;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
}

.approval-placeholder,
.back-validation,
.glass-action {
  min-height: 32px;
  padding: 6px 13px;
  border: 1px solid rgba(110, 155, 215, 0.28);
  border-radius: 999px;
  color: var(--mactav-cyber-blue);
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 10px 24px rgba(31, 91, 180, 0.1);
  cursor: pointer;
  font-weight: 900;
  backdrop-filter: blur(14px);
}

.approval-placeholder:disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.approval-note {
  grid-column: 1 / -1;
  color: var(--mactav-text-muted);
  font-size: 12px;
}

.panel-state {
  margin: 12px 0 0;
  color: var(--mactav-cyber-blue);
  font-weight: 800;
}

.panel-error {
  margin: 12px 0 0;
  padding: 10px 12px;
  border: 1px solid rgba(220, 38, 38, 0.24);
  border-radius: 14px;
  color: #b91c1c;
  background: rgba(254, 226, 226, 0.48);
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.not-implemented-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.not-implemented-list span {
  padding: 8px 10px;
  border: 1px solid rgba(245, 158, 11, 0.28);
  border-radius: 999px;
  color: #92400e;
  background: rgba(255, 251, 235, 0.72);
  font-size: 12px;
  font-weight: 800;
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

@media (max-width: 1180px) {
  .mission-grid,
  .validation-grid,
  .assertion-grid-wrap,
  .repair-layout,
  .approval-zone,
  .view-switcher {
    grid-template-columns: 1fr;
  }

  .panel-head,
  .section-head {
    flex-direction: column;
  }
}
</style>
