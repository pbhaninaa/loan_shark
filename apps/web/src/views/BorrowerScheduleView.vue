<template>
  <div class="page-shell">
    <AppPageHeader
      title="Repayment Schedule"
      description="Choose one of your loans to review the exact installment plan and status."
    />

 <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
  {{ error }}
</v-alert>

<v-alert
  v-if="message"
  type="info"
  variant="tonal"
  class="mb-4"
>
  {{ message }}
</v-alert>

    <AppTableCard title="Loan Selection" :count-label="selectedLoanLabel" chip-color="secondary">
      <AppSelectField
        v-model="selectedLoanId"
        label="Loan"
        prepend-inner-icon="mdi-pound"
        :items="loanOptions"
        item-title="title"
        item-value="value"
        @update:model-value="onLoanSelected"
      />
    </AppTableCard>

    <AppTableCard title="Installment Schedule" :count-label="`${schedule.length} installments`" chip-color="info">
      <p class="text-body-2 text-medium-emphasis mb-3">
        Suggested installments and due dates. You are not bound to these amounts pay any amount you can, in full or in parts, until the loan is paid off.
      </p>
      <AppDataTable
        title=""
        :headers="scheduleHeaders"
        :items="schedule"
        :items-per-page="5"
        no-data-message="Select a loan to view its repayment schedule."
      >
        <template #item.installmentNumber="{ item }">{{ item.installmentNumber }}</template>
        <template #item.dueDate="{ item }">{{ item.dueDate }}</template>
        <template #item.amountDue="{ item }">{{ formatCurrency(item.amountDue) }}</template>
        <template #item.status="{ item }">
          <v-chip size="small" :color="scheduleColor(item.status)" variant="tonal">{{ item.status }}</v-chip>
        </template>
   
       <template #item.actions="{ item }">
  <div class="d-flex ga-2 justify-center">

    <!-- Pay Button -->
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

    <v-tooltip text="Pay instantly using Capitec PayMe">
      <template #activator="{ props }">
        <AppActionButton
          v-bind="props"
          v-if="item.status !== 'PAID'"
          size="small"
          color="success"
          variant="flat"
          text="Instant Pay"
          prepend-icon="mdi-lightning-bolt"
          @click="instantPay()"
        />
      </template>
    </v-tooltip>

    <!-- <span v-else class="text-medium-emphasis text-caption">—</span> -->
  </div>
</template>
        <template #footer>
          <AppPaginationFooter
            v-model="page"
            :total-pages="repaymentsPage.totalPages"
            :total-elements="repaymentsPage.totalElements"
            @update:model-value="loadRepayments"
          />
        </template>
      </AppDataTable>
    </AppTableCard>

    <!-- Payment Dialog -->
    <v-dialog v-model="showPayDialog" max-width="440" persistent>
      <v-card>
        <v-card-title class="d-flex align-center">
          <v-icon start>mdi-cash-check</v-icon>
          Make a payment
        </v-card-title>
        <v-divider />
        <v-card-text>
          <v-alert type="info" variant="tonal" density="compact" class="mb-3">
            Pay any amount you can afford. You can pay in full, pay one installment, or pay a partial amount it all reduces your debt until the loan is paid off.
          </v-alert>
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
              :max="totalPendingAmount"
              prepend-inner-icon="mdi-cash"
              :hint="`Enter any amount up to ${formatCurrency(totalPendingAmount)}`"
            />
            <AppSelectField
              v-model="payForm.paymentMethod"
              label="Payment method"
              :items="paymentMethods"
              class="mt-2"
            />
           
            <AppTextField
              v-if="payForm.paymentMethod === 'MOBILE_TRANSFER'"
              type="file"
              label="Proof of Payment (PDF)"
              accept="application/pdf"
              prepend-inner-icon="mdi-file-pdf"
              @change="handleFileUpload"
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
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import api from "../services/api";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const route = useRoute();
const router = useRouter();
const store = useAppStore();

const error = ref("");
const selectedLoanId = ref(null);
const repaymentsPage = computed(() => store.repaymentsPage);

const paymentMethods = ["EFT", "MOBILE_TRANSFER"];
const showPayDialog = ref(false);
const payLoading = ref(false);
const payError = ref("");
const message = ref("");
const payingInstallment = ref(null);
const proofFile = ref(null);
const payForm = ref({
  installmentNumber: null,
  amountPaid: 0,
  paymentMethod: "MOBILE_TRANSFER",
  referenceNumber: ""
});

const schedule = computed(() => store.loanSchedule);
const scheduleHeaders = [
  { title: "Installment", key: "installmentNumber" },
  { title: "Due Date", key: "dueDate" },
  { title: "Amount", key: "amountDue" },
  { title: "Status", key: "status",   sortable: false },
  { title: "Actions", key: "actions",
  align:"center",   sortable: false}
];

const loanOptions = computed(() =>
  store.loans
 .filter((loan) => String(loan.status).toUpperCase() === "ACTIVE")
    .map((loan) => {
      const id = loan?.id != null ? String(loan.id) : "";
      return {
        title: `Loan #${id || "—"} - ${formatCurrency(loan.totalAmount)} - ${loan.status}`,
        value: id
      };
    })
    .filter((opt) => opt.value)
);

