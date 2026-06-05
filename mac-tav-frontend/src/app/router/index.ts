import { createRouter, createWebHistory } from 'vue-router';
import ConsolePage from '@/pages/ConsolePage.vue';
import PlaceholderPage from '@/pages/PlaceholderPage.vue';
import TaskDetailPage from '@/pages/TaskDetailPage.vue';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/console' },
    { path: '/console', component: ConsolePage },
    { path: '/tasks/:taskId', component: TaskDetailPage, props: true },
    { path: '/tasks/:taskId/:section', component: PlaceholderPage, props: true },
    { path: '/:pathMatch(.*)*', redirect: '/console' }
  ]
});
