<template>
  <v-card>
    <v-card-title class="d-flex justify-space-between align-center">
      <span>{{ title }}</span>
      <div class="d-flex ga-2 align-center">
        <slot name="header-actions" />
        <v-chip v-if="countLabel" :color="chipColor" variant="tonal">{{ countLabel }}</v-chip>
      </div>
    </v-card-title>
    <v-divider />
    <v-card-text>
      <slot />
    </v-card-text>
  </v-card>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    required: true
  },
  countLabel: {
    type: String,
    default: ""
  },
  chipColor: {
    type: String,
    default: "primary"
  }
});
</script>
