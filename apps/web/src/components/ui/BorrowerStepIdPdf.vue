<template>
  <div class="borrower-step-id-pdf">
    <v-file-input
      v-model="borrowerForm.idDocument"
      accept="application/pdf"
      label="ID copy (PDF only)"
      prepend-icon="mdi-file-pdf-box"
      show-size
      hint="Upload a clear PDF copy of your South African ID."
      persistent-hint
    />
  </div>
</template>

<script setup>
import { inject } from "vue";

// Inject the shared borrowerForm from parent
const borrowerForm = inject("borrowerForm");
</script>

<style scoped>
.borrower-step-id-pdf {
  margin-top: 16px;
}
</style>