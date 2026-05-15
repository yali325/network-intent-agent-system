import { createRouter, createWebHistory } from 'vue-router';
import TaskCreateView from '@/views/TaskCreateView.vue';
import TaskDetailView from '@/views/TaskDetailView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'task-create',
      component: TaskCreateView
    },
    {
      path: '/tasks/:taskId',
      name: 'task-detail',
      component: TaskDetailView,
      props: true
    }
  ]
});

export default router;
