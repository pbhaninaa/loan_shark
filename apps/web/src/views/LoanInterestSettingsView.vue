<template>
  <div class="page-shell">
    <AppPageHeader
      title="Loan interest & term settings"
      description="Configure default interest rate, type (simple/compound), interest period, grace period, and default loan term. These apply when clients apply for a loan with only the amount."
    >
      <template #actions>
        <v-chip color="primary" variant="tonal" size="large">Owner only</v-chip>
      </template>
    </AppPageHeader>

    <v-alert v-if="message" type="success" variant="tonal" class="mb-4">
      {{ message }}
    </v-alert>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon start>mdi-cog-outline</v-icon>
        Current settings
      </v-card-title>
      <v-divider />
      <v-card-text>
        <v-form v-if="settings" @submit.prevent="save">
          <v-row dense>
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.defaultInterestRate"
                label="Default interest rate (%)"
                type="number"
                step="0.01"
                min="0.01"
                prepend-inner-icon="mdi-percent-outline"
                hint="Used for new loans when not overridden."
              />
            </v-col>
            <v-col cols="12" md="6">
              <AppSelectField
                v-model="form.interestType"
                label="Interest type"
                :items="['SIMPLE', 'COMPOUND']"
                hint="Simple: interest on principal only. Compound: interest on principal + accrued interest each period."
              />
            </v-col>
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.interestPeriodDays"
                label="Interest period (days)"
                type="number"
                min="1"
                prepend-inner-icon="mdi-calendar"
                hint="E.g. 30 = interest accrues every 30 days from disbursement."
              />
            </v-col>
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.gracePeriodDays"
                label="Grace period (days)"
                type="number"
                min="0"
                prepend-inner-icon="mdi-timer-sand"
                hint="Days after due date before extra interest applies (0 = no grace)."
              />
            </v-col>
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.defaultLoanTermDays"
                label="Default loan term (days)"
                type="number"
                min="1"
                prepend-inner-icon="mdi-calendar-range"
                hint="Used when client only specifies amount (nominal term for due date; actual payoff by repayments)."
              />
            </v-col>
          </v-row>
          <div class="mt-4 d-flex ga-2">
            <AppActionButton text="Save settings" type="submit" :loading="saving" />
          </div>
        </v-form>
        <v-progress-linear v-else-if="loading" indeterminate color="primary" class="my-4" />
        <v-alert v-else type="warning" variant="tonal">
          Could not load settings. Run database migrations and ensure loan_interest_settings has a row.
        </v-alert>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import { useAppStore } from "../store";

const store = useAppStore();
const message = ref("");
const error = ref("");
const loading = ref(true);
const saving = ref(false);
const settings = ref(null);

const form = reactive({
  defaultInterestRate: 30,
  interestType: "SIMPLE",
  interestPeriodDays: 30,
  gracePeriodDays: 0,
  defaultLoanTermDays: 365
});

function assignForm() {
  if (!settings.value) return;
  form.defaultInterestRate = Number(settings.value.defaultInterestRate) || 30;
  form.interestType = settings.value.interestType || "SIMPLE";
  form.interestPeriodDays = Number(settings.value.interestPeriodDays) || 30;
  form.gracePeriodDays = Number(settings.value.gracePeriodDays) ?? 0;
  form.defaultLoanTermDays = Number(settings.value.defaultLoanTermDays) ?? 365;
}

watch(settings, assignForm, { immediate: true });

onMounted(async () => {
  loading.value = true;
  error.value = "";
  try {
    settings.value = await store.fetchLoanInterestSettings();
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Failed to load settings";
  } finally {
    loading.value = false;
  }
});

async function save() {
  saving.value = true;
  message.value = "";
  error.value = "";
  try {
    const updated = await store.updateLoanInterestSettings({
      defaultInterestRate: form.defaultInterestRate,
      interestType: form.interestType,
      interestPeriodDays: form.interestPeriodDays,
      gracePeriodDays: form.gracePeriodDays,
      defaultLoanTermDays: form.defaultLoanTermDays
    });
    settings.value = updated;
    message.value = "Settings saved.";
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Failed to save settings";
  } finally {
    saving.value = false;
  }
}
</script>
