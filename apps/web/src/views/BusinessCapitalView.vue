<template>
  <div class="page-shell">
    <AppPageHeader
      title="Business capital"
      description="Track how much you put from your pocket and how much you got back. Loans are funded from this pool; we never forget what you contributed."
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

    <v-alert type="info" variant="tonal" density="compact" class="mb-4">
      <strong>Capital Invested:</strong> {{ formatCurrency(summary.principalAmount) }} from your pocket
      &nbsp;·&nbsp;
      <strong>You got back:</strong> {{ formatCurrency(summary.totalMoneyIn) }} from clients (repayments)
    </v-alert>

    <v-row class="mb-4">
      <v-col cols="10" sm="4" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-secondary">
            <v-icon start>mdi-wallet-plus</v-icon>
            What you put in
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="text-h4 font-weight-bold">
              {{ formatCurrency(summary.principalAmount) }}
            </div>
            <p class="text-caption text-medium-emphasis mt-2 mb-0">
              Total you added from your pocket (every top-up). We track this so you always know how much you gave.
            </p>
          </v-card-text>
        </v-card>
      </v-col>
    <v-col cols="10" sm="4" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-primary">
            <v-icon start>mdi-cash-multiple</v-icon>
         Working Capital
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="text-h4 font-weight-bold">
              {{ formatCurrency(summary.balance) }}
            </div>
            <p class="text-caption text-medium-emphasis mt-2 mb-0">
              Principal + repayments in − disbursements out. Use this when lending to keep rotation flowing.
            </p>
          </v-card-text>
        </v-card>
      </v-col>
 <v-col cols="10" sm="4" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-error">
            <v-icon start>mdi-cash-minus</v-icon>
            Funds Disbursed
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="text-h4 font-weight-bold">
              {{ formatCurrency(summary.totalMoneyOut) }}
            </div>
            <p class="text-caption text-medium-emphasis mt-2 mb-0">
              Amount we gave to clients (disbursed).
            </p>
          </v-card-text>
        </v-card>
      </v-col>
   <v-col cols="10" sm="4" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-success">
            <v-icon start>mdi-cash-plus</v-icon>
          Cash Inflow
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="text-h4 font-weight-bold">
              {{ formatCurrency(summary.totalMoneyIn) }}
            </div>
            <p class="text-caption text-medium-emphasis mt-2 mb-0">
              Total paid by clients (repayments). So you can see how much you got at the end.
            </p>
          </v-card-text>
        </v-card>
      </v-col>
<v-col cols="10" sm="4" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-warning">
            <v-icon start>mdi-currency-usd</v-icon>
           Outstanding Loans
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="text-h4 font-weight-bold">
              {{ formatCurrency(summary.expectedAmount) }}
            </div>
            <p class="text-caption text-medium-emphasis mt-2 mb-0">
              Pending payments from clients (not yet received).
            </p>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon start>mdi-plus-circle-outline</v-icon>
        Add funds
      </v-card-title>
      <v-divider />
      <v-card-text>
        <p class="text-body-2 text-medium-emphasis mb-4">
          Add money from your pocket (we track it under &quot;What you put in&quot;). When clients repay, that goes to &quot;What you got back&quot; and into the pool so you can keep lending.
        </p>
        <v-form @submit.prevent="topUp">
          <v-row dense align="center">
              <v-col cols="12" md="4" class="d-flex align-center">
              <AppTextField
                v-model.number="topUpAmount"
                label="Amount to add"
                type="number"
                step="0.01"
                min="0.01"
                prepend-inner-icon="mdi-cash-plus"
                hint="Initial capital or top-up to grow the business."
              />
            </v-col>
            <v-col cols="12" md="4" class="d-flex align-center">
              <AppActionButton text="Add funds" class="mb-5" type="submit" prepend-icon="mdi-plus-circle-outline" :loading="topUpLoading" />
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const store = useAppStore();
const summary = ref({
  balance: 0,
  principalAmount: 0,
  totalMoneyOut: 0,
  totalMoneyIn: 0,
  expectedAmount: 0
});
const topUpAmount = ref("");
const topUpLoading = ref(false);
const message = ref("");
const error = ref("");

onMounted(async () => {
  await loadSummary();
});

async function loadSummary() {
  try {
    const data = await store.fetchBusinessCapitalSummary();
    summary.value = {
      balance: data.balance ?? 0,
      principalAmount: data.principalAmount ?? 0,
      totalMoneyOut: data.totalMoneyOut ?? 0,
      totalMoneyIn: data.totalMoneyIn ?? 0,
      expectedAmount: data.expectedAmount ?? 0
    };
  } catch (e) {
    error.value = e.response?.data?.message || "Could not load capital summary.";
  }
}

async function topUp() {
  const amount = Number(topUpAmount.value);
  if (!amount || amount <= 0) {
    error.value = "Enter an amount greater than zero.";
    return;
  }
  topUpLoading.value = true;
  message.value = "";
  error.value = "";
  try {
    await store.topUpBusinessCapital(amount);
    topUpAmount.value = "";
    await loadSummary();
    message.value = `Added ${formatCurrency(amount)} from your pocket. Total you put in: ${formatCurrency(summary.value.principalAmount)}. Money made (available to lend): ${formatCurrency(summary.value.balance)}.`;
  } catch (e) {
    error.value = e.response?.data?.message || "Could not add funds.";
  } finally {
    topUpLoading.value = false;
  }
}
</script>
