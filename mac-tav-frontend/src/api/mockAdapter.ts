import { createDemoTask, defaultDemoTask } from '@/fixtures/demoTask';
import type { DemoTask } from './types';

const tasks = new Map<string, DemoTask>([[defaultDemoTask.task.taskId, defaultDemoTask]]);

export function createMockTask(rawText: string): DemoTask {
  const task = createDemoTask(rawText);
  tasks.set(task.task.taskId, task);
  return task;
}

export function getMockTask(taskId: string): DemoTask {
  const existing = tasks.get(taskId);
  if (existing) return existing;
  const cloned = JSON.parse(JSON.stringify(defaultDemoTask)) as DemoTask;
  cloned.task.taskId = taskId;
  cloned.latestJob.taskId = taskId;
  tasks.set(taskId, cloned);
  return cloned;
}
