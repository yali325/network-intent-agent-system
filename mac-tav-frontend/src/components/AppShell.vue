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
        <StatusPill tone="signal">API Mode: {{ apiMode }}</StatusPill>
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
import { apiMode } from '@/api/config';
import StatusPill from '@/components/StatusPill.vue';
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
