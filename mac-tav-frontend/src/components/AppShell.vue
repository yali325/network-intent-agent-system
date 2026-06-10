<template>
  <main class="shell">
    <header class="topbar page-frame">
      <router-link to="/console" class="brand">
        <span class="brand-mark">M</span>
        <span>
          <strong>MAC-TAV</strong>
          <small>网络意图闭环验证</small>
        </span>
      </router-link>
      <div class="top-actions">
        <a-popconfirm
          placement="bottomRight"
          ok-text="切换"
          cancel-text="取消"
          @confirm="toggleApiMode"
        >
          <template #title>
            <p style="max-width: 280px; margin: 0;">
              切换到 <strong>{{ nextModeLabel }}</strong> 模式。当前 mock 任务数据不会自动迁移。
            </p>
          </template>
          <StatusPill :tone="apiStore.isReal ? 'signal' : 'pending'" class="mode-pill">
            API Mode: {{ apiStore.mode }}
            <span v-if="apiStore.isReal && apiStore.baseUrl" class="mode-url"> · {{ displayBaseUrl }}</span>
          </StatusPill>
        </a-popconfirm>
        <StatusPill tone="pending">Phase 10 Visual Baseline</StatusPill>
      </div>
    </header>
    <router-view v-slot="{ Component }">
      <Transition name="route-fade" mode="out-in">
        <component :is="Component" />
      </Transition>
    </router-view>
  </main>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useApiModeStore } from "@/stores/apiModeStore";
import StatusPill from "@/components/StatusPill.vue";

const apiStore = useApiModeStore();

const nextModeLabel = computed(() => (apiStore.isMock ? "real" : "mock"));
const displayBaseUrl = computed(() => {
  const url = apiStore.baseUrl;
  if (!url) return "";
  return url.replace(/^https?:\/\//, "");
});

function toggleApiMode(): void {
  apiStore.setMode(apiStore.isMock ? "real" : "mock");
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  padding: 18px 0 32px;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--mactav-text-main);
  text-decoration: none;
}

.brand-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid rgba(0, 98, 255, 0.28);
  border-radius: 14px;
  color: white;
  background: linear-gradient(135deg, var(--mactav-cyber-blue), var(--mactav-neon-teal));
  box-shadow: 0 14px 30px rgba(0, 98, 255, 0.2);
  font-weight: 950;
}

.brand strong,
.brand small {
  display: block;
}

.brand small {
  color: var(--mactav-text-muted);
  font-size: 12px;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.mode-pill {
  cursor: pointer;
  user-select: none;
}

.mode-url {
  font-variant-numeric: tabular-nums;
}

.route-fade-enter-active,
.route-fade-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
}

.route-fade-enter-from,
.route-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 720px) {
  .topbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
