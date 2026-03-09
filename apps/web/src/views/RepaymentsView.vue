<template>
  <div class="page-shell">
    <AppPageHeader
      title="Repayments"
      description="Capture incoming payments and view all payment history across loans. Use the filter to show a specific loan."
    >
      <template #actions>
        <AppActionButton text="Record Payment" prepend-icon="mdi-cash-check" @click="openCapturePaymentDialog" />
      </template>
    </AppPageHeader>

    <AppTableCard
      title="Repayment History"
      :count-label="`${repaymentsPage.totalElements} payments`"
      chip-color="info"
    >
      <AppDataTable
        title=""
        :headers="repaymentHeaders"
        :items="repayments"
        :loading="loading"
        show-search
        search-placeholder="Search repayments"
        no-data-message="No repayments."
        :items-per-page="8"
        @update:search-value="onSearch"
      >
        <template #header-actions>
          <AppSelectField
            v-model="filterLoanId"
            label="Filter by loan"
            clearable
            placeholder="All loans"
            :items="loanFilterOptions"
            item-title="title"
            item-value="value"
            density="compact"
            hide-details
            style="min-width: 200px;"
            @update:model-value="onFilterLoanChange"
          />
        </template>
        <template #item.borrowerFullName="{ item }">{{ item.borrowerFullName || item.borrowerUsername || "None" }}</template>
        <template #item.loanId="{ item }">#{{ item.loanId }}</template>
        <template #item.amountPaid="{ item }">{{ formatCurrency(item.amountPaid) }}</template>
        <template #item.paymentDate="{ item }">{{ formatDate(item.paymentDate) }}</template>
        <template #item.paymentMethod="{ item }">
          <v-chip color="success" size="small" variant="tonal">{{ item.paymentMethod }}</v-chip>
        </template>
        <template #item.referenceNumber="{ item }">{{ item.referenceNumber }}</template>
        <template #item.capturedByUsername="{ item }">{{ item.capturedByUsername || "None" }}</template>
        <template #footer>
          <AppPaginationFooter v-model="page" :total-pages="repaymentsPage.totalPages" :total-elements="repaymentsPage.totalElements" @update:model-value="loadRepayments" />
        </template>
      </AppDataTable>
    </AppTableCard>

    <AppDialogCard v-model="showRepaymentDialog" title="Capture Payment" :max-width="520">
      <v-alert type="info" variant="tonal" density="compact" class="mb-4">
        Select the loan and enter the amount received. The borrower can pay in full, pay installments as due, or pay whatever they can afford at any time—any amount is applied to their balance until the loan is paid off.
      </v-alert>
      <v-form @submit.prevent="recordRepayment">
            <AppSelectField
              v-model="form.loanId"
              label="Loan (payer account)"
              prepend-inner-icon="mdi-pound"
              :items="loanOptions"
              item-title="title"
              item-value="value"
            />
            <v-alert v-if="selectedLoan" type="warning" variant="tonal" density="compact" class="mt-2 mb-2">
              <strong>Paying for:</strong> {{ selectedLoan.borrowerFullName || selectedLoan.borrowerUsername || `Client #${selectedLoan.borrowerId}` }}
              <span class="text-caption d-block mt-1">Confirm this is the person making the payment to avoid crediting the wrong account.</span>
            </v-alert>
            <AppTextField v-model.number="form.amountPaid" label="Amount paid" type="number" prepend-inner-icon="mdi-cash" />
            <AppSelectField v-model="form.paymentMethod" label="Payment method" :items="paymentMethods" />
            <AppTextField v-model="form.referenceNumber" label="Reference number" prepend-inner-icon="mdi-receipt-text-outline" />
            <div class="d-flex ga-2">
              <AppActionButton text="Record Payment" type="submit" prepend-icon="mdi-cash-check" class="flex-1-1" />
              <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="showRepaymentDialog = false" />
            </div>
      </v-form>
    </AppDialogCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppDialogCard from "../components/ui/AppDialogCard.vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import api from "../services/api";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const store = useAppStore();
const repayments = computed(() => store.repayments);
const repaymentsPage = computed(() => store.repaymentsPage);
const loanOptions = computed(() =>
  store.loans
    .filter((loan) => loan.status === "ACTIVE")
    .map((loan) => {
      const payerLabel = loan.borrowerFullName || loan.borrowerUsername || `Client #${loan.borrowerId}`;
      return {
        title: `Loan #${loan.id} — ${payerLabel} — ${loan.status}`,
        value: loan.id
      };
    })
);
const selectedLoan = computed(() =>
  form.loanId ? store.loans.find((l) => l.id === form.loanId) : null
);
const filterLoanId = ref(null);
const loanFilterOptions = computed(() => [
  { title: "All loans", value: null },
  ...store.loans.map((loan) => {
    const label = loan.borrowerFullName || loan.borrowerUsername || `Client #${loan.borrowerId}`;
    return { title: `Loan #${loan.id} — ${label}`, value: loan.id };
  })
]);
const paymentMethods = ["CASH", "EFT", "MOBILE_TRANSFER"];
const showRepaymentDialog = ref(false);
const search = ref("");
const page = ref(0);
const loading = ref(false);

const repaymentHeaders = [
  { title: "Payer (full name)", key: "borrowerFullName" },
  { title: "Loan", key: "loanId" },
  { title: "Amount", key: "amountPaid" },
  { title: "Date", key: "paymentDate" },
  { title: "Method", key: "paymentMethod" },
  { title: "Reference", key: "referenceNumber" },
  { title: "Recorded by", key: "capturedByUsername" }
];

const form = reactive({
  loanId: null,
  amountPaid: 325,
  paymentMethod: "CASH",
  referenceNumber: ""
});

onMounted(async () => {
  await store.fetchLoans();
  await loadRepayments(0);
  if (!form.loanId && loanOptions.value.length) {
    form.loanId = loanOptions.value[0].value;
  }
});

function formatDate(value) {
  if (!value) return "None";
  const d = new Date(value);
  return d.toLocaleDateString(undefined, { dateStyle: "short" }) + " " + d.toLocaleTimeString(undefined, { timeStyle: "short" });
}

function onFilterLoanChange() {
  page.value = 0;
  loadRepayments(0);
}

async function openCapturePaymentDialog() {
  try {
    const { data } = await api.get("/repayments/next-reference");
    form.referenceNumber = data?.nextReference ?? "PAY-1001";
  } catch {
    form.referenceNumber = "PAY-1001";
  }
  showRepaymentDialog.value = true;
}

async function recordRepayment() {
  const res = await api.post("/repayments", form);
  showRepaymentDialog.value = false;
  await loadRepayments(0);
  const account = res?.data?.borrowerUsername || "Account";
  const amount = res?.data?.amountPaid != null ? formatCurrency(res.data.amountPaid) : "";
  if (typeof toast !== "undefined" && amount) {
    toast.success(`Payment recorded. ${account}'s debt reduced by ${amount}.`);
  }
}

async function loadRepayments(nextPage = page.value) {
  page.value = nextPage;
  loading.value = true;
  try {
    const loanId = filterLoanId.value ?? null;
    await store.fetchRepayments(loanId, { q: search.value, page: page.value, size: 8 });
  } finally {
    loading.value = false;
  }
}

function onSearch(value) {
  search.value = value;
  page.value = 0;
  loadRepayments(0);
}
</script>
