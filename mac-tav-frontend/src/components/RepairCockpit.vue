<template>
  <GlassPanel>
    <div class="section-head">
      <div>
        <div class="eyebrow">Repair Cockpit / Demo Self-Healing</div>
        <h2>AI 自愈诊断舱</h2>
        <p>候选动作等待 Orchestrator 受控处理；前端不执行 CLI，不真实下发网络配置。</p>
      </div>
      <div class="repair-head-actions">
        <button class="back-validation" type="button" @click="store.activeView = 'validation'">返回验证证据矩阵</button>
        <StatusPill :tone="phaseTone">{{ store.repairPhase }}</StatusPill>
      </div>
    </div>

    <div class="repair-layout">
      <RcaReportPanel :rca="plan.rca" />
      <RepairActionPipeline :actions="plan.actions" :selected-action-id="store.selectedRepairActionId" @select="store.selectRepairAction" />
    </div>

    <div class="approval-zone">
      <SlideToApprove
        :approved="selectedAction?.status === 'APPROVED' || selectedAction?.status === 'APPLIED'"
        :disabled="!selectedAction || applying"
        @approved="approve"
      />
      <a-button class="apply-button" :class="{ ready: canApply }" type="primary" :loading="applying" :disabled="!canApply" @click="apply">
        提交修复 Apply
      </a-button>
      <span class="approval-note">
        mock/demo：Apply 后播放 1.5s 修复过渡并回到指挥舱；real：只提交真实 job，等待后端状态刷新，不伪造成功。
      </span>
    </div>
  </GlassPanel>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { RepairPlanDemo } from '@/api/futureContracts';
import { apiMode } from '@/api/config';
import GlassPanel from '@/components/GlassPanel.vue';
import RcaReportPanel from '@/components/RcaReportPanel.vue';
import RepairActionPipeline from '@/components/RepairActionPipeline.vue';
import SlideToApprove from '@/components/SlideToApprove.vue';
import StatusPill from '@/components/StatusPill.vue';
import { useTaskStore } from '@/stores/taskStore';

defineProps<{ plan: RepairPlanDemo }>();

const store = useTaskStore();
const applying = ref(false);
const selectedAction = computed(() => store.selectedRepairAction);
const canApply = computed(() => selectedAction.value?.status === 'APPROVED' && !applying.value);
const phaseTone = computed(() => (store.repairPhase === 'applied' ? 'ok' : store.repairPhase === 'applying' ? 'warn' : 'signal'));

async function approve(): Promise<void> {
  await store.approveSelectedRepairAction();
  message.success(apiMode === 'real' ? '修复动作已授权，等待 Apply' : 'mock/demo：人工授权成功');
}

async function apply(): Promise<void> {
  applying.value = true;
  try {
    const jobId = await store.applySelectedRepairAction();
    if (apiMode === 'real') {
      message.success(`修复动作已提交：${jobId}`);
    } else {
      message.loading({ content: `mock/demo Apply 已提交：${jobId}`, key: 'mock-apply', duration: 1.2 });
      window.setTimeout(() => {
        message.success({ content: 'Mock 修复闭环已完成，拓扑已切换为演示安全隔离态。', key: 'mock-apply', duration: 2.2 });
      }, 1500);
    }
  } finally {
    if (apiMode === 'real') {
      applying.value = false;
    } else {
      window.setTimeout(() => {
        applying.value = false;
      }, 1500);
    }
  }
}
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

h2 {
  margin: 6px 0 0;
  color: var(--mactav-text-main);
}

.section-head p {
  margin: 10px 0 0;
  color: var(--mactav-text-muted);
}

.repair-head-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.back-validation {
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

.repair-layout {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(380px, 1.08fr);
  gap: 16px;
}

.approval-zone {
  display: grid;
  grid-template-columns: minmax(320px, 0.42fr) auto;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
}

.apply-button.ready {
  box-shadow: 0 12px 30px rgba(0, 98, 255, 0.22), 0 0 0 1px rgba(0, 217, 192, 0.18);
}

.approval-note {
  grid-column: 1 / -1;
  color: var(--mactav-text-muted);
  font-size: 12px;
}

@media (max-width: 980px) {
  .section-head,
  .repair-layout,
  .approval-zone {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .repair-head-actions {
    justify-content: flex-start;
  }
}
</style>
