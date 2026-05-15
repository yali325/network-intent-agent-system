<script setup lang="ts">
defineProps<{
  value: unknown;
  title?: string;
}>();
</script>

<template>
  <section class="json-viewer">
    <div v-if="title" class="section-title">{{ title }}</div>
    <pre>{{ JSON.stringify(value, null, 2) }}</pre>
  </section>
</template>
