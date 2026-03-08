<template>
  <div class="page-shell">
    <AppPageHeader
      title="My account"
      description="Your login details and email. You must add your email before you can use the system; it is also used to send password reset links."
    />

    <v-alert v-if="me && !me.email?.trim()" type="warning" variant="tonal" class="mb-4" prominent>
      You must add and save your email below before you can access the rest of the system.
    </v-alert>

    <v-alert v-if="message" type="success" variant="tonal" class="mb-4">
      {{ message }}
    </v-alert>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon start>mdi-account-outline</v-icon>
        Account
      </v-card-title>
      <v-divider />
      <v-card-text v-if="me">
        <v-list density="comfortable">
          <v-list-item title="Username" :subtitle="me.username" />
          <v-list-item title="Role" :subtitle="me.role" />
        </v-list>
        <v-form @submit.prevent="saveEmail" class="mt-4">
          <AppTextField
            v-model="emailInput"
            label="Email"
            type="email"
            prepend-inner-icon="mdi-email-outline"
            hint="Required for password reset. Add or update your email to receive reset links."
          />
          <div class="mt-3">
            <AppActionButton text="Save email" type="submit" :loading="saving" />
          </div>
        </v-form>
      </v-card-text>
      <v-progress-linear v-else-if="loading" indeterminate color="primary" class="my-4" />
      <v-alert v-else type="warning" variant="tonal">
        Could not load account details.
      </v-alert>
    </v-card>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import { useAppStore } from "../store";

const store = useAppStore();
const message = ref("");
const error = ref("");
const loading = ref(true);
const saving = ref(false);
const emailInput = ref("");

const me = ref(null);

watch(
  () => store.authMe,
  (v) => {
    me.value = v;
    if (v) emailInput.value = v.email || "";
  },
  { immediate: true }
);

onMounted(async () => {
  loading.value = true;
  error.value = "";
  message.value = "";
  try {
    const data = await store.fetchMe();
    me.value = data;
    emailInput.value = data?.email || "";
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Failed to load account";
  } finally {
    loading.value = false;
  }
});

async function saveEmail() {
  saving.value = true;
  message.value = "";
  error.value = "";
  try {
    await store.updateMyEmail(emailInput.value?.trim() || "");
    message.value = "Email saved. You can use it to receive password reset links.";
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Failed to save email";
  } finally {
    saving.value = false;
  }
}
</script>
