<template>
  <div class="topology-canvas">
    <svg viewBox="0 0 800 760" role="img" aria-label="Enterprise network topology" @click="keepSelection">
      <defs>
        <filter id="blur-glow" x="-60%" y="-60%" width="220%" height="220%">
          <feGaussianBlur stdDeviation="5" result="coloredBlur" />
          <feMerge>
            <feMergeNode in="coloredBlur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <g class="link-layer">
        <path v-for="link in resolvedLinks" :key="`${link.from.id}-${link.to.id}-base`" :d="linkPath(link)" class="link-base" />
        <g v-for="link in permitLinks" :key="`${link.from.id}-${link.to.id}-permit`">
          <path :d="linkPath(link)" class="permit-glow" filter="url(#blur-glow)" />
          <path :d="linkPath(link)" class="permit-flow">
            <animate attributeName="stroke-dashoffset" from="108" to="0" dur="1.05s" repeatCount="indefinite" />
          </path>
        </g>
        <g v-for="link in activeLinks" :key="`${link.from.id}-${link.to.id}-active`">
          <path :d="linkPath(link)" class="link-glow" filter="url(#blur-glow)" />
          <path :d="linkPath(link)" class="link-flow">
            <animate attributeName="stroke-dashoffset" from="108" to="0" dur="1.15s" repeatCount="indefinite" />
          </path>
        </g>
        <path :d="aclDenyPath" :class="['acl-deny-arc', { healing: isHealingAlert }]" />
      </g>

      <g class="policy-layer">
        <g :class="['acl-badge', { healing: isHealingAlert }]" transform="translate(404 584)">
          <rect x="-42" y="-12" width="84" height="24" rx="12" />
          <text text-anchor="middle" y="4">🚫 ACL Deny</text>
        </g>
      </g>

      <g class="interface-badge-layer">
        <text v-for="badge in interfaceBadges" :key="badge.id" :x="badge.x" :y="badge.y" class="interface-badge">
          {{ badge.label }}
        </text>
      </g>

      <g class="node-layer">
        <g
          v-for="node in layoutNodes"
          :key="node.id"
          :class="['network-node', node.kind, { selected: node.id === selectedDeviceId }]"
          :transform="`translate(${node.x} ${node.y})`"
          role="button"
          tabindex="0"
          @click.stop="selectDevice(node.id)"
          @keydown.enter.stop.prevent="selectDevice(node.id)"
        >
          <circle v-if="rippleDeviceId === node.id" class="click-ripple" r="34" />

          <template v-if="node.kind === 'router'">
            <circle class="router-shell" r="25" />
            <circle class="router-orbit" r="16.5" />
            <path class="router-glyph" d="M-12 -3 C-8 -11, 5 -12, 12 -5" />
            <path class="router-glyph" d="M7 -10 L13 -5 L6 -2" />
            <path class="router-glyph" d="M12 3 C8 11, -5 12, -12 5" />
            <path class="router-glyph" d="M-7 10 L-13 5 L-6 2" />
            <circle class="selected-router-ring" r="33" />
          </template>

          <template v-else-if="node.kind === 'switch' && node.id === 'core-switch'">
            <rect class="switch-shell" x="-28" y="-24" width="56" height="48" rx="11" />
            <circle class="core-negative-space" r="3" />
            <path class="core-glyph" d="M-20 0 H-5 M5 0 H20" />
            <path class="core-glyph" d="M14 -6 L20 0 L14 6 M-14 6 L-20 0 L-14 -6" />
            <path class="core-glyph" d="M0 -20 V-5 M0 5 V20" />
            <path class="core-glyph" d="M-6 -14 L0 -20 L6 -14 M6 14 L0 20 L-6 14" />
            <path class="core-glyph diagonal" d="M-15 -15 L-5 -5 M5 5 L15 15" />
            <path class="core-glyph diagonal" d="M15 -15 L5 -5 M-5 5 L-15 15" />
            <rect class="selected-switch-ring" x="-37" y="-33" width="74" height="66" rx="17" />
          </template>

          <template v-else-if="node.kind === 'switch'">
            <rect class="agg-shell" x="-30" y="-20" width="60" height="40" rx="12" />
            <path class="agg-glyph" d="M-20 -7 H13" />
            <path class="agg-glyph" d="M8 -13 L17 -7 L8 -1" />
            <path class="agg-glyph" d="M20 8 H-13" />
            <path class="agg-glyph" d="M-8 2 L-17 8 L-8 14" />
            <line class="agg-divider" x1="-20" y1="0" x2="20" y2="0" />
            <rect class="selected-switch-ring agg" x="-39" y="-29" width="78" height="58" rx="18" />
          </template>

          <template v-else>
            <rect
              class="service-tray"
              x="-70"
              y="-45"
              width="140"
              height="90"
              rx="12"
              fill="rgba(248, 250, 252, 0.7)"
              stroke="rgba(0, 98, 255, 0.2)"
              stroke-width="1.5"
            />
            <g v-if="node.id === 'prod'" class="rack-icon" transform="translate(0 -15)">
              <rect x="-24" y="-18" width="48" height="38" rx="6" />
              <line x1="-17" y1="-6" x2="17" y2="-6" />
              <line x1="-17" y1="6" x2="17" y2="6" />
              <circle cx="-13" cy="-12" r="2" />
              <circle cx="-13" cy="0" r="2" />
              <circle cx="-13" cy="12" r="2" />
            </g>
            <g v-else class="terminal-icon" transform="translate(0 -15)">
              <rect x="-25" y="-17" width="50" height="32" rx="7" />
              <path d="M-11 24 H11 M0 15 V24" />
              <circle cx="16" cy="-8" r="2" />
            </g>
            <text class="service-vlan" text-anchor="middle" y="25">{{ serviceMeta(node.id) }}</text>
            <rect class="selected-service-ring" x="-78" y="-53" width="156" height="106" rx="18" />
          </template>
        </g>
      </g>

      <g class="label-layer">
        <g v-for="node in layoutNodes" :key="`${node.id}-label`" class="node-label">
          <text :x="node.x" :y="node.y + 44" text-anchor="middle" class="device-name">{{ node.name }}</text>
          <text :x="node.x" :y="node.y + 60" text-anchor="middle" class="device-role">{{ node.role }}</text>
        </g>
      </g>
    </svg>
    <p class="hint">点击拓扑节点，右侧仅显示所选设备配置。空白处点击不会取消当前高亮。</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { TopologyDevice, TopologyLink } from '@/api/types';

