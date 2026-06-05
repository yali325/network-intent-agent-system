import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { createMockTask, getMockTask } from '@/api/mockAdapter';
import type { DemoTask, WorkflowStage } from '@/api/types';

export const useTaskStore = defineStore('task', () => {
  const activeTask = ref<DemoTask | null>(null);
  const selectedStage = ref<WorkflowStage>('CONFIGURATION');
  const draftIntent = ref('');
  const appendedIntents = ref<Array<{ id: string; text: string; createTime: string }>>([]);

  const currentSummary = computed(() => activeTask.value?.stageSummaries.find((item) => item.stage === selectedStage.value) ?? null);

  function createTask(rawText: string): DemoTask {
    draftIntent.value = rawText;
    activeTask.value = createMockTask(rawText);
    appendedIntents.value = [];
    selectedStage.value = activeTask.value.task.currentStage;
    return activeTask.value;
  }

  function loadTask(taskId: string): DemoTask {
    activeTask.value = getMockTask(taskId);
    selectedStage.value = activeTask.value.task.currentStage;
    return activeTask.value;
  }

  function prepareNewIntent(): void {
    draftIntent.value = '';
  }

  async function appendIntent(text: string): Promise<void> {
    if (!activeTask.value) return;
    const createTime = new Date().toISOString();
    appendedIntents.value.unshift({
      id: `append-${Date.now().toString(36)}`,
      text,
      createTime
    });
    activeTask.value.task.rawText = `${activeTask.value.task.rawText}\n追加指令：${text}`;
    activeTask.value.telemetry.unshift({
      eventId: `evt-append-${Date.now().toString(36)}`,
      eventType: 'intent.appended',
      stage: 'INTENT',
      severity: 'INFO',
      title: '追加意图已进入多轮队列',
      message: text,
      eventTime: createTime
    });
    activeTask.value.latestJob.jobStatus = 'RUNNING';
    activeTask.value.task.currentStage = 'CONFIGURATION';
    selectedStage.value = 'CONFIGURATION';
  }

  return {
    activeTask,
    selectedStage,
    draftIntent,
    appendedIntents,
    currentSummary,
    createTask,
    loadTask,
    prepareNewIntent,
    appendIntent
  };
});
