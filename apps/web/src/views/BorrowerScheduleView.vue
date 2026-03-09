<template>
  <div class="page-shell">
    <AppPageHeader
      title="Repayment Schedule"
      description="Choose one of your loans to review the exact installment plan and status."
    />

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <AppTableCard title="Loan Selection" :count-label="selectedLoanLabel" chip-color="secondary">
      <AppSelectField
        v-model="selectedLoanId"
        label="Loan"
        prepend-inner-icon="mdi-pound"
        :items="loanOptions"
        item-title="title"
        item-value="value"
        @update:model-value="loadSchedule"
      />
    </AppTableCard>

    <AppTableCard title="Installment Schedule" :count-label="`${schedule.length} installments`" chip-color="info">
      <AppDataTable
        title=""
        :headers="scheduleHeaders"
        :items="schedule"
        no-data-message="Select a loan to view its repayment schedule."
      >
        <template #item.installmentNumber="{ item }">{{ item.installmentNumber }}</template>
        <template #item.dueDate="{ item }">{{ item.dueDate }}</template>
        <template #item.amountDue="{ item }">{{ formatCurrency(item.amountDue) }}</template>
        <template #item.status="{ item }">
          <v-chip size="small" :color="scheduleColor(item.status)" variant="tonal">{{ item.status }}</v-chip>
        </template>
        <template #item.actions="{ item }">
          <div class="text-right">
            <AppActionButton
              v-if="item.status !== 'PAID' && Number(item.amountDue) > 0"
              size="small"
              color="primary"
              variant="tonal"
              text="Pay"
              prepend-icon="mdi-cash-check"
              :loading="payLoading && payingInstallment === item.installmentNumber"
              @click="openPayDialog(item)"
            />
            <span v-else class="text-medium-emphasis text-caption">—</span>
          </div>
        </template>
      </AppDataTable>
    </AppTableCard>

    <v-dialog v-model="showPayDialog" max-width="440" persistent>
      <v-card>
        <v-card-title class="d-flex align-center">
          <v-icon start>mdi-cash-check</v-icon>
          Pay installment #{{ payForm.installmentNumber }}
        </v-card-title>
        <v-divider />
        <v-card-text>
          <v-alert v-if="payError" type="error" variant="tonal" class="mb-3" density="compact">
            {{ payError }}
          </v-alert>
          <v-form ref="payFormRef" @submit.prevent="submitPay">
            <AppTextField
              v-model.number="payForm.amountPaid"
              label="Amount"
              type="number"
              step="0.01"
              min="0.01"
              prepend-inner-icon="mdi-cash"
              hint="Amount to pay for this installment."
            />
            <AppSelectField
              v-model="payForm.paymentMethod"
              label="Payment method"
              :items="paymentMethods"
              class="mt-2"
            />
            <AppTextField
              v-model="payForm.referenceNumber"
              label="Reference number"
              prepend-inner-icon="mdi-receipt-text-outline"
              readonly
              hint="Auto-generated from loan, installment and time."
              class="mt-2"
            />
          </v-form>
        </v-card-text>
        <v-divider />
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="closePayDialog">Cancel</v-btn>
          <AppActionButton
            text="Record payment"
            color="primary"
            prepend-icon="mdi-cash-check"
            :loading="payLoading"
            @click="submitPay"
          />
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import api from "../services/api";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const route = useRoute();
const router = useRouter();
const store = useAppStore();
const error = ref("");
const selectedLoanId = ref(null);

const paymentMethods = ["CASH", "EFT", "MOBILE_TRANSFER"];
const showPayDialog = ref(false);
const payLoading = ref(false);
const payError = ref("");
const payingInstallment = ref(null);
const payForm = ref({
  installmentNumber: null,
  amountPaid: 0,
  paymentMethod: "CASH",
  referenceNumber: ""
});

