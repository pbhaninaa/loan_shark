<template>
  <div class="page-shell">
    <AppPageHeader
      title="My Loans"
      description="Track your loan applications and move into the repayment schedule for each loan."
    >
      <template #actions>
        <AppActionButton text="Apply For Loan" prepend-icon="mdi-cash-plus" @click="showApplyDialog = true" />
      </template>
    </AppPageHeader>

    <v-alert v-if="message" type="success" variant="tonal" class="mb-4">
      {{ message }}
    </v-alert>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <!-- Loan Terms & Conditions Card -->
    <v-card class="mb-4" elevation="2">
      <v-card-title class="d-flex align-center ga-2">
        <v-icon color="primary">mdi-information-outline</v-icon>
        Loan Terms & Conditions
      </v-card-title>
      <v-divider/>
      <v-card-text>
        <v-alert type="info" variant="tonal" density="compact" class="mb-3">
          <strong>Important:</strong> Interest and terms are set by the business. Review these settings carefully before applying for a loan.
        </v-alert>

        <div v-if="loanSettings">
          <v-row dense>
            <v-col cols="12" sm="6" md="4">
              <div class="text-caption text-medium-emphasis">Interest Rate</div>
              <div class="text-h6 font-weight-bold text-primary">{{ loanSettings.defaultInterestRate }}%</div>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <div class="text-caption text-medium-emphasis">Interest Type</div>
              <div class="text-h6">{{ loanSettings.interestType }}</div>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <div class="text-caption text-medium-emphasis">Interest Period</div>
              <div class="text-h6">{{ loanSettings.interestPeriodDays }} days</div>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <div class="text-caption text-medium-emphasis">Grace Period</div>
              <div class="text-h6">{{ loanSettings.gracePeriodDays }} days</div>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <div class="text-caption text-medium-emphasis">Default Loan Term</div>
              <div class="text-h6">{{ loanSettings.defaultLoanTermDays }} days</div>
            </v-col>
          </v-row>

          <v-divider class="my-3"></v-divider>

          <div class="text-body-2">
            <p class="mb-2">
              <v-icon size="small" color="success">mdi-check-circle</v-icon>
              <strong>Flexible Repayment:</strong> You can pay any amount at any time. Each payment reduces what you owe.
            </p>
            <p class="mb-2">
              <v-icon size="small" color="info">mdi-information</v-icon>
              <strong>How Interest Works:</strong> Interest starts when you receive the loan and accrues every {{ loanSettings.interestPeriodDays }} days using {{ loanSettings.interestType }} calculation at {{ loanSettings.defaultInterestRate }}% rate.
            </p>
            <p class="mb-0">
              <v-icon size="small" color="warning">mdi-clock-outline</v-icon>
              <strong>Grace Period:</strong> You have {{ loanSettings.gracePeriodDays }} days after each due date where no additional interest is charged. Interest continues to accrue per business rules until the loan is fully paid off.
            </p>
          </div>
        </div>
        <div v-else class="text-center py-4">
          <v-progress-circular indeterminate color="primary"></v-progress-circular>
          <div class="text-caption mt-2">Loading loan terms...</div>
        </div>
      </v-card-text>
    </v-card>

    <AppTableCard title="Loan Applications" :count-label="`${loans.length} loans`">
      <AppDataTable
        title=""
        :headers="loanHeaders"
        :items="loans"
        :loading="loading"
        show-search
        search-placeholder="Search my loans"
        no-data-message="No loans."
        :items-per-page="2"
        @update:search-value="onSearch"
      >
        <template #item.id="{ item }">#{{ item.id }}</template>
        <template #item.status="{ item }">
          <v-chip :color="statusColor(item.status)" size="small" variant="tonal">{{ item.status }}</v-chip>
        </template>
        <template #item.loanAmount="{ item }">{{ formatCurrency(item.loanAmount) }}</template>
        <template #item.totalAmount="{ item }">{{ formatCurrency(item.totalAmount) }}</template>
        <template #item.dueDate="{ item }">{{ item.dueDate || "None" }}</template>
        <template #item.pendingAmount="{ item }">{{ formatCurrency(item.pendingAmount) }}</template>

        <template #item.actions="{ item }">
        <AppActionButton
            size="small"
            color="primary"
            variant="tonal"
            text="View"
            class="d-inline-flex"
          @click="openSchedule(item.id)"
        />
      </template>
        <template #footer>
          <AppPaginationFooter v-model="page" :total-pages="loansPage.totalPages" :total-elements="loansPage.totalElements" @update:model-value="loadLoans" />
        </template>
      </AppDataTable>
    </AppTableCard>

    <AppDialogCard v-model="showApplyDialog" title="Apply For A Loan" :max-width="520" @update:model-value="onApplyDialogToggle">
      <v-form @submit.prevent="applyLoan">
        <v-alert v-if="availableBalance != null" type="info" variant="tonal" density="compact" class="mb-3">
          Money made (available for lending): <strong>{{ formatCurrency(availableBalance) }}</strong>. You can only be approved up to this amount. If you need more, the business must add funds first.
        </v-alert>
        <v-alert type="info" variant="tonal" density="compact" class="mb-3">
          <div>By applying, you agree to the loan terms and conditions displayed above. You only choose the <strong>loan amount</strong>; all other terms are set by the business.</div>
          <div v-if="loanSettings" class="text-caption mt-2 pt-2" style="border-top: 1px solid rgba(255,255,255,0.2);">
            <strong>Quick summary:</strong> {{ loanSettings.defaultInterestRate }}% interest ({{ loanSettings.interestType }}), {{ loanSettings.interestPeriodDays }}-day periods, {{ loanSettings.gracePeriodDays }}-day grace, {{ loanSettings.defaultLoanTermDays }}-day term.
          </div>
        </v-alert>
        <AppTextField v-model.number="applyForm.loanAmount" label="Loan amount" type="number" prepend-inner-icon="mdi-cash-plus" />
        <div class="d-flex ga-2">
          <AppActionButton   :loading="submitting" text="Submit" type="submit"  />
          <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="showApplyDialog = false" />
        </div>
      </v-form>
    </AppDialogCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppDialogCard from "../components/ui/AppDialogCard.vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import api from "../services/api";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const router = useRouter();
