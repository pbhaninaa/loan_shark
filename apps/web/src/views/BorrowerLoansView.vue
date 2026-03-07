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

    <AppTableCard title="Loan Applications" :count-label="`${loans.length} loans`">
      <template #header-actions>
        <AppSearchField v-model="search" label="Search my loans" style="min-width: 240px;" @update:model-value="handleSearch" />
      </template>
      <v-table>
        <thead>
          <tr>
            <th>Loan</th>
            <th>Status</th>
            <th>Amount</th>
            <th>Total</th>
            <th>Due</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="loan in loans" :key="loan.id">
            <td>#{{ loan.id }}</td>
            <td>
              <v-chip :color="statusColor(loan.status)" size="small" variant="tonal">
                {{ loan.status }}
              </v-chip>
            </td>
            <td>{{ formatCurrency(loan.loanAmount) }}</td>
            <td>{{ formatCurrency(loan.totalAmount) }}</td>
            <td>{{ loan.dueDate || "-" }}</td>
            <td>
              <AppActionButton
                size="small"
                color="secondary"
                variant="tonal"
                text="View Schedule"
                @click="openSchedule(loan.id)"
              />
            </td>
          </tr>
        </tbody>
      </v-table>
      <AppPaginationFooter v-model="page" :total-pages="loansPage.totalPages" :total-elements="loansPage.totalElements" @update:model-value="loadLoans" />
    </AppTableCard>

    <AppDialogCard v-model="showApplyDialog" title="Apply For A Loan" :max-width="520" @update:model-value="onApplyDialogToggle">
      <v-form @submit.prevent="applyLoan">
        <v-alert v-if="availableBalance != null" type="info" variant="tonal" density="compact" class="mb-3">
          Available for lending: <strong>{{ formatCurrency(availableBalance) }}</strong>. If you request more than this amount, your application cannot be approved until the business adds funds.
        </v-alert>
        <v-alert type="info" variant="tonal" density="compact" class="mb-3">
          Interest and terms are set by the business. You can pay any amount at any time; each payment reduces what you owe and interest continues per business rules until the loan is paid off.
        </v-alert>
        <AppTextField v-model.number="applyForm.loanAmount" label="Loan amount" type="number" prepend-inner-icon="mdi-cash-plus" />
        <div class="d-flex ga-2">
          <AppActionButton text="Submit" type="submit" class="flex-1-1" />
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
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSearchField from "../components/ui/AppSearchField.vue";
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

const applyForm = reactive({
  loanAmount: 1000
});
const availableBalance = ref(null);

function onApplyDialogToggle(isOpen) {
  if (isOpen) {
    store.fetchBusinessCapitalBalance().then((b) => { availableBalance.value = b; }).catch(() => { availableBalance.value = null; });
  }
}

const loans = computed(() => store.loans);
const loansPage = computed(() => store.loansPage);

onMounted(async () => {
  try {
    await loadLoans();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load loans";
  }
});

async function applyLoan() {
  message.value = "";
  error.value = "";
  try {
    await api.post("/loans/apply", {
      borrowerId: store.borrowerId,
      loanAmount: applyForm.loanAmount
    });
    showApplyDialog.value = false;
    message.value = "Loan application submitted successfully.";
    await loadLoans();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not submit loan application";
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
  await store.fetchMyLoans({ q: search.value, page: page.value, size: 8 });
}

async function handleSearch() {
  page.value = 0;
  await loadLoans(0);
}
</script>
