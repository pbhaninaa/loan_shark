<template>
  <v-dialog :model-value="modelValue" :max-width="maxWidth" @update:model-value="$emit('update:modelValue', $event)">
    <v-card>
      <v-card-title>{{ title }}</v-card-title>
      <v-divider />
      <v-card-text>
        <slot />
      </v-card-text>
    </v-card>
  </v-dialog>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    required: true
  },
  maxWidth: {
    type: [String, Number],
    default: 560
  }
});

defineEmits(["update:modelValue"]);
</script>
