<template>
  <v-select
    :model-value="modelValue"
    :label="label"
    :prepend-inner-icon="prependInnerIcon"
    :items="items"
    :item-title="itemTitle"
    :item-value="itemValue"
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
    type: [String, Number, Object, Array, null],
    default: null
  },
  label: {
    type: String,
    default: ""
  },
  prependInnerIcon: {
    type: String,
    default: undefined
  },
  items: {
    type: Array,
    default: () => []
  },
  itemTitle: {
    type: String,
    default: undefined
  },
  itemValue: {
    type: String,
    default: undefined
  }
});

defineEmits(["update:modelValue"]);
</script>
