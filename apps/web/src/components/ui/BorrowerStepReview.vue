<template>
  <v-sheet variant="tonal" rounded="lg" class="pa-4 mb-4">
    <div class="text-caption text-medium-emphasis">Personal</div>
    <div class="mb-2">{{ borrowerForm.firstName }} {{ borrowerForm.lastName }}, {{ borrowerForm.idNumber }}</div>

    <div class="text-caption text-medium-emphasis">Contact</div>
    <div class="mb-2">{{ borrowerForm.phone }} {{ borrowerForm.email ? `· ${borrowerForm.email}` : "" }}</div>

    <div class="text-caption text-medium-emphasis">Address</div>
    <div class="mb-2">{{ borrowerForm.address }}</div>

    <div class="text-caption text-medium-emphasis">Location captured</div>
    <div class="mb-2">{{ borrowerForm.locationName || "None" }}</div>

    <div class="text-caption text-medium-emphasis">Documents</div>
    <div>ID PDF and selfie attached.</div>
  </v-sheet>
</template>

<script setup>
import { inject } from "vue";

const borrowerForm = inject("borrowerForm");
</script>

<style scoped>
</style>