const props = withDefaults(
  defineProps<{
    devices: TopologyDevice[];
    links: TopologyLink[];
    initialSelectedDeviceId?: string;
    healingState?: 'normal' | 'failed' | 'healing';
  }>(),
  {
    initialSelectedDeviceId: undefined,
    healingState: 'normal'
  }
);

const emit = defineEmits<{ selectDevice: [deviceId: string] }>();

interface LayoutNode extends TopologyDevice {
  x: number;
  y: number;
}

type ResolvedLink = TopologyLink & {
  from: LayoutNode;
  to: LayoutNode;
};

interface InterfaceBadge {
  id: string;
  label: string;
  x: number;
  y: number;
}

const industrialLayout: Record<string, { x: number; y: number }> = {
  internet: { x: 400, y: 80 },
  'core-router': { x: 400, y: 220 },
  'core-switch': { x: 400, y: 360 },
  'agg-a': { x: 220, y: 500 },
  'agg-b': { x: 580, y: 500 },
  prod: { x: 220, y: 660 },
  guest: { x: 580, y: 660 }
};

const layoutNodes = computed<LayoutNode[]>(() =>
  props.devices.map((device) => ({
    ...device,
    ...(industrialLayout[device.id] ?? { x: device.x * 8, y: device.y * 7.6 })
  }))
);