const schedule = computed(() => store.loanSchedule);
const scheduleHeaders = [
  { title: "Installment", key: "installmentNumber" },
  { title: "Due Date", key: "dueDate" },
  { title: "Amount", key: "amountDue" },
  { title: "Status", key: "status" },
  { title: "Actions", key: "actions" }
];
const loanOptions = computed(() =>
  store.loans.map((loan) => ({
    title: `Loan #${loan.id} - ${formatCurrency(loan.totalAmount)} - ${loan.status}`,
    value: loan.id
  }))
);
const selectedLoanLabel = computed(() => (selectedLoanId.value ? `Loan #${selectedLoanId.value}` : "Choose loan"));

onMounted(async () => {
  try {
    await store.fetchMyLoans();
    const routeLoanId = route.query.loanId ? Number(route.query.loanId) : null;
    selectedLoanId.value = routeLoanId || loanOptions.value[0]?.value || null;
    await loadSchedule(selectedLoanId.value);
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load repayment schedules";
  }
});

watch(
  () => route.query.loanId,
  async (loanId) => {
    if (!loanId) {
      return;
    }
    selectedLoanId.value = Number(loanId);
    await loadSchedule(selectedLoanId.value);
  }
);

async function loadSchedule(loanId) {
  if (!loanId) {
    return;
  }
  error.value = "";
  selectedLoanId.value = Number(loanId);
  router.replace({ name: "borrower-schedule", query: { loanId: selectedLoanId.value } });
  try {
    await store.fetchLoanSchedule(selectedLoanId.value);
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load repayment schedule";
  }
}

function scheduleColor(status) {
  if (status === "OVERDUE") {
    return "error";
  }
  if (status === "PAID") {
    return "success";
  }
  return "warning";
}

function generatePaymentReference(loanId, installmentNumber, payerId) {
  const ts = new Date();
  const y = ts.getFullYear();
  const m = String(ts.getMonth() + 1).padStart(2, "0");
  const d = String(ts.getDate()).padStart(2, "0");
  const h = String(ts.getHours()).padStart(2, "0");
  const min = String(ts.getMinutes()).padStart(2, "0");
  const s = String(ts.getSeconds()).padStart(2, "0");
  const timestamp = `${y}${m}${d}${h}${min}${s}`;
  const payer = payerId != null ? payerId : store.borrowerId ?? store.userId ?? "";
  return `Loan-${loanId}-Inst-${installmentNumber}-Payer-${payer}-${timestamp}`;
}

function openPayDialog(item) {
  payingInstallment.value = item.installmentNumber;
  const loanId = selectedLoanId.value;
  const payerId = store.borrowerId ?? store.userId;
  const ref = generatePaymentReference(loanId, item.installmentNumber, payerId);
  payForm.value = {
    installmentNumber: item.installmentNumber,
    amountPaid: Number(item.amountDue) || 0,
    paymentMethod: "CASH",
    referenceNumber: ref
  };
  payError.value = "";
  showPayDialog.value = true;
}

function closePayDialog() {
  showPayDialog.value = false;
  payError.value = "";
  payingInstallment.value = null;
}

async function submitPay() {
  if (!selectedLoanId.value) {
    payError.value = "Please select a loan.";
    return;
  }
  const amount = Number(payForm.value.amountPaid);
  if (!amount || amount <= 0) {
    payError.value = "Enter a valid amount.";
    return;
  }
  payError.value = "";
  payLoading.value = true;
  try {
    await api.post("/repayments", {
      loanId: selectedLoanId.value,
      amountPaid: amount,
      paymentMethod: payForm.value.paymentMethod,
      referenceNumber: String(payForm.value.referenceNumber).trim()
    });
    closePayDialog();
    await loadSchedule(selectedLoanId.value);
  } catch (e) {
    payError.value = e.response?.data?.message || e.message || "Payment could not be recorded.";
  } finally {
    payLoading.value = false;
  }
}
</script>
