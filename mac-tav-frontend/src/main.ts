import 'ant-design-vue/dist/reset.css';
import './styles.css';

import Antd from 'ant-design-vue';
import { createPinia } from 'pinia';
import { createApp } from 'vue';
import App from './App.vue';
import { router } from './app/router';

createApp(App).use(createPinia()).use(router).use(Antd).mount('#app');
