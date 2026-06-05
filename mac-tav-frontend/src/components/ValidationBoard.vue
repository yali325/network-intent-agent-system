<template>
  <GlassPanel>
    <div class="section-head">
      <div>
        <div class="eyebrow">Validation Board / Demo Evidence</div>
        <h2>验证证据矩阵</h2>
        <p>
          这里聚焦意图断言、验证证据和冲突定位。mock 模式展示 fixture evidence；real 模式只提交真实 validation /
          repair job，不伪造结果。
        </p>
      </div>
      <div class="board-actions">
        <a-button :loading="validating" @click="runValidation">重新验证</a-button>
        <a-button v-if="hasFailedAssertion" type="primary" danger :loading="analyzing" @click="enterRepair">
          发现配置冲突，进入修复分析
        </a-button>
      </div>
    </div>

    <div v-if="!hasFailedAssertion" class="all-clear">
      <span class="pulse-dot"></span>
      全部核心意图断言已通过，当前策略满足业务隔离与连通性目标。
    </div>

    <div class="validation-grid">
      <div class="assertion-grid-wrap">
        <ValidationAssertionCard v-for="assertion in assertions" :key="assertion.id" :assertion="assertion" />
      </div>
      <EvidenceInspectorPanel :evidence="evidenceInspectorFixture">
        <template #action>
          <button v-if="hasFailedAssertion" class="repair-entry" type="button" :disabled="analyzing" @click="enterRepair">
            <span>发现配置冲突</span>
            <strong>进入 AI 自愈诊断舱</strong>
          </button>
        </template>
      </EvidenceInspectorPanel>
    </div>
  </GlassPanel>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { ValidationAssertionDemo } from '@/api/futureContracts';
import { apiMode } from '@/api/config';
import EvidenceInspectorPanel from '@/components/EvidenceInspectorPanel.vue';
import GlassPanel from '@/components/GlassPanel.vue';
import ValidationAssertionCard from '@/components/ValidationAssertionCard.vue';
import { evidenceInspectorFixture } from '@/fixtures/futureValidationDemo';
import { useTaskStore } from '@/stores/taskStore';

const props = defineProps<{ assertions: ValidationAssertionDemo[] }>();

const store = useTaskStore();
const validating = ref(false);
const analyzing = ref(false);
const hasFailedAssertion = computed(() => props.assertions.some((assertion) => assertion.status === 'FAILED'));

async function runValidation(): Promise<void> {
  validating.value = true;
  try {
    const jobId = await store.runValidation();
    message.success(apiMode === 'real' ? `验证任务已提交：${jobId}` : 'mock 验证完成：访客区到服务器区冲突仍可见');
  } finally {
    validating.value = false;
  }
}

async function enterRepair(): Promise<void> {
  if (!hasFailedAssertion.value) return;
  analyzing.value = true;
  try {
    const jobId = await store.analyzeRepair();
    message.success(apiMode === 'real' ? `修复分析任务已提交：${jobId}` : 'mock 修复分析已生成');
  } finally {
    analyzing.value = false;
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
  max-width: 760px;
  margin: 10px 0 0;
  color: var(--mactav-text-muted);
  line-height: 1.6;
}

.board-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.all-clear {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border: 1px solid rgba(16, 185, 129, 0.24);
  border-radius: 16px;
  color: #047857;
  background: rgba(236, 253, 245, 0.68);
  font-weight: 800;
}

.pulse-dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: var(--mactav-success);
  box-shadow: 0 0 0 7px rgba(16, 185, 129, 0.12);
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
  transition: transform 160ms ease, box-shadow 160ms ease;
}

.repair-entry:hover {
  box-shadow: 0 18px 34px rgba(239, 68, 68, 0.16);
  transform: translateY(-1px);
}

.repair-entry:disabled {
  cursor: wait;
  opacity: 0.72;
}

.repair-entry span {
  color: #ef4444;
  font-size: 12px;
  font-weight: 900;
}

.repair-entry strong {
  color: var(--mactav-text-main);
}

@media (max-width: 1180px) {
  .validation-grid,
  .assertion-grid-wrap {
    grid-template-columns: 1fr;
  }
}
</style>
