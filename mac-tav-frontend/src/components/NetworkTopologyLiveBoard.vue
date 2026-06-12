<template>
  <GlassPanel>
    <div class="board-head">
      <div>
        <div class="eyebrow">拓扑与配置遥测看板</div>
        <h2>Config Applied / Huawei VRP</h2>
      </div>
      <StatusPill :tone="statusTone">状态: {{ statusLabel }}</StatusPill>
    </div>
    <div class="board-grid">
      <DeviceTopologyCanvas
        :devices="resolvedDevices"
        :links="resolvedLinks"
        :initial-selected-device-id="selectedDeviceId"
        :healing-state="healingState"
        :policy-state="policyState"
        :show-demo-overlays="showDemoOverlays"
        :empty-message="emptyMessage"
        @select-device="selectDevice"
      />
      <DeviceCommandPanel :config="selectedConfig" :analyzing="analyzing" :empty-message="emptyMessage" />
    </div>
  </GlassPanel>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { DemoTask, DeviceConfig, TopologyDevice, TopologyLink } from '@/api/types';
import DeviceCommandPanel from '@/components/DeviceCommandPanel.vue';
import DeviceTopologyCanvas from '@/components/DeviceTopologyCanvas.vue';
import GlassPanel from '@/components/GlassPanel.vue';
import StatusPill from '@/components/StatusPill.vue';

const props = withDefaults(defineProps<{
  task?: DemoTask;
  devices?: TopologyDevice[];
  links?: TopologyLink[];
  deviceConfigs?: DeviceConfig[];
  statusLabel?: string;
  statusTone?: 'running' | 'ok' | 'bad' | 'warn' | 'pending' | 'signal';
  emptyMessage?: string;
  showDemoOverlays?: boolean;
  healingState?: 'normal' | 'failed' | 'healing';
  policyState?: 'conflict' | 'approved' | 'repaired';
}>(), {
  task: undefined,
  devices: undefined,
  links: undefined,
  deviceConfigs: undefined,
  statusLabel: 'Config Applied',
  statusTone: 'ok',
  emptyMessage: '真实拓扑或配置产物尚未生成。',
  showDemoOverlays: true,
  healingState: 'normal',
  policyState: 'conflict'
});

const emptyConfig: DeviceConfig = {
  deviceId: 'not-ready',
  title: '配置未就绪',
  subtitle: 'CONFIG_SET_NOT_FOUND',
  commands: []
};

const resolvedDevices = computed(() => props.devices ?? props.task?.topology.devices ?? []);
const resolvedLinks = computed(() => props.links ?? props.task?.topology.links ?? []);
const resolvedConfigs = computed(() => props.deviceConfigs ?? props.task?.deviceConfigs ?? []);
const selectedDeviceId = ref(resolvedConfigs.value[0]?.deviceId ?? resolvedDevices.value[0]?.id ?? 'not-ready');
const analyzing = ref(false);
let timer: number | undefined;

const selectedConfig = computed(() => resolvedConfigs.value.find((config) => config.deviceId === selectedDeviceId.value) ?? resolvedConfigs.value[0] ?? emptyConfig);

watch(resolvedConfigs, (configs) => {
  if (!configs.some((config) => config.deviceId === selectedDeviceId.value)) {
    selectedDeviceId.value = configs[0]?.deviceId ?? resolvedDevices.value[0]?.id ?? 'not-ready';
  }
});

function selectDevice(deviceId: string): void {
  if (deviceId === selectedDeviceId.value) return;
  selectedDeviceId.value = deviceId;
  analyzing.value = true;
  window.clearTimeout(timer);
  timer = window.setTimeout(() => {
    analyzing.value = false;
  }, 300);
}
</script>

<style scoped>
.board-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

h2 {
  margin: 6px 0 0;
  color: var(--mactav-text-main);
}

.board-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(390px, 0.92fr);
  gap: 16px;
}

@media (max-width: 1080px) {
  .board-grid {
    grid-template-columns: 1fr;
  }
}
</style>
