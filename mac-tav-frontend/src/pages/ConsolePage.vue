<template>
  <div class="console page-frame">
    <section class="intent-stage">
      <div class="stage-copy">
        <div class="eyebrow">意图捕获舞台</div>
        <h1 class="page-title">把自然语言网络意图推入闭环验证</h1>
        <p>
          支持隔离、连通性、ACL、路由、验证与修复诉求。当前为 mock 演示模式，创建任务后直接进入 Mission Control。
        </p>
        <div class="agent-ready">
          <StatusPill tone="signal">IntentAgent Ready</StatusPill>
          <StatusPill tone="pending">Current Task: optional</StatusPill>
        </div>
      </div>

      <GlassPanel class="capture-panel">
        <label for="intent">请输入自然语言网络意图</label>
        <a-textarea
          id="intent"
          v-model:value="store.draftIntent"
          class="intent-input"
          :auto-size="{ minRows: 8, maxRows: 12 }"
          placeholder="例如：办公区与访客区隔离，访客不能访问服务器区，验证失败时给出修复建议。"
        />
        <div class="launch-row">
          <div class="launch-beam" />
          <GlowButton label="启动意图翻译与闭环验证" :disabled="!store.draftIntent.trim()" @click="launch" />
        </div>
      </GlassPanel>
    </section>

    <section class="templates">
      <div class="templates-head">
        <div>
          <div class="eyebrow">场景模板快选</div>
          <h2>比赛展示常用网络意图</h2>
        </div>
      </div>
      <div class="template-grid">
        <SceneTemplateCard v-for="template in sceneTemplates" :key="template.title" v-bind="template" @select="store.draftIntent = $event" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import GlassPanel from '@/components/GlassPanel.vue';
import GlowButton from '@/components/GlowButton.vue';
import SceneTemplateCard from '@/components/SceneTemplateCard.vue';
import StatusPill from '@/components/StatusPill.vue';
import { sceneTemplates } from '@/fixtures/demoTask';
import { useTaskStore } from '@/stores/taskStore';

const router = useRouter();
const store = useTaskStore();

if (!store.draftIntent) {
  store.draftIntent = sceneTemplates[0].prompt;
}

async function launch(): Promise<void> {
  const task = store.createTask(store.draftIntent.trim());
  await router.push(`/tasks/${task.task.taskId}`);
}
</script>

<style scoped>
.console {
  display: grid;
  gap: 20px;
}

.intent-stage {
  display: grid;
  grid-template-columns: minmax(320px, 0.85fr) minmax(420px, 1.15fr);
  min-height: calc(100vh - 260px);
  align-items: center;
  gap: 26px;
}

.stage-copy {
  padding: 28px 0;
}

.stage-copy p {
  max-width: 650px;
  margin: 18px 0 0;
  color: var(--mactav-text-soft);
  font-size: 18px;
  line-height: 1.8;
}

.agent-ready {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 24px;
}

.capture-panel {
  min-height: 460px;
}

label {
  display: block;
  margin-bottom: 14px;
  color: var(--mactav-text-main);
  font-size: 18px;
  font-weight: 950;
}

.intent-input {
  border: 1px solid rgba(0, 98, 255, 0.2);
  border-radius: 22px;
  color: var(--mactav-text-main);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(240, 248, 255, 0.72)),
    radial-gradient(circle at 86% 18%, rgba(0, 217, 192, 0.12), transparent 28%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.76);
  font-size: 16px;
  line-height: 1.75;
}

.launch-row {
  display: grid;
  justify-items: center;
  gap: 10px;
  margin-top: 22px;
}

.launch-beam {
  width: 2px;
  height: 38px;
  border-radius: 999px;
  background: linear-gradient(var(--mactav-neon-teal), var(--mactav-cyber-blue));
  box-shadow: 0 0 18px rgba(0, 98, 255, 0.36);
}

.templates {
  display: grid;
  gap: 14px;
}

.templates-head {
  display: flex;
  justify-content: space-between;
}

h2 {
  margin: 6px 0 0;
  color: var(--mactav-text-main);
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

@media (max-width: 1080px) {
  .intent-stage,
  .template-grid {
    grid-template-columns: 1fr 1fr;
  }

  .intent-stage {
    align-items: stretch;
  }

  .stage-copy,
  .capture-panel {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .template-grid {
    grid-template-columns: 1fr;
  }
}
</style>
