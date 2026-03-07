<template>
  <v-btn
    :color="color"
    :variant="variant"
    :prepend-icon="prependIcon"
    :type="type"
    :size="size"
    :loading="loading"
    :block="block"
    v-bind="$attrs"
  >
    <slot>{{ text }}</slot>
  </v-btn>
</template>

<script setup>
defineOptions({
  inheritAttrs: false
});

defineProps({
  text: {
    type: String,
    default: ""
  },
  color: {
    type: String,
    default: "primary"
  },
  variant: {
    type: String,
    default: "flat"
  },
  prependIcon: {
    type: String,
    default: undefined
  },
  type: {
    type: String,
    default: undefined
  },
  size: {
    type: String,
    default: undefined
  },
  loading: {
    type: Boolean,
    default: false
  },
  block: {
    type: Boolean,
    default: false
  }
});
</script>