const store = useAppStore();
const showApplyDialog = ref(false);
const message = ref("");
const error = ref("");
const search = ref("");
const page = ref(0);
const loading = ref(false);
const submitting = ref(false);

const loanHeaders = [
  { title: "Loan", value: "id" },
  { title: "Status", value: "status" },
  { title: "Amount", value: "loanAmount" },
  { title: "Total", value: "totalAmount" },
  { title: "Due", value: "dueDate" },
  {title:"Pending Amount",value:"pendingAmount"},
  { title: "Actions", value: "actions",   sortable: false } 
];

const applyForm = reactive({
  loanAmount: null
});
const availableBalance = ref(null);
const loanSettings = ref(null);

function onApplyDialogToggle(isOpen) {
  if (isOpen) {
    store.fetchBusinessCapitalBalance().then((b) => { availableBalance.value = b; }).catch(() => { availableBalance.value = null; });
   
    loadSettings();
  }
}

const loans = computed(() => store.loans);
const loansPage = computed(() => store.loansPage);

onMounted(async () => {
  try {
    await loadLoans();
    await loadSettings();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load loans";
  }
});

async function loadSettings() {
  try {
    loanSettings.value = await store.fetchLoanInterestSettings();
  } catch (requestError) {
    console.error("Could not load loan settings:", requestError);
    loanSettings.value = null;
  }
}
async function applyLoan() {
  message.value = "";
  error.value = "";

  if (submitting.value) return;

  submitting.value = true;

  try {
    await api.post("/loans/apply", {
      borrowerId: store.borrowerId,
      loanAmount: applyForm.loanAmount
    });

    showApplyDialog.value = false;
    message.value = "Loan application submitted successfully.";
    applyForm.loanAmount = null;
    await loadLoans();
  } catch (requestError) {
    error.value =
      requestError.response?.data?.message ||
      "Could not submit loan application";
  } finally {
    submitting.value = false;
  }
}

function openSchedule(loanId) {

  router.push({ name: "borrower-schedule", query: { loanId } });
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

async function loadLoans(nextPage = page.value) {
  page.value = nextPage;
  loading.value = true;
  try {
    await store.fetchMyLoans({ q: search.value, page: page.value, size: 5 });
  } finally {
    loading.value = false;
  }
}

function onSearch(value) {
  search.value = value;
  page.value = 0;
  loadLoans(0);
}

async function handleSearch() {
  page.value = 0;
  await loadLoans(0);
}
</script>
