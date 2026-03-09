<template>
  <v-text-field
    :model-value="modelValue"
    :label="label"
    prepend-inner-icon="mdi-magnify"
    clearable
    hide-details
    density="comfortable"
    class="mr-5"
    v-bind="$attrs"
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script setup>
defineProps({
  modelValue: {
    type: String,
    default: ""
  },
  label: {
    type: String,
    default: "Search"
  }
});

defineEmits(["update:modelValue"]);
</script>
