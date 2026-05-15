<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { PlayCircleOutlined } from '@ant-design/icons-vue';
import { useTaskStore } from '@/stores/taskStore';

const defaultRawText =
  '构建一个办公区和访客区隔离的网络，两个区域都能访问互联网，办公区可以访问服务器，访客区不能访问服务器，采用 OSPF。';

const router = useRouter();
const taskStore = useTaskStore();
const rawText = ref(defaultRawText);

const getErrorMessage = (error: unknown) => (error instanceof Error ? error.message : '运行 Demo 失败');

const submit = async () => {
  if (!rawText.value.trim()) {
    message.warning('请输入网络需求');
    return;
  }

  try {
    const workspace = await taskStore.run(rawText.value.trim());
    const taskId = workspace.task?.taskId;
    if (!taskId) {
      throw new Error('后端未返回 taskId');
    }
    message.success('Demo 任务已完成');
    await router.push(`/tasks/${taskId}`);
  } catch (error) {
    message.error(getErrorMessage(error));
  }
};
</script>

<template>
  <main class="create-page">
    <section class="create-panel">
      <div class="page-heading">
        <p class="eyebrow">Demo Task</p>
        <h1>提交网络意图</h1>
      </div>

      <a-form layout="vertical" @submit.prevent="submit">
        <a-form-item label="自然语言网络需求">
          <a-textarea
            v-model:value="rawText"
            :auto-size="{ minRows: 7, maxRows: 12 }"
            placeholder="输入网络意图..."
          />
        </a-form-item>
        <a-space>
          <a-button type="primary" size="large" :loading="taskStore.loading" @click="submit">
            <template #icon><PlayCircleOutlined /></template>
            运行 Demo
          </a-button>
        </a-space>
      </a-form>
    </section>
  </main>
</template>
