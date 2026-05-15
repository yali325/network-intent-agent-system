import { defineStore } from 'pinia';
import { getWorkspace, runDemoTask } from '@/api/taskApi';
import type { NetworkWorkspace } from '@/types';

interface TaskState {
  currentWorkspace: NetworkWorkspace | null;
  loading: boolean;
}

export const useTaskStore = defineStore('task', {
  state: (): TaskState => ({
    currentWorkspace: null,
    loading: false
  }),
  actions: {
    async run(rawText: string) {
      this.loading = true;
      try {
        this.currentWorkspace = await runDemoTask(rawText);
        return this.currentWorkspace;
      } finally {
        this.loading = false;
      }
    },
    async fetch(taskId: string) {
      this.loading = true;
      try {
        this.currentWorkspace = await getWorkspace(taskId);
        return this.currentWorkspace;
      } finally {
        this.loading = false;
      }
    }
  }
});
