<script setup lang="ts">
import { computed } from 'vue';
import type { CommandBlock, ConfigSet, DeviceConfig } from '@/types';
import JsonViewer from './JsonViewer.vue';

const props = defineProps<{
  configSet?: ConfigSet;
}>();

const devices = computed(() => props.configSet?.deviceConfigs ?? []);

const blockTitle = (block: CommandBlock) =>
  `${block.order ?? '-'} · ${block.title || block.blockId} · ${block.blockType || '-'}`;

const deviceLabel = (device: DeviceConfig) =>
  `${device.deviceName || device.deviceId}${device.deviceType ? ` / ${device.deviceType}` : ''}`;
</script>

<template>
  <a-empty v-if="!configSet" description="配置产物尚未生成" />
  <section v-else class="panel-stack">
    <section class="panel">
      <div class="section-title">配置摘要</div>
      <a-typography-paragraph>{{ configSet.generationSummary || '-' }}</a-typography-paragraph>
      <a-alert
        v-for="warning in configSet.warnings || []"
        :key="warning.warningId || warning.message"
        class="inline-alert"
        :message="warning.message || '配置警告'"
        :type="warning.level === 'ERROR' ? 'error' : 'warning'"
        show-icon
      />
    </section>

    <section class="panel">
      <div class="section-title">设备配置</div>
      <a-tabs v-if="devices.length" type="card">
        <a-tab-pane v-for="device in devices" :key="device.deviceId" :tab="deviceLabel(device)">
          <pre class="command-pre">{{ device.configText || '暂无配置文本' }}</pre>

          <a-collapse v-if="device.commandBlocks?.length" class="block-collapse">
            <a-collapse-panel v-for="block in device.commandBlocks" :key="block.blockId" :header="blockTitle(block)">
              <a-descriptions size="small" bordered :column="{ xs: 1, md: 2 }">
                <a-descriptions-item label="Block ID">{{ block.blockId }}</a-descriptions-item>
                <a-descriptions-item label="Block Type">{{ block.blockType || '-' }}</a-descriptions-item>
                <a-descriptions-item label="Explanation" :span="2">
                  {{ block.explanation || '-' }}
                </a-descriptions-item>
                <a-descriptions-item label="Depends On" :span="2">
                  <a-space wrap>
                    <a-tag v-for="item in block.dependsOn || []" :key="item">{{ item }}</a-tag>
                    <span v-if="!block.dependsOn?.length">-</span>
                  </a-space>
                </a-descriptions-item>
                <a-descriptions-item label="Trace Refs" :span="2">
                  <JsonViewer :value="block.traceRefs || {}" />
                </a-descriptions-item>
              </a-descriptions>

              <div class="subsection-title">Commands</div>
              <pre class="command-pre">{{ (block.commands || []).join('\n') || '-' }}</pre>

              <div class="subsection-title">Rollback Commands</div>
              <pre class="command-pre rollback">{{ (block.rollbackCommands || []).join('\n') || '-' }}</pre>
            </a-collapse-panel>
          </a-collapse>
        </a-tab-pane>
      </a-tabs>
      <a-empty v-else description="暂无设备配置" />
    </section>

    <a-collapse ghost>
      <a-collapse-panel key="json" header="完整 JSON">
        <JsonViewer :value="configSet" />
      </a-collapse-panel>
    </a-collapse>
  </section>
</template>