const selectedDeviceId = ref<string | null>(props.initialSelectedDeviceId ?? 'core-switch');
const rippleDeviceId = ref<string | null>(null);
let rippleTimer: number | undefined;

const resolvedLinks = computed<ResolvedLink[]>(() =>
  props.links
    .map((link) => ({
      ...link,
      from: layoutNodes.value.find((node) => node.id === link.from),
      to: layoutNodes.value.find((node) => node.id === link.to)
    }))
    .filter((link): link is ResolvedLink => Boolean(link.from && link.to))
);

const activeLinks = computed(() => resolvedLinks.value.filter((link) => isLinkedToSelected(link) && !isPermitLink(link)));
const permitLinks = computed(() =>
  resolvedLinks.value.filter((link) => isPermitLink(link))
);
const isHealingAlert = computed(() => props.healingState === 'failed' || props.healingState === 'healing');
const aclDenyPath = 'M 580 532 C 616 604, 384 592, 290 615';

const interfaceBadges: InterfaceBadge[] = [
  { id: 'internet-down', label: 'GE 0/0/0', x: 424, y: 120 },
  { id: 'core-router-up', label: 'GE 0/0/0', x: 424, y: 186 },
  { id: 'core-router-down', label: 'GE 0/0/1', x: 424, y: 254 },
  { id: 'core-switch-up', label: 'GE 1/0/0', x: 424, y: 326 },
  { id: 'core-switch-left', label: 'GE 1/0/1', x: 342, y: 398 },
  { id: 'agg-a-up', label: 'GE 1/0/24', x: 246, y: 468 },
  { id: 'core-switch-right', label: 'GE 1/0/2', x: 458, y: 398 },
  { id: 'agg-b-up', label: 'GE 1/0/24', x: 554, y: 468 },
  { id: 'agg-a-down', label: 'GE 1/0/3', x: 186, y: 548 },
  { id: 'prod-up', label: 'GE 0/0', x: 252, y: 608 },
  { id: 'agg-b-down', label: 'GE 1/0/3', x: 614, y: 548 },
  { id: 'guest-up', label: 'GE 0/0', x: 548, y: 608 }
];

function selectDevice(deviceId: string): void {
  selectedDeviceId.value = deviceId;
  rippleDeviceId.value = deviceId;
  window.clearTimeout(rippleTimer);
  rippleTimer = window.setTimeout(() => {
    rippleDeviceId.value = null;
  }, 500);
  emit('selectDevice', deviceId);
}

function keepSelection(): void {
  // Blank canvas clicks intentionally preserve selectedDeviceId.
}

function isLinkedToSelected(link: ResolvedLink): boolean {
  return Boolean(selectedDeviceId.value && (link.from.id === selectedDeviceId.value || link.to.id === selectedDeviceId.value));
}

function isPermitLink(link: ResolvedLink): boolean {
  return (link.from.id === 'core-switch' && link.to.id === 'agg-a') || (link.from.id === 'agg-a' && link.to.id === 'prod');
}

function linkPath(link: ResolvedLink): string {
  const { from, to } = link;
  const midY = (from.y + to.y) / 2;

  if (from.x === to.x) {
    return `M ${from.x} ${from.y + nodeExitOffset(from)} C ${from.x} ${midY - 28}, ${to.x} ${midY + 28}, ${to.x} ${to.y - nodeEntryOffset(to)}`;
  }

  if ((from.id === 'guest' && to.id === 'prod') || (from.id === 'prod' && to.id === 'guest')) {
    return `M ${from.x} ${from.y - 50} C ${from.x} ${from.y - 82}, ${to.x} ${to.y - 82}, ${to.x} ${to.y - 50}`;
  }

  const fromY = from.y + nodeExitOffset(from);
  const toY = to.y - nodeEntryOffset(to);
  const bendY = fromY + (toY - fromY) * 0.55;
  return `M ${from.x} ${fromY} C ${from.x} ${bendY}, ${to.x} ${bendY}, ${to.x} ${toY}`;
}

