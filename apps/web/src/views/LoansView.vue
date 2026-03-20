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

    <!-- Pending Loan Requests: always has Actions (Approve, Reject, Edit, Cancel) -->
    <AppTableCard
      v-if="store.isOwner || store.isCashier"
      title="Pending Loan Requests"
      :count-label="`${pendingLoans.length} pending`"
      chip-color="warning"
      class="mb-6"
    >
      <AppDataTable
        title=""
        :headers="pendingHeaders"
        :items="pendingLoans"
        :loading="pendingLoading"
        show-search
        search-placeholder="Search pending requests"
        no-data-message="No pending loan requests."
        :items-per-page="3"
        @update:search-value="onPendingSearch"
      >
        <template #item.id="{ item }">#{{ item.id }}</template>
        <template #item.borrowerId="{ item }">{{ borrowerName(item.borrowerId) }}</template>
        <template #item.status="{ item }">
          <v-chip :color="statusColor(item.status)" size="small" variant="tonal">{{ item.status }}</v-chip>
        </template>
        <template #item.riskBand="{ item }">
          <v-chip :color="riskColor(item.riskBand)" size="small" variant="tonal">{{ item.riskBand }} / {{ item.riskScore }}</v-chip>
        </template>
        <template #item.loanAmount="{ item }">{{ formatCurrency(item.loanAmount) }}</template>
        <template #item.totalAmount="{ item }">{{ formatCurrency(item.totalAmount) }}</template>
        <template #item.actions="{ item }">
          <div class="d-flex ga-2 flex-wrap">
           <AppActionButton
  v-if="canActOnLoan(item)"
  size="small"
  color="success"
  variant="flat"
  text="Approve"
  :loading="actionLoading.approve === item.id"
  @click="approve(item.id)"
/>

<AppActionButton
  v-if="canActOnLoan(item)"
  size="small"
  color="error"
  variant="flat"
  text="Reject"
  :loading="actionLoading.reject === item.id"
  @click="reject(item.id)"
/>
            <AppActionButton
              v-if="store.isOwner"
              size="small"
              variant="tonal"
              text="Edit"
              prepend-icon="mdi-pencil-outline"
              @click="openEditLoan(item)"
            />
            <AppActionButton
  v-if="store.isOwner"
  size="small"
  color="error"
  variant="tonal"
  text="Cancel"
  prepend-icon="mdi-close-circle-outline"
  :loading="actionLoading.cancel === item.id"
  @click="cancelLoan(item)"
/>
          </div>
        </template>
        <template #footer>
          <AppPaginationFooter v-model="pendingPage" :total-pages="pendingLoansPage.totalPages" :total-elements="pendingLoansPage.totalElements" @update:model-value="loadPendingLoans" />
        </template>
      </AppDataTable>
    </AppTableCard>

    <!-- Loans (Active & Completed): Loan Amount, Total Amount, Pending Amount — no Actions -->
    <AppTableCard
      v-if="store.isOwner || store.isCashier"
      title="Loans"
      :count-label="`${loans.length} active or completed`"
      chip-color="primary"
    >
      <AppDataTable
        title=""
        :headers="loansTableHeaders"
        :items="loans"
        :loading="loansLoading"
        show-search
        search-placeholder="Search loans"
        no-data-message="No active or completed loans."
        :items-per-page="8"
        @update:search-value="onLoansSearch"
      >
        <template #item.id="{ item }">#{{ item.id }}</template>
        <template #item.borrowerId="{ item }">{{ borrowerName(item.borrowerId) }}</template>
        <template #item.status="{ item }">
          <v-chip :color="statusColor(item.status)" size="small" variant="tonal">{{ item.status }}</v-chip>
        </template>
        <template #item.loanAmount="{ item }">{{ formatCurrency(item.loanAmount) }}</template>
        <template #item.totalAmount="{ item }">{{ formatCurrency(item.totalAmount) }}</template>
        <template #item.pendingAmount="{ item }">{{ formatCurrency(item.pendingAmount) }}</template>
        <template #footer>
          <AppPaginationFooter v-model="loansPageNum" :total-pages="loansPage.totalPages" :total-elements="loansPage.totalElements" @update:model-value="loadLoansTable" />
        </template>
      </AppDataTable>
    </AppTableCard>

    <AppDialogCard v-model="showEditDialog" title="Edit loan (PENDING only)" :max-width="440">
      <v-alert v-if="editError" type="error" variant="tonal" density="compact" class="mb-3">
        {{ editError }}
      </v-alert>
      <v-form v-if="editingLoan" @submit.prevent="saveEditLoan">
        <AppTextField v-model.number="editForm.loanAmount" label="Loan amount" type="number" prepend-inner-icon="mdi-cash-plus" />
        <AppTextField v-model.number="editForm.loanTermDays" label="Loan term (days, optional)" type="number" prepend-inner-icon="mdi-calendar" hint="Leave blank to keep current term" persistent-hint />
        <div class="d-flex ga-2 mt-3">
         <AppActionButton
  text="Update"
  type="submit"
  :loading="actionLoading.update"
