<template>
  <section class="pipeline">
    <div class="eyebrow">RepairAction Pipeline</div>
    <div class="cards">
      <RepairActionCard
        v-for="action in actions"
        :key="action.actionId"
        :action="action"
        :selected="action.actionId === selectedActionId"
        @select="$emit('select', action.actionId)"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import type { RepairActionDemo } from '@/api/futureContracts';
import RepairActionCard from '@/components/RepairActionCard.vue';

defineProps<{ actions: RepairActionDemo[]; selectedActionId: string }>();
defineEmits<{ select: [actionId: string] }>();
</script>

<style scoped>
.pipeline {
  display: grid;
  gap: 12px;
}

.cards {
  display: grid;
  gap: 10px;
}
</style>
