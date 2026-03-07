<template>
  <v-row justify="center" align="center" class="fill-height" style="min-height: calc(100vh - 96px);">
    <v-col cols="12" sm="12" md="8" lg="6" class="d-flex justify-center">
      <v-card class="pa-6 pa-md-10 login-card" style="max-width: 440px; width: 100%;">
        <div class="mb-6">
          <div class="text-overline text-primary">Password reset</div>
          <div class="text-h4 font-weight-bold">Set a new password</div>
          <div class="text-body-1 text-medium-emphasis mt-2">
            Enter your new password below. This link is valid for 24 hours.
          </div>
        </div>

        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
          {{ error }}
        </v-alert>
        <v-alert v-if="success" type="success" variant="tonal" class="mb-4">
          Password updated. You can now sign in.
        </v-alert>

        <v-form v-if="!success && token" @submit.prevent="submit">
          <AppTextField
            v-model="newPassword"
            type="password"
            label="New password"
            prepend-inner-icon="mdi-lock-outline"
            :rules="[() => !newPassword || newPassword.length >= 4 || 'At least 4 characters']"
            required
          />
          <AppTextField
            v-model="confirmPassword"
            type="password"
            label="Confirm new password"
            prepend-inner-icon="mdi-lock-check-outline"
            :rules="[() => confirmPassword === newPassword || 'Passwords must match']"
            required
          />
          <div class="d-flex ga-2 mt-4">
            <AppActionButton type="submit" color="primary" :loading="loading" text="Update password" />
            <v-btn variant="tonal" :to="{ name: 'login' }">Back to sign in</v-btn>
          </div>
        </v-form>

        <v-alert v-else-if="!token" type="warning" variant="tonal">
          No reset token provided. Request a new link from the
          <router-link to="/login">login page</router-link>.
        </v-alert>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppTextField from "../components/ui/AppTextField.vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import { useAppStore } from "../store";

const route = useRoute();
const router = useRouter();
const store = useAppStore();

const token = ref("");
const newPassword = ref("");
const confirmPassword = ref("");
const loading = ref(false);
const error = ref("");
const success = ref(false);

onMounted(() => {
  token.value = (route.query.token || "").trim();
});

async function submit() {
  error.value = "";
  if (!newPassword.value || newPassword.value.length < 4) {
    error.value = "Password must be at least 4 characters.";
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    error.value = "Passwords do not match.";
    return;
  }
  loading.value = true;
  try {
    await store.resetPasswordWithToken(token.value, newPassword.value);
    success.value = true;
    setTimeout(() => {
      router.push({ name: "login" });
    }, 2000);
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Reset failed. The link may have expired.";
  } finally {
    loading.value = false;
  }
}
</script>
