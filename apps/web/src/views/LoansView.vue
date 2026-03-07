<template>
  <div class="page-shell">
    <AppPageHeader
      title="Loan Pipeline"
      description="Capture new applications, inspect risk results, and approve or reject from a cleaner queue."
    >
      <template #actions>
        <AppActionButton text="New Application" prepend-icon="mdi-cash-plus" @click="showApplyDialog = true" />
      </template>
    </AppPageHeader>

    <AppTableCard title="Loan Queue" :count-label="`${loans.length} applications`" chip-color="warning">
      <template #header-actions>
        <AppSearchField v-model="search" label="Search loans" style="min-width: 260px;" @update:model-value="handleSearch" />
      </template>
      <v-table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Client</th>
            <th>Status</th>
            <th>Risk</th>
            <th>Total</th>
            <th v-if="store.isOwner">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="loan in loans" :key="loan.id">
            <td>#{{ loan.id }}</td>
            <td>{{ borrowerName(loan.borrowerId) }}</td>
            <td>
              <v-chip :color="statusColor(loan.status)" size="small" variant="tonal">
                {{ loan.status }}
              </v-chip>
            </td>
            <td>
              <v-chip :color="riskColor(loan.riskBand)" size="small" variant="tonal">
                {{ loan.riskBand }} / {{ loan.riskScore }}
              </v-chip>
            </td>
            <td>{{ formatCurrency(loan.totalAmount) }}</td>
            <td v-if="store.isOwner">
              <div class="d-flex ga-2">
                <AppActionButton
                  v-if="loan.status === 'PENDING'"
                  size="small"
                  color="success"
                  variant="flat"
                  text="Approve"
                  @click="approve(loan.id)"
                />
                <AppActionButton
                  v-if="loan.status === 'PENDING'"
                  size="small"
                  color="error"
                  variant="flat"
                  text="Reject"
                  @click="reject(loan.id)"
                />
              </div>
            </td>
          </tr>
        </tbody>
      </v-table>
      <AppPaginationFooter v-model="page" :total-pages="loansPage.totalPages" :total-elements="loansPage.totalElements" @update:model-value="loadLoans" />
    </AppTableCard>

    <AppDialogCard v-model="showApplyDialog" title="Capture Loan Application" :max-width="520" @update:model-value="onApplyDialogToggle">
      <v-alert v-if="applyError" type="error" variant="tonal" density="compact" class="mb-3">
        {{ applyError }}
      </v-alert>
      <v-form @submit.prevent="applyLoan">
        <AppSelectField
          v-model="form.borrowerId"
          label="Client"
          prepend-inner-icon="mdi-account-search-outline"
          :items="borrowerOptions"
          item-title="title"
          item-value="value"
        />
        <v-alert v-if="availableBalance != null" type="info" variant="tonal" density="compact" class="mb-3">
          Available for lending: <strong>{{ formatCurrency(availableBalance) }}</strong>. If the amount requested exceeds this, the application will be rejected until the admin adds funds.
        </v-alert>
        <v-alert type="info" variant="tonal" density="compact" class="mb-3">
          Interest and terms are set by the business. The client only chooses the amount; repayments reduce what they owe and interest continues per business rules until the loan is paid off.
        </v-alert>
        <AppTextField v-model.number="form.loanAmount" label="Loan amount" type="number" prepend-inner-icon="mdi-cash-plus" />
        <div class="d-flex ga-2">
          <AppActionButton text="Submit Application" type="submit" prepend-icon="mdi-send-outline" class="flex-1-1" />
          <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="showApplyDialog = false" />
        </div>
      </v-form>
    </AppDialogCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppDialogCard from "../components/ui/AppDialogCard.vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSearchField from "../components/ui/AppSearchField.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import api from "../services/api";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const store = useAppStore();
const loans = computed(() => store.loans);
const borrowers = computed(() => store.borrowers);
const loansPage = computed(() => store.loansPage);
const showApplyDialog = ref(false);
const search = ref("");
const page = ref(0);
const borrowerOptions = computed(() =>
  borrowers.value.map((borrower) => ({
    title: `${borrower.firstName} ${borrower.lastName} - ${borrower.phone}`,
    value: borrower.id
  }))
);

const form = reactive({
  borrowerId: null,
  loanAmount: 1000
});
const applyError = ref("");
const availableBalance = ref(null);

function onApplyDialogToggle(isOpen) {
  if (isOpen) {
    applyError.value = "";
    store.fetchBusinessCapitalBalance().then((b) => { availableBalance.value = b; }).catch(() => { availableBalance.value = null; });
  }
}

onMounted(async () => {
  await Promise.all([loadLoans(), store.fetchBorrowers({ page: 0, size: 100 })]);
  if (!form.borrowerId && borrowerOptions.value.length) {
    form.borrowerId = borrowerOptions.value[0].value;
  }
});

async function applyLoan() {
  applyError.value = "";
  try {
    await api.post("/loans/apply", {
      borrowerId: form.borrowerId,
      loanAmount: form.loanAmount
    });
    showApplyDialog.value = false;
    await loadLoans();
  } catch (e) {
    applyError.value = e.response?.data?.message || e.message || "Application failed.";
  }
}

async function approve(loanId) {
  await api.post("/loans/approve", { loanId, note: "Approved from portal" });
  await loadLoans();
}

async function reject(loanId) {
  await api.post("/loans/reject", { loanId, note: "Rejected from portal" });
  await loadLoans();
}

async function loadLoans(nextPage = page.value) {
  page.value = nextPage;
  await store.fetchLoans({ q: search.value, page: page.value, size: 8 });
}

async function handleSearch() {
  page.value = 0;
  await loadLoans(0);
}

function riskColor(riskBand) {
  if (riskBand === "HIGH_RISK") {
    return "error";
  }
  if (riskBand === "MEDIUM_RISK") {
    return "warning";
  }
  return "success";
}

function statusColor(status) {
  if (status === "REJECTED" || status === "DEFAULTED") {
    return "error";
  }
  if (status === "PENDING") {
    return "warning";
  }
  if (status === "ACTIVE" || status === "APPROVED") {
    return "success";
  }
  return "info";
}

function borrowerName(borrowerId) {
  const borrower = borrowers.value.find((item) => item.id === borrowerId);
  return borrower ? `${borrower.firstName} ${borrower.lastName}` : `Client #${borrowerId}`;
}
</script>
