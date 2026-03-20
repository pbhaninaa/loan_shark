<template>
  <v-text-field
    :model-value="modelValue"
    :label="label"
    :prepend-inner-icon="prependInnerIcon"
    v-bind="$attrs"
    :rules="rules"           
    :error="error"           
    :error-messages="errorMessages" 
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
  },
  rules: {
    type: Array,
    default: () => []
  },
  error: {
    type: Boolean,
    default: false
  },
  errorMessages: {
    type: [String, Array],
    default: () => []
  }
});

defineEmits(["update:modelValue"]);
</script>