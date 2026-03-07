<template>
  <div class="page-shell">
    <AppPageHeader
      title="Help & Contact"
      description="Get in touch with the lender for queries, enquiries, or support with your loan."
    />

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <v-card class="mb-4">
      <v-card-title class="d-flex align-center text-primary">
        <v-icon start>mdi-help-circle-outline</v-icon>
        Lender contact details
      </v-card-title>
      <v-divider />
      <v-card-text>
        <p class="text-body-2 text-medium-emphasis mb-4">
          Use the details below to contact us about your loan, repayments, or any questions. We're here to help.
        </p>
        <v-list density="comfortable" class="bg-transparent">
          <v-list-item v-if="contact.name" class="px-0">
            <template #prepend>
              <v-icon color="primary">mdi-domain</v-icon>
            </template>
            <v-list-item-title>{{ contact.name }}</v-list-item-title>
            <v-list-item-subtitle>Business name</v-list-item-subtitle>
          </v-list-item>
          <v-list-item v-if="contact.phone" class="px-0">
            <template #prepend>
              <v-icon color="primary">mdi-phone-outline</v-icon>
            </template>
            <v-list-item-title>
              <a :href="`tel:${contact.phone}`" class="text-primary text-decoration-none">{{ contact.phone }}</a>
            </v-list-item-title>
            <v-list-item-subtitle>Phone</v-list-item-subtitle>
          </v-list-item>
          <v-list-item v-if="contact.email" class="px-0">
            <template #prepend>
              <v-icon color="primary">mdi-email-outline</v-icon>
            </template>
            <v-list-item-title>
              <a :href="`mailto:${contact.email}`" class="text-primary text-decoration-none">{{ contact.email }}</a>
            </v-list-item-title>
            <v-list-item-subtitle>Email for enquiries</v-list-item-subtitle>
          </v-list-item>
          <v-list-item v-if="contact.address" class="px-0">
            <template #prepend>
              <v-icon color="primary">mdi-map-marker-outline</v-icon>
            </template>
            <v-list-item-title>{{ contact.address }}</v-list-item-title>
            <v-list-item-subtitle>Address</v-list-item-subtitle>
          </v-list-item>
        </v-list>
        <p v-if="!hasAnyContact" class="text-medium-emphasis mb-0">
          Contact details are being set up. Please try again later or ask staff for assistance.
        </p>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import { useAppStore } from "../store";

const store = useAppStore();
const error = ref("");
const contact = ref({
  name: "",
  phone: "",
  email: "",
  address: ""
});

const hasAnyContact = computed(
  () =>
    (contact.value.name && contact.value.name.trim()) ||
    (contact.value.phone && contact.value.phone.trim()) ||
    (contact.value.email && contact.value.email.trim()) ||
    (contact.value.address && contact.value.address.trim())
);

onMounted(async () => {
  try {
    const data = await store.fetchLenderContact();
    contact.value = {
      name: data.name ?? "Philasande Bhani",
      phone: data.phone ?? "0782141216",
      email: data.email ?? "pbhanina@gmail.com",
      address: data.address ?? "Ewewe P O Box 47, \nQandashe Location\nBizana\n4800"
    };
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Could not load contact details.";
  }
});
</script>