/>
          <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="showEditDialog = false" />
        </div>
      </v-form>
    </AppDialogCard>

    <v-dialog v-model="showCancelConfirm" max-width="440" persistent>
      <v-card>
        <v-card-title>Cancel loan application</v-card-title>
        <v-card-text>
          Pending loan #{{ cancelLoanId }} will be removed. This cannot be undone. Continue?
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="showCancelConfirm = false">No</v-btn>
          <v-btn color="error" variant="flat" :loading="confirmCancelLoading" @click="confirmCancelLoan">Yes, cancel loan</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

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
          Money made (available for lending): <strong>{{ formatCurrency(availableBalance) }}</strong>. Lending is limited to this so the rotation keeps flowing. If the amount requested exceeds this, the application will be rejected until the owner adds funds.
        </v-alert>
        <v-alert type="info" variant="tonal" density="compact" class="mb-3">
          <div class="mb-2">Interest and terms are set by the business. The client only chooses the amount; repayments reduce what they owe and interest continues per business rules until the loan is paid off.</div>
          <div v-if="loanSettings" class="text-caption mt-2 pt-2" style="border-top: 1px solid rgba(255,255,255,0.2);">
            <strong>Current settings:</strong> {{ loanSettings.defaultInterestRate }}% interest ({{ loanSettings.interestType }}), interest period {{ loanSettings.interestPeriodDays }} days, grace period {{ loanSettings.gracePeriodDays }} days, default loan term {{ loanSettings.defaultLoanTermDays }} days.
          </div>
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
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import api from "../services/api";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const store = useAppStore();
const pendingLoans = computed(() => (store.pendingLoans || []).filter((l) => l.status === "PENDING"));
const pendingLoansPage = computed(() => store.pendingLoansPage);
const loans = computed(() => store.loans);
const loansPage = computed(() => store.loansPage);
const borrowers = computed(() => store.borrowers);
const showApplyDialog = ref(false);
const showEditDialog = ref(false);
const editingLoan = ref(null);
const editForm = reactive({ loanAmount: 0, loanTermDays: null });
const editError = ref("");
const editLoading = ref(false);
const showCancelConfirm = ref(false);
const cancelLoanId = ref(null);
const cancelLoading = ref(null);
const confirmCancelLoading = ref(false);
const pendingSearch = ref("");
const pendingPage = ref(0);
const pendingLoading = ref(false);
const loansSearch = ref("");
const loansPageNum = ref(0);
const loansLoading = ref(false);
const remindLoading = ref(null);
const actionLoading = reactive({
  approve: null,   // loanId currently being approved
  reject: null,    // loanId currently being rejected
  cancel: null,    // loanId currently being cancelled
  update: false    // update button in edit dialog
});
const pendingHeaders = computed(() => [
  // { title: "ID", key: "id" },
  
  { title: "Client", key: "borrowerId" },
  { title: "Status", key: "status" },
  { title: "Risk", key: "riskBand" },
  { title: "Amount", key: "loanAmount" },
  { title: "Total", key: "totalAmount" },
  { title: "Actions", key: "actions",   sortable: false }
]);

const loansTableHeaders = computed(() => [
  // { title: "ID", key: "id" },
  { title: "Client", key: "borrowerId" },
  { title: "Status", key: "status" },
  { title: "Loan Amount", key: "loanAmount" },
  { title: "Total Amount", key: "totalAmount" },
  { title: "Pending Amount", key: "pendingAmount" }
]);
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
const loanSettings = ref(null);

function onApplyDialogToggle(isOpen) {
  if (isOpen) {
    applyError.value = "";
    store.fetchBusinessCapitalBalance().then((b) => { availableBalance.value = b; }).catch(() => { availableBalance.value = null; });
    store.fetchLoanInterestSettings().then((s) => { loanSettings.value = s; }).catch(() => { loanSettings.value = null; });
  }
}

