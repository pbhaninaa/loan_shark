<template>
  <div class="page-shell">
    <AppPageHeader
      title="My payment history"
      description="View payments you have made across your loans. Use the filter to show a specific loan."
    />

    <AppTableCard
      title="Payment history"
      :count-label="`${repaymentsPage.totalElements} payments`"
      chip-color="info"
    >
      <AppDataTable
        title=""
        :headers="repaymentHeaders"
        :items="repayments"
        :loading="loading"
        show-search
        search-placeholder="Search your payments"
        no-data-message="No payments yet."
        :items-per-page="8"
        @update:search-value="onSearch"
      >
        <template #header-actions>
          <AppSelectField
            v-model="filterLoanId"
            label="Filter by loan"
            clearable
            placeholder="All my loans"
            :items="loanFilterOptions"
            item-title="title"
            item-value="value"
            density="compact"
            hide-details
            style="min-width: 200px;"
            @update:model-value="onFilterLoanChange"
          />
        </template>
        <template #item.borrowerFullName="{ item }">{{ item.borrowerFullName || item.borrowerUsername || "—" }}</template>
        <template #item.loanId="{ item }">#{{ item.loanId }}</template>
        <template #item.amountPaid="{ item }">{{ formatCurrency(item.amountPaid) }}</template>
        <template #item.paymentDate="{ item }">{{ formatDate(item.paymentDate) }}</template>
        <template #item.paymentMethod="{ item }">
          <v-chip color="success" size="small" variant="tonal">{{ item.paymentMethod }}</v-chip>
        </template>
        <template #item.referenceNumber="{ item }">{{ item.referenceNumber }}</template>
        <template #item.capturedByUsername="{ item }">{{ item.capturedByUsername || "—" }}</template>
        <template #footer>
          <AppPaginationFooter v-model="page" :total-pages="repaymentsPage.totalPages" :total-elements="repaymentsPage.totalElements" @update:model-value="loadRepayments" />
        </template>
      </AppDataTable>
    </AppTableCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const store = useAppStore();
const repayments = computed(() => store.repayments);
const repaymentsPage = computed(() => store.repaymentsPage);
const filterLoanId = ref(null);
const loanFilterOptions = computed(() => [
  { title: "All my loans", value: null },
  ...store.loans.map((loan) => {
    const label = loan.borrowerFullName || loan.borrowerUsername || `Loan #${loan.id}`;
    return { title: `Loan #${loan.id} — ${label}`, value: loan.id };
  })
]);
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

onMounted(async () => {
  await store.fetchMyLoans();
  await loadRepayments(0);
});

function formatDate(value) {
  if (!value) return "—";
  const d = new Date(value);
  return d.toLocaleDateString(undefined, { dateStyle: "short" }) + " " + d.toLocaleTimeString(undefined, { timeStyle: "short" });
}

function onFilterLoanChange() {
  page.value = 0;
  loadRepayments(0);
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
