<template>
  <div class="page-shell">
    <AppPageHeader
      title="Verification Status"
      description="Your profile was created, but your account is restricted until owner review is completed."
    />

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <v-card>
      <v-card-title>KYC Review</v-card-title>
      <v-divider />
      <v-card-text v-if="verification">
        <div class="d-flex ga-2 flex-wrap mb-4">
          <v-chip :color="statusColor(verification.status)" variant="tonal">
            {{ verification.status }}
          </v-chip>
          <v-chip :color="verification.saIdValid ? 'success' : 'error'" variant="tonal">
            SA ID {{ verification.saIdValid ? "Valid" : "Invalid" }}
          </v-chip>
          <v-chip :color="verification.faceMatched ? 'success' : 'warning'" variant="tonal">
            Face Match {{ verification.faceMatched ? "Passed" : "Needs Review" }}
          </v-chip>
        </div>

        <v-list density="comfortable">
          <v-list-item title="Client status" :subtitle="borrowerStatus || 'None'" />
          <v-list-item title="OCR confidence" :subtitle="verification.ocrConfidence || 'None'" />
          <v-list-item title="Face match score" :subtitle="verification.faceMatchScore || 'None'" />
          <v-list-item title="Review notes" :subtitle="verification.reviewNotes || 'Your verification is still being processed.'" />
          <v-list-item title="Location captured" :subtitle="locationLabel" />
          <v-list-item title="Last update" :subtitle="formatDateTime(verification.updatedAt)" />
        </v-list>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import { useAppStore } from "../store";
import { formatDateTime } from "../utils/formatters";

const store = useAppStore();
const error = ref("");
const verification = computed(() => store.verification);
const borrowerStatus = computed(() => store.borrowerStatus);
const locationLabel = computed(() => {
  if (!verification.value?.latitude || !verification.value?.longitude) {
    return "None";
  }
  if (verification.value.locationName) {
    return `${verification.value.locationName} (${verification.value.latitude}, ${verification.value.longitude})`;
  }
  return `${verification.value.latitude}, ${verification.value.longitude}`;
});

onMounted(async () => {
  error.value = "";
  try {
    await Promise.all([store.fetchMyBorrower(), store.fetchMyVerification()]);
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load verification status";
  }
});

function statusColor(status) {
  if (status === "APPROVED") {
    return "success";
  }
  if (status === "REJECTED") {
    return "error";
  }
  return "warning";
}
</script>