onMounted(async () => {
  await Promise.all([
    store.isOwner || store.isCashier ? Promise.all([loadPendingLoans(), loadLoansTable()]) : Promise.resolve(),
    store.fetchBorrowers({ page: 0, size: 100 })
  ]);
  if (!form.borrowerId && borrowerOptions.value.length) {
    form.borrowerId = borrowerOptions.value[0].value;
  }
});

async function refreshBothTables() {
  await Promise.all([loadPendingLoans(), loadLoansTable()]);
}

async function applyLoan() {
  applyError.value = "";
  try {
    await api.post("/loans/apply", {
      borrowerId: form.borrowerId,
      loanAmount: form.loanAmount
    });
    showApplyDialog.value = false;
    await refreshBothTables();
  } catch (e) {
    applyError.value = e.response?.data?.message || e.message || "Application failed.";
  }
}

async function approve(loanId) {
  actionLoading.approve = loanId;
  try {
    await api.post("/loans/approve", { loanId, note: "Approved from portal" });
    await refreshBothTables();
  } finally {
    actionLoading.approve = null;
  }
}

async function reject(loanId) {
  actionLoading.reject = loanId;
  try {
    await api.post("/loans/reject", { loanId, note: "Rejected from portal" });
    await refreshBothTables();
  } finally {
    actionLoading.reject = null;
  }
}

// async function confirmCancelLoan() {
//   if (!cancelLoanId.value) return;
//   actionLoading.cancel = cancelLoanId.value;
//   try {
//     await store.deleteLoan(cancelLoanId.value);
//     showCancelConfirm.value = false;
//     cancelLoanId.value = null;
//     await refreshBothTables();
//   } finally {
//     actionLoading.cancel = null;
//   }
// }



function openEditLoan(loan) {
  editingLoan.value = loan;
  editForm.loanAmount = Number(loan.loanAmount);
  editForm.loanTermDays = loan.loanTermDays ?? null;
  editError.value = "";
  showEditDialog.value = true;
}

async function saveEditLoan() {
  if (!editingLoan.value) return;
  editError.value = "";
  actionLoading.update = true;   // <-- loading
  try {
    const payload = { loanAmount: editForm.loanAmount };
    if (editForm.loanTermDays != null && editForm.loanTermDays > 0)
      payload.loanTermDays = editForm.loanTermDays;
    await store.updateLoan(editingLoan.value.id, payload);
    showEditDialog.value = false;
    editingLoan.value = null;
    await refreshBothTables();
  } catch (e) {
    editError.value = e.response?.data?.message || e.message || "Failed to update loan.";
  } finally {
    actionLoading.update = false;
  }
}

function cancelLoan(loan) {
  cancelLoanId.value = loan.id;
  showCancelConfirm.value = true;
}

async function confirmCancelLoan() {
  if (!cancelLoanId.value) return;
  actionLoading.cancel = cancelLoanId.value;
  try {
    await store.deleteLoan(cancelLoanId.value);
    showCancelConfirm.value = false;
    cancelLoanId.value = null;
    await refreshBothTables();
  } finally {
    actionLoading.cancel = null;
  }
}

async function loadPendingLoans(nextPage = pendingPage.value) {
  pendingPage.value = nextPage;
  pendingLoading.value = true;
  try {
    await store.fetchPendingLoans({ q: pendingSearch.value, page: pendingPage.value, size: 5});
  } finally {
    pendingLoading.value = false;
  }
}

function onPendingSearch(value) {
  pendingSearch.value = value;
  pendingPage.value = 0;
  loadPendingLoans(0);
}

async function loadLoansTable(nextPage = loansPageNum.value) {
  loansPageNum.value = nextPage;
  loansLoading.value = true;
  try {
    await store.fetchLoans({
      q: loansSearch.value,
      page: loansPageNum.value,
      size: 5,
      status: ["ACTIVE", "COMPLETED"]
    });
  } finally {
    loansLoading.value = false;
  }
}

function onLoansSearch(value) {
  loansSearch.value = value;
  loansPageNum.value = 0;
  loadLoansTable(0);
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

function canActOnLoan(loan) {
  if (loan.status !== "PENDING") return false;
  if (store.isOwner) return true;
  if (store.isCashier) return Number(loan.loanAmount) < 10000;
  return false;
}
</script>
