<template>
  <div class="page-shell">
    <AppPageHeader
      title="Business capital"
      description="Amount left = money you currently have (initial + money in − money out). Money out = amount you gave to clients. Money in = amount paid by clients. Expected amount = pending payments from clients."
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

    <v-row class="mb-4">
      <v-col cols="12" sm="6" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-error">
            <v-icon start>mdi-cash-minus</v-icon>
            Money out
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
      <v-col cols="12" sm="6" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-success">
            <v-icon start>mdi-cash-plus</v-icon>
            Money in
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="text-h4 font-weight-bold">
              {{ formatCurrency(summary.totalMoneyIn) }}
            </div>
            <p class="text-caption text-medium-emphasis mt-2 mb-0">
              Amount paid by clients (repayments).
            </p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-warning">
            <v-icon start>mdi-currency-usd</v-icon>
            Expected amount
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="text-h4 font-weight-bold">
              {{ formatCurrency(summary.expectedAmount) }}
            </div>
            <p class="text-caption text-medium-emphasis mt-2 mb-0">
              Pending payments from clients .
            </p>
          </v-card-text>
        </v-card>
      </v-col>
      <v-col cols="12" sm="6" md="3">
        <v-card>
          <v-card-title class="d-flex align-center text-primary">
            <v-icon start>mdi-bank-outline</v-icon>
            Amount left
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="text-h4 font-weight-bold">
              {{ formatCurrency(summary.balance) }}
            </div>
            <p class="text-caption text-medium-emphasis mt-2 mb-0">
              Money you currently have (initial + money in ).
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
          Add initial capital or top up the lending pool. When clients repay, that money is added to the pool automatically.
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
    message.value = `Added ${formatCurrency(amount)}. New balance: ${formatCurrency(summary.value.balance)}.`;
  } catch (e) {
    error.value = e.response?.data?.message || "Could not add funds.";
  } finally {
    topUpLoading.value = false;
  }
}
</script>