const selectedLoanLabel = computed(() =>
  selectedLoanId.value ? `Loan #${selectedLoanId.value}` : "Choose loan"
);

function normalizeLoanId(val) {
  if (val == null || val === "") return null;
  const s = String(val).trim();
  if (s === "" || s === "NaN" || s === "undefined") return null;
  return s;
}
async function instantPay() {
  try {
    // Call the store method which returns { message, timestamp, ... }
    const result = await store.instantPay();

    // Display the message in the UI
    message.value = result?.message || "Payment action completed.";
    console.log("Instant Pay result:", result);
  } catch (error) {
    message.value = "Failed to initiate Instant Pay. Please try again.";
    console.error("Instant Pay error:", error);
  }
}
function onLoanSelected(val) {
  const id = normalizeLoanId(val);
  selectedLoanId.value = id;
  if (id) loadSchedule(id);
}

onMounted(async () => {
  try {
    await store.fetchMyLoans();
    const routeLoanId = normalizeLoanId(route.query.loanId);
    const firstLoanId = loanOptions.value[0]?.value ?? null;
    const id = routeLoanId || (firstLoanId ? normalizeLoanId(firstLoanId) : null);
    selectedLoanId.value = id;
    if (id) {
      await loadSchedule(id);
    }
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load repayment schedules";
  }
});

watch(
  () => route.query.loanId,
  async (loanId) => {
    const id = normalizeLoanId(loanId);
    if (!id) return;
    selectedLoanId.value = id;
    await loadSchedule(id);
  }
);

async function loadSchedule(loanId) {
  const id = normalizeLoanId(loanId);
  if (!id) return;
  error.value = "";
  selectedLoanId.value = id;
  router.replace({ name: "borrower-schedule", query: { loanId: id } });
  try {
    await store.fetchLoanSchedule(id);
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load repayment schedule";
  }
}

function scheduleColor(status) {
  if (status === "OVERDUE") return "error";
  if (status === "PAID") return "success";
  return "warning";
}


function generatePaymentReference(loanId, installmentNumber) {
  const unique = Date.now().toString(36) + Math.random().toString(36).substring(2,5);

  return `PAY-${unique}`.toUpperCase();
}

const totalPendingAmount = computed(() => {
  const uniqueInstallments = new Map();
  schedule.value.forEach((item) => {
    if (item.status !== "PAID" && !uniqueInstallments.has(item.installmentNumber)) {
      uniqueInstallments.set(item.installmentNumber, Number(item.amountDue));
    }
  });
  return Array.from(uniqueInstallments.values()).reduce((sum, amount) => sum + amount, 0);
});

function openPayDialog(item) {
  payingInstallment.value = item.installmentNumber;
  const loanId = selectedLoanId.value;
  const ref = generatePaymentReference(loanId, item.installmentNumber);
  payForm.value = {
    installmentNumber: item.installmentNumber,
    amountPaid: Number(item.amountDue) || 0,
    paymentMethod: "MOBILE_TRANSFER",
    referenceNumber: ref
  };
  proofFile.value = null;
  payError.value = "";
  message.value = "";
  showPayDialog.value = true;
}

// Convert file to Base64 for DB storage
function fileToBase64(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = (error) => reject(error);
  });
}

function handleFileUpload(event) {
  const file = event.target.files[0];
  if (!file) {
    proofFile.value = null;
    return;
  }
  if (file.type !== "application/pdf") {
    payError.value = "Only PDF files are allowed for proof of payment.";
    proofFile.value = null;
    return;
  }
  payError.value = "";
  proofFile.value = file;
}

function closePayDialog() {
  showPayDialog.value = false;
  message.value = "";
  payError.value = "";
  proofFile.value = null;
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

  if (amount > totalPendingAmount.value) {
    payError.value = `You cannot pay more than your total pending amount: ${formatCurrency(totalPendingAmount.value)}`;
    return;
  }

  if (payForm.value.paymentMethod === "MOBILE_TRANSFER" && !proofFile.value) {
    payError.value = "You must upload a PDF proof of payment for CASH payments.";
    return;
  }

  payError.value = "";
  message.value = "";
  payLoading.value = true;

  try {
    let proofBase64 = null;
    if (payForm.value.paymentMethod === "MOBILE_TRANSFER" && proofFile.value) {
      proofBase64 = await fileToBase64(proofFile.value);
    }

    await api.post("/repayments", {
      loanId: selectedLoanId.value,
      amountPaid: amount,
      paymentMethod: payForm.value.paymentMethod,
      referenceNumber: String(payForm.value.referenceNumber).trim(),
      proof: proofBase64 // send Base64 to backend
    });

    message.value = "Payment saved successfully";
    closePayDialog();
    await loadSchedule(selectedLoanId.value);
  } catch (e) {
    payError.value = e.response?.data?.message || e.message || "Payment could not be recorded.";
  } finally {
    payLoading.value = false;
  }
}
</script>