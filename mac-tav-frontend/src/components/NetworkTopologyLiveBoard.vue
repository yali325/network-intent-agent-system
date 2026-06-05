<template>
  <GlassPanel>
    <div class="board-head">
      <div>
        <div class="eyebrow">拓扑与配置遥测看板</div>
        <h2>Config Applied / Huawei VRP</h2>
      </div>
      <StatusPill tone="ok">状态: Config Applied</StatusPill>
    </div>
    <div class="board-grid">
      <DeviceTopologyCanvas
        :devices="task.topology.devices"
        :links="task.topology.links"
        :initial-selected-device-id="selectedDeviceId"
        :healing-state="healingState"
        :policy-state="policyState"
        @select-device="selectDevice"
      />
      <DeviceCommandPanel :config="selectedConfig" :analyzing="analyzing" />
    </div>
  </GlassPanel>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { DemoTask } from '@/api/types';
import DeviceCommandPanel from '@/components/DeviceCommandPanel.vue';
import DeviceTopologyCanvas from '@/components/DeviceTopologyCanvas.vue';
import GlassPanel from '@/components/GlassPanel.vue';
import StatusPill from '@/components/StatusPill.vue';

const props = withDefaults(defineProps<{
  task: DemoTask;
  healingState?: 'normal' | 'failed' | 'healing';
  policyState?: 'conflict' | 'approved' | 'repaired';
}>(), {
  healingState: 'normal',
  policyState: 'conflict'
});
const selectedDeviceId = ref('core-switch');
const analyzing = ref(false);
let timer: number | undefined;

const selectedConfig = computed(() => props.task.deviceConfigs.find((config) => config.deviceId === selectedDeviceId.value) ?? props.task.deviceConfigs[0]);

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
