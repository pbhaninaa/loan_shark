<template>
  <v-tooltip v-if="displayText !== fullText" location="top" max-width="400">
    <template #activator="{ props: activatorProps }">
      <span v-bind="activatorProps" class="text-truncate d-inline-block" :style="truncateStyle">
        {{ displayText }}{{ suffix }}
      </span>
    </template>
    <span class="text-wrap">{{ fullText }}</span>
  </v-tooltip>
  <span v-else>{{ displayText || fallback }}</span>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  /** Full text to show; truncated to maxChars when longer. */
  text: { type: [String, Number], default: "" },
  /** Max characters before truncation (default 90). */
  maxChars: { type: Number, default: 90 },
  /** Shown when text is empty. */
  fallback: { type: String, default: "-" },
  /** Optional max-width for the cell (e.g. "280px") so truncation is visible. */
  maxWidth: { type: String, default: null }
});

const fullText = computed(() => (props.text != null ? String(props.text) : ""));

const displayText = computed(() => {
  const s = fullText.value;
  if (s.length <= props.maxChars) return s;
  return s.slice(0, props.maxChars);
});

const suffix = computed(() => (fullText.value.length > props.maxChars ? "..." : ""));

const truncateStyle = computed(() => (props.maxWidth ? { maxWidth: props.maxWidth } : {}));
</script>
