<template>
  <v-text-field
    :model-value="modelValue"
    :label="label"
    :prepend-inner-icon="prependInnerIcon"
    v-bind="$attrs"
    @update:model-value="$emit('update:modelValue', $event)"
  />
</template>

<script setup>
defineOptions({
  inheritAttrs: false
});

defineProps({
  modelValue: {
    type: [String, Number, null],
    default: ""
  },
  label: {
    type: String,
    default: ""
  },
  prependInnerIcon: {
    type: String,
    default: undefined
  }
});

defineEmits(["update:modelValue"]);
</script>
