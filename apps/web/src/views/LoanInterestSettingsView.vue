<template>
  <div class="page-shell">
    <AppPageHeader
      title="Loan interest & term settings"
      description="Configure default interest rate, type (simple/compound), interest period, grace period, and default loan term. The expected amount is calculated from what you put in (Business capital) and updates as you add more."
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

      <div
        v-if="expectedAmount != null"
        class="expected-amount pa-4 rounded"
      >
        <div class="text-caption text-medium-emphasis mb-1">
          Expected amount at end of term
        </div>

        <div class="text-h5 font-weight-bold text-primary">
          {{ formatCurrency(expectedAmount.expectedAmountDue) }}
        </div>

        <p class="text-caption text-medium-emphasis mt-2 mb-0">
          Based on <strong>what you put in</strong>
          ({{ formatCurrency(expectedAmount.principal) }}) over
          {{ expectedAmount.termDays }} days, using rate, type,
          period & grace above. This updates when you add more
          funds in Business capital.
        </p>
      </div>

      <v-card-text>
        <v-form v-if="settings" @submit.prevent="save">
          <v-row dense>

            <!-- Interest Rate -->
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

            <!-- Interest Type -->
            <v-col cols="12" md="6">
              <AppSelectField
                v-model="form.interestType"
                label="Interest type"
                :items="['SIMPLE', 'COMPOUND']"
                hint="Simple: interest on principal only. Compound: interest on principal + accrued interest each period."
              />
            </v-col>

            <!-- Interest Period -->
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.interestPeriodDays"
                label="Interest period (days)"
                type="number"
                min="1"
                prepend-inner-icon="mdi-calendar"
                hint="Example: 30 = interest accrues every 30 days."
              />
            </v-col>

            <!-- Grace Period -->
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.gracePeriodDays"
                label="Grace period (days)"
                type="number"
                min="0"
                prepend-inner-icon="mdi-timer-sand"
                hint="First N days after disbursement do not attract interest."
              />
            </v-col>

            <!-- Default Loan Term -->
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.defaultLoanTermDays"
                label="Default loan term (days)"
                type="number"
                min="1"
                prepend-inner-icon="mdi-calendar-range"
                hint="Nominal loan term used when client does not specify a term."
              />
            </v-col>

            <!-- Salary Based Borrower Limit -->
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.borrowerLimitPercentageSalaryBased"
                label="Borrower limit (% of salary)"
                type="number"
                step="0.01"
                min="0"
                max="100"
                prepend-inner-icon="mdi-percent"
                hint="Maximum loan allowed as a percentage of the client's monthly income."
              />
            </v-col>

            <!-- Previous Loan Repayment Limit -->
            <v-col cols="12" md="6">
              <AppTextField
                v-model.number="form.borrowerLimitPercentagePreviousLoan"
                label="Borrower limit (% of amount repaid)"
                type="number"
                step="0.01"
                min="0"
                max="100"
                prepend-inner-icon="mdi-percent"
                hint="Client can borrow a percentage of the amount already repaid on their active loan."
              />
            </v-col>

          </v-row>

          <div class="mt-4 d-flex ga-2">
            <AppActionButton
              text="Save settings"
              type="submit"
              :loading="saving"
            />
          </div>
        </v-form>

        <v-progress-linear
          v-else-if="loading"
          indeterminate
          color="primary"
          class="my-4"
        />

        <v-alert v-else type="warning" variant="tonal">
          Could not load settings. Run database migrations and ensure
          loan_interest_settings has a row.
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
import { formatCurrency } from "../utils/formatters";
import { useAppStore } from "../store";

const store = useAppStore();

const message = ref("");
const error = ref("");
const loading = ref(true);
const saving = ref(false);

const settings = ref(null);
const expectedAmount = ref(null);

const form = reactive({
  defaultInterestRate: 30,
  interestType: "SIMPLE",
  interestPeriodDays: 30,
  gracePeriodDays: 0,
  defaultLoanTermDays: 365,
  borrowerLimitPercentageSalaryBased: 100,
  borrowerLimitPercentagePreviousLoan: 100
});

function assignForm() {
  if (!settings.value) return;

  form.defaultInterestRate =
    Number(settings.value.defaultInterestRate) || 30;

  form.interestType =
    settings.value.interestType || "SIMPLE";

  form.interestPeriodDays =
    Number(settings.value.interestPeriodDays) || 30;

  form.gracePeriodDays =
    Number(settings.value.gracePeriodDays) ?? 0;

  form.defaultLoanTermDays =
    Number(settings.value.defaultLoanTermDays) ?? 365;

  form.borrowerLimitPercentageSalaryBased =
    Number(settings.value.borrowerLimitPercentageSalaryBased) ?? 100;

  form.borrowerLimitPercentagePreviousLoan =
    Number(settings.value.borrowerLimitPercentagePreviousLoan) ?? 100;
}

watch(settings, assignForm, { immediate: true });

async function loadExpectedAmount() {
  try {
    expectedAmount.value =
      await store.fetchExpectedAmountAtEndOfTerm();
  } catch {
    expectedAmount.value = null;
  }
}

onMounted(async () => {
  loading.value = true;
  error.value = "";

  try {
    settings.value =
      await store.fetchLoanInterestSettings();

    await loadExpectedAmount();
  } catch (e) {
    error.value =
      e.response?.data?.message ||
      e.message ||
      "Failed to load settings";
  } finally {
    loading.value = false;
  }
});

async function save() {
  saving.value = true;
  message.value = "";
  error.value = "";

  try {
    const updated =
      await store.updateLoanInterestSettings({
        defaultInterestRate: form.defaultInterestRate,
        interestType: form.interestType,
        interestPeriodDays: form.interestPeriodDays,
        gracePeriodDays: form.gracePeriodDays,
        defaultLoanTermDays: form.defaultLoanTermDays,
        borrowerLimitPercentageSalaryBased:
          form.borrowerLimitPercentageSalaryBased,
        borrowerLimitPercentagePreviousLoan:
          form.borrowerLimitPercentagePreviousLoan
      });

    settings.value = updated;

    message.value = "Settings saved.";

    await loadExpectedAmount();
  } catch (e) {
    error.value =
      e.response?.data?.message ||
      e.message ||
      "Failed to save settings";
  } finally {
    saving.value = false;
  }
}
</script>
