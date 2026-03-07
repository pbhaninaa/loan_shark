<template>
  <div class="page-shell">
    <AppPageHeader
      title="Repayments"
      description="Capture incoming payments, verify references, and review repayment history for any loan."
    >
      <template #actions>
        <AppActionButton text="Record Payment" prepend-icon="mdi-cash-check" @click="showRepaymentDialog = true" />
      </template>
    </AppPageHeader>

    <AppTableCard title="Repayment History" :count-label="`${repayments.length} payments`" chip-color="info">
      <template #header-actions>
        <AppSearchField v-model="search" label="Search repayments" style="min-width: 240px;" @update:model-value="handleSearch" />
        <AppActionButton
          text="Load Loan History"
          color="secondary"
          variant="tonal"
          prepend-icon="mdi-history"
          @click="loadRepayments"
        />
      </template>
        <v-table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Loan</th>
              <th>Amount</th>
              <th>Method</th>
              <th>Reference</th>
              <th>Client</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="repayment in repayments" :key="repayment.id">
              <td>#{{ repayment.id }}</td>
              <td>{{ repayment.loanId }}</td>
              <td>{{ formatCurrency(repayment.amountPaid) }}</td>
              <td>
                <v-chip color="success" size="small" variant="tonal">
                  {{ repayment.paymentMethod }}
                </v-chip>
              </td>
              <td>{{ repayment.referenceNumber }}</td>
              <td>{{ repayment.capturedByUsername || "—" }}</td>
            </tr>
          </tbody>
        </v-table>
      <AppPaginationFooter v-model="page" :total-pages="repaymentsPage.totalPages" :total-elements="repaymentsPage.totalElements" @update:model-value="loadRepayments" />
    </AppTableCard>

    <AppDialogCard v-model="showRepaymentDialog" title="Capture Payment" :max-width="520">
      <v-form @submit.prevent="recordRepayment">
            <AppSelectField
              v-model="form.loanId"
              label="Loan"
              prepend-inner-icon="mdi-pound"
              :items="loanOptions"
              item-title="title"
              item-value="value"
            />
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
const repayments = computed(() => store.repayments);
const repaymentsPage = computed(() => store.repaymentsPage);
const loanOptions = computed(() =>
  store.loans
    .filter((loan) => loan.status !== "REJECTED")
    .map((loan) => ({
      title: `Loan #${loan.id} - Client #${loan.borrowerId} - ${loan.status}`,
      value: loan.id
    }))
);
const paymentMethods = ["CASH", "EFT", "MOBILE_TRANSFER"];
const showRepaymentDialog = ref(false);
const search = ref("");
const page = ref(0);

const form = reactive({
  loanId: null,
  amountPaid: 325,
  paymentMethod: "CASH",
  referenceNumber: "PAY-1001"
});

onMounted(async () => {
  await store.fetchLoans();
  if (!form.loanId && loanOptions.value.length) {
    form.loanId = loanOptions.value[0].value;
    await loadRepayments();
  }
});

async function recordRepayment() {
  await api.post("/repayments", form);
  showRepaymentDialog.value = false;
  await loadRepayments(0);
}

async function loadRepayments(nextPage = page.value) {
  if (!form.loanId) {
    return;
  }
  page.value = nextPage;
  await store.fetchRepayments(form.loanId, { q: search.value, page: page.value, size: 8 });
}

async function handleSearch() {
  page.value = 0;
  await loadRepayments(0);
}
</script>