function nodeExitOffset(node: LayoutNode): number {
  if (node.kind === 'service') return 50;
  if (node.kind === 'switch') return 30;
  return 28;
}

function nodeEntryOffset(node: LayoutNode): number {
  if (node.kind === 'service') return 50;
  if (node.kind === 'switch') return 30;
  return 28;
}

function serviceMeta(deviceId: string): string {
  if (deviceId === 'prod') return 'VLAN 30 | 10.1.3.0/24';
  if (deviceId === 'guest') return 'VLAN 10 | 10.1.1.0/24';
  return '';
}
</script>

<style scoped>
.topology-canvas {
  min-height: 690px;
  overflow: hidden;
  border: 1px solid rgba(110, 155, 215, 0.28);
  border-radius: 24px;
  background:
    radial-gradient(circle at 50% 40%, rgba(0, 217, 192, 0.1), transparent 30%),
    radial-gradient(circle at 50% 24%, rgba(0, 98, 255, 0.08), transparent 34%),
    radial-gradient(circle, rgba(0, 98, 255, 0.13) 0.8px, transparent 1px),
    linear-gradient(145deg, rgba(255, 255, 255, 0.72), rgba(238, 248, 255, 0.62));
  background-size: auto, auto, 24px 24px, auto;
}

svg {
  display: block;
  width: 100%;
  height: 670px;
}

.link-base {
  fill: none;
  stroke: #e2e8f0;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.link-glow {
  fill: none;
  stroke: rgba(0, 98, 255, 0.4);
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 4;
}

