<template>
  <div class="page-shell">
    <AppPageHeader
      title="My Profile"
      description="Review your client profile, status, and affordability information."
    />

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <v-card>
      <v-card-title>Client Profile</v-card-title>
      <v-divider />
      <v-card-text v-if="profile">
        <div class="text-h6">{{ profile.firstName }} {{ profile.lastName }}</div>
        <div class="text-body-2 text-medium-emphasis mb-4">Client #{{ profile.id }}</div>
        <v-list density="comfortable">
          <v-list-item title="Phone" :subtitle="profile.phone" />
          <v-list-item title="Email" :subtitle="profile.email || '-'" />
          <v-list-item title="Address" :subtitle="profile.address" />
          <v-list-item title="Employment" :subtitle="profile.employmentStatus" />
          <v-list-item title="Monthly Income" :subtitle="formatCurrency(profile.monthlyIncome)" />
        </v-list>
        <div class="d-flex ga-2 flex-wrap mt-4">
          <v-chip :color="profile.status === 'BLACKLISTED' ? 'error' : 'success'" variant="tonal">
            {{ profile.status }}
          </v-chip>
          <v-chip color="info" variant="tonal">Risk Score: {{ profile.riskScore ?? "N/A" }}</v-chip>
        </div>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const store = useAppStore();
const error = ref("");
const profile = computed(() => store.borrowerProfile);

onMounted(async () => {
  try {
    await store.fetchMyBorrower();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load client profile";
  }
});
</script>
