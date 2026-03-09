<template>
  <div class="d-flex justify-space-between align-center flex-wrap ga-3 mt-4">
    <div class="text-body-2 text-medium-emphasis ml-4">
      {{ totalLabel }}
    </div>
    <v-pagination
      :model-value="modelValue + 1"
      :length="Math.max(totalPages, 1)"
      density="comfortable"
      @update:model-value="$emit('update:modelValue', $event - 1)"
    />
  </div>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  modelValue: {
    type: Number,
    default: 0
  },
  totalPages: {
    type: Number,
    default: 0
  },
  totalElements: {
    type: Number,
    default: 0
  }
});

defineEmits(["update:modelValue"]);

const totalLabel = computed(() => `${props.totalElements} records`);
</script>