.link-flow {
  fill: none;
  stroke: #f8fdff;
  stroke-dasharray: 8 100;
  stroke-dashoffset: 108;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.permit-glow {
  fill: none;
  stroke: rgba(16, 185, 129, 0.34);
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 4;
}

.permit-flow {
  fill: none;
  stroke: #22c55e;
  stroke-dasharray: 8 100;
  stroke-dashoffset: 108;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.acl-deny-arc {
  fill: none;
  stroke: rgba(239, 68, 68, 0.4);
  stroke-dasharray: 4 4;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.acl-deny-arc.healing {
  stroke: #f59e0b;
  animation: acl-alert 1.05s ease-in-out infinite;
}

.acl-badge rect {
  fill: rgba(254, 242, 242, 0.9);
  stroke: rgba(239, 68, 68, 0.24);
  stroke-width: 1;
  filter: drop-shadow(0 0 10px rgba(239, 68, 68, 0.16));
}

.acl-badge text {
  fill: #dc2626;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 10px;
  font-weight: 900;
}

.acl-badge.healing rect {
  fill: rgba(255, 251, 235, 0.94);
  stroke: rgba(245, 158, 11, 0.34);
}

.acl-badge.healing text {
  fill: #b45309;
}

.interface-badge {
  fill: #64748b;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 10px;
  font-weight: 750;
  paint-order: stroke;
  stroke: rgba(248, 252, 255, 0.96);
  stroke-linejoin: round;
  stroke-width: 3px;
}

.network-node {
  cursor: pointer;
  outline: none;
  transition: filter 180ms ease;
}

.network-node.selected {
  filter: drop-shadow(0 0 8px rgba(0, 245, 212, 0.4));
}

.router-shell {
  fill: #f8fbff;
  stroke: #0062ff;
  stroke-width: 2.2;
}

.switch-shell {
  fill: #f3fffc;
  stroke: #0ea5e9;
  stroke-width: 2.2;
}

.agg-shell {
  fill: #f8fcff;
  stroke: #38bdf8;
  stroke-width: 2;
}

.router-glyph,
.router-orbit,
.core-glyph,
.agg-glyph,
.agg-divider {
  fill: none;
  stroke: #475569;
  stroke-linecap: round;
  stroke-linejoin: round;
  transition: stroke 180ms ease, opacity 180ms ease;
}

.router-orbit {
  opacity: 0.26;
  stroke-width: 1.5;
}

.router-glyph {
  stroke-width: 1.8;
}

.core-glyph {
  stroke-width: 1.75;
}

.core-glyph.diagonal {
  opacity: 0.72;
  stroke-width: 1.45;
}

.core-negative-space {
  fill: #f3fffc;
  stroke: rgba(14, 165, 233, 0.2);
  stroke-width: 0.7;
}

.agg-glyph {
  stroke-width: 1.8;
}

.agg-divider {
  opacity: 0.22;
  stroke-width: 1.2;
}

.selected .router-orbit,
.selected .router-glyph,
.selected .core-glyph,
.selected .agg-glyph,
.selected .agg-divider {
  stroke: #00f5d4;
}

.selected .core-negative-space {
  fill: rgba(0, 245, 212, 0.1);
  stroke: rgba(0, 245, 212, 0.34);
}

.service-tray {
  filter: drop-shadow(0 14px 22px rgba(31, 91, 180, 0.08));
}

.rack-icon rect,
.terminal-icon rect {
  fill: rgba(255, 255, 255, 0.92);
  stroke: #0062ff;
  stroke-width: 2.5;
}

.rack-icon line,
.terminal-icon path {
  fill: none;
  stroke: #0062ff;
  stroke-linecap: round;
  stroke-width: 2.2;
}

.rack-icon circle,
.terminal-icon circle {
  fill: #00d9c0;
}

.service-vlan {
  fill: #94a3b8;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 10px;
  font-weight: 800;
  paint-order: stroke;
  stroke: rgba(248, 252, 255, 0.92);
  stroke-linejoin: round;
  stroke-width: 2.4px;
}

.selected-router-ring,
.selected-switch-ring,
.selected-service-ring {
  fill: transparent;
  opacity: 0;
  stroke: #00f5d4;
  stroke-width: 3;
}

.selected-switch-ring.agg {
  stroke-width: 2.6;
}

.selected .selected-router-ring,
.selected .selected-switch-ring,
.selected .selected-service-ring {
  opacity: 0.86;
  filter: url(#blur-glow);
  animation: selected-breathe 1.8s ease-in-out infinite;
}

.click-ripple {
  fill: rgba(0, 245, 212, 0.1);
  opacity: 0.9;
  stroke: rgba(0, 245, 212, 0.55);
  stroke-width: 3;
  transform-box: fill-box;
  transform-origin: center;
  animation: ripple-once 0.5s ease-out forwards;
}

.device-name {
  fill: #1e293b;
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 13px;
  font-weight: 950;
  paint-order: stroke;
  stroke: rgba(247, 251, 255, 0.96);
  stroke-linejoin: round;
  stroke-width: 4px;
}

.device-role {
  fill: #64748b;
  font-size: 11px;
  font-weight: 700;
  paint-order: stroke;
  stroke: rgba(247, 251, 255, 0.96);
  stroke-linejoin: round;
  stroke-width: 3px;
}

.hint {
  margin: -2px 18px 18px;
  color: var(--mactav-text-muted);
  font-size: 13px;
}

@keyframes selected-breathe {
  0%,
  100% {
    opacity: 0.5;
    transform: scale(1);
  }

  50% {
    opacity: 0.95;
    transform: scale(1.06);
  }
}

@keyframes ripple-once {
  from {
    opacity: 0.75;
    transform: scale(0.85);
  }

  to {
    opacity: 0;
    transform: scale(2.05);
  }
}

@keyframes acl-alert {
  0%,
  100% {
    opacity: 0.45;
  }

  50% {
    opacity: 1;
  }
}
</style>
