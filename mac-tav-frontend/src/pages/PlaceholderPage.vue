<template>
  <div class="page-frame">
    <GlassPanel>
      <div class="placeholder">
        <div class="eyebrow">预留入口</div>
        <h1 class="page-title">{{ sectionTitle }}</h1>
        <p>
          本轮只实现 `/console` 与 `/tasks/:taskId` 的视觉基线。该入口已预留，后续可接入真实 artifacts、validation、repair、jobs 或 events 页面。
        </p>
        <router-link :to="`/tasks/${taskId}`">返回任务指挥舱</router-link>
      </div>
    </GlassPanel>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import GlassPanel from '@/components/GlassPanel.vue';

const props = defineProps<{ taskId: string; section?: string }>();

const sectionTitle = computed(() => {
  const map: Record<string, string> = {
    artifacts: '阶段产物入口',
    validation: '验证证据入口',
    repair: '修复闭环入口',
    jobs: '异步任务入口',
    events: '事件流入口'
  };
  return map[props.section ?? ''] ?? '预留页面';
});
</script>

<style scoped>
.placeholder {
  min-height: 420px;
  display: grid;
  align-content: center;
  justify-items: start;
}

p {
  max-width: 760px;
  color: var(--mactav-text-muted);
  font-size: 17px;
  line-height: 1.75;
}

a {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 999px;
  color: white;
  background: var(--mactav-cyber-blue);
  text-decoration: none;
  font-weight: 900;
}
</style>
