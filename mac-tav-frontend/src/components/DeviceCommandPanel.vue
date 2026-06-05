<template>
  <div class="command-panel">
    <div class="device-card">
      <div>
        <span class="eyebrow">单设备命令视窗</span>
        <h3>{{ config.title }}</h3>
        <p>{{ config.subtitle }}</p>
      </div>
      <a-button size="small" @click="copyCommands">复制命令</a-button>
    </div>
    <div v-if="analyzing" class="analyzing">
      <span />
      正在分析所选设备配置...
    </div>
    <HuaweiCommandHighlighter v-else :commands="config.commands" />
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue';
import type { DeviceConfig } from '@/api/types';
import HuaweiCommandHighlighter from '@/components/HuaweiCommandHighlighter.vue';

const props = defineProps<{
  config: DeviceConfig;
  analyzing: boolean;
}>();

async function copyCommands(): Promise<void> {
  await navigator.clipboard.writeText(props.config.commands.join('\n'));
  message.success('命令已复制');
}
</script>

<style scoped>
.command-panel {
  display: grid;
  gap: 14px;
}

.device-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(0, 98, 255, 0.16);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.68);
}

h3 {
  margin: 8px 0 4px;
  color: var(--mactav-text-main);
  font-size: 22px;
}

p {
  margin: 0;
  color: var(--mactav-text-muted);
}

.analyzing {
  display: grid;
  min-height: 438px;
  place-items: center;
  border: 1px solid rgba(110, 155, 215, 0.28);
  border-radius: 18px;
  color: var(--mactav-cyber-blue);
  background: rgba(255, 255, 255, 0.58);
  font-weight: 900;
}

.analyzing span {
  width: 42px;
  height: 42px;
  margin-bottom: 12px;
  border: 3px solid rgba(0, 98, 255, 0.18);
  border-top-color: var(--mactav-cyber-blue);
  border-radius: 999px;
  animation: spin 800ms linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
