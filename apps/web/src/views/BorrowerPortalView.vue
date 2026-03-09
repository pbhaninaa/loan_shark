<template>
  <div class="page-shell">
    <AppPageHeader
      title="Client Portal"
      description="Clients can manage their own profile access, submit loan requests, and track repayments here."
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

    <v-row>
      <v-col cols="12" lg="4">
        <v-card>
          <v-card-title>My Profile</v-card-title>
          <v-divider />
          <v-card-text v-if="profile">
            <div class="text-h6">{{ profile.firstName }} {{ profile.lastName }}</div>
            <div class="text-body-2 text-medium-emphasis mb-4">Client #{{ profile.id }}</div>
            <v-list density="compact">
              <v-list-item title="Phone" :subtitle="profile.phone" />
              <v-list-item title="Email" :subtitle="profile.email || 'None'" />
              <v-list-item title="Address" :subtitle="profile.address" />
              <v-list-item title="Employment" :subtitle="profile.employmentStatus" />
              <v-list-item title="Monthly Income" :subtitle="`R ${profile.monthlyIncome}`" />
            </v-list>
            <div class="d-flex ga-2 flex-wrap mt-4">
              <v-chip :color="profile.status === 'BLACKLISTED' ? 'error' : 'success'" variant="tonal">
                {{ profile.status }}
              </v-chip>
              <v-chip color="info" variant="tonal">Risk Score: {{ profile.riskScore ?? "N/A" }}</v-chip>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="8">
        <AppTableCard title="My Loans" :count-label="`${loans.length} loans`">
          <AppDataTable
            title=""
            :headers="portalLoanHeaders"
            :items="loans"
            no-data-message="No loans."
          >
            <template #item.id="{ item }">#{{ item.id }}</template>
            <template #item.status="{ item }">
              <v-chip :color="statusColor(item.status)" size="small" variant="tonal">{{ item.status }}</v-chip>
            </template>
            <template #item.loanAmount="{ item }">R {{ item.loanAmount }}</template>
            <template #item.totalAmount="{ item }">R {{ item.totalAmount }}</template>
            <template #item.dueDate="{ item }">{{ item.dueDate || "None" }}</template>
            <template #item.actions="{ item }">
              <AppActionButton size="small" color="secondary" variant="tonal" text="View Schedule" @click="showSchedule(item.id)" />
            </template>
          </AppDataTable>
        </AppTableCard>
      </v-col>
    </v-row>

    <v-row class="mt-1">
      <v-col cols="12" lg="6">
        <v-card>
          <v-card-title>Repayment Schedule</v-card-title>
          <v-divider />
          <v-card-text>
            <div v-if="selectedLoanId" class="text-body-2 text-medium-emphasis mb-3">
              Showing installments for loan #{{ selectedLoanId }}
            </div>
            <AppDataTable
              title=""
              :headers="portalScheduleHeaders"
              :items="schedule"
              no-data-message="Choose a loan to view its repayment schedule."
            >
              <template #item.installmentNumber="{ item }">{{ item.installmentNumber }}</template>
              <template #item.dueDate="{ item }">{{ item.dueDate }}</template>
              <template #item.amountDue="{ item }">R {{ item.amountDue }}</template>
              <template #item.status="{ item }">
                <v-chip size="small" :color="scheduleColor(item.status)" variant="tonal">{{ item.status }}</v-chip>
              </template>
            </AppDataTable>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" lg="6">
        <AppTableCard title="Notifications">
          <template #header-actions>
            <AppActionButton size="small" variant="tonal" color="secondary" prepend-icon="mdi-refresh" text="Refresh" @click="loadPortal" />
          </template>
            <v-list lines="two">
              <v-list-item
                v-for="notification in notifications"
                :key="notification.id"
                :title="notification.channel"
                :subtitle="notification.message"
              >
                <template #append>
                  <v-chip size="small" color="info" variant="tonal">{{ formatDate(notification.createdAt) }}</v-chip>
                </template>
              </v-list-item>
            </v-list>
            <div v-if="!notifications.length" class="text-medium-emphasis">No notifications yet.</div>
        </AppTableCard>
      </v-col>
    </v-row>

    <AppDialogCard v-model="showApplyDialog" title="Apply For A Loan" :max-width="520" @update:model-value="onApplyDialogToggle">
      <v-form @submit.prevent="applyLoan">
            <v-alert type="info" variant="tonal" density="compact" class="mb-3">
              <div class="mb-2">Interest and terms are set by the business. You can pay any amount at any time; each payment reduces what you owe and interest continues per business rules until the loan is paid off.</div>
              <div v-if="loanSettings" class="text-caption mt-2 pt-2" style="border-top: 1px solid rgba(255,255,255,0.2);">
                <strong>Current settings:</strong> {{ loanSettings.defaultInterestRate }}% interest ({{ loanSettings.interestType }}), interest period {{ loanSettings.interestPeriodDays }} days, grace period {{ loanSettings.gracePeriodDays }} days, default loan term {{ loanSettings.defaultLoanTermDays }} days.
              </div>
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
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppDialogCard from "../components/ui/AppDialogCard.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import api from "../services/api";
import { useAppStore } from "../store";

const store = useAppStore();
const showApplyDialog = ref(false);
const selectedLoanId = ref(null);
const message = ref("");
const error = ref("");
const loanSettings = ref(null);

const applyForm = reactive({
  loanAmount: 1000
});

function onApplyDialogToggle(isOpen) {
  if (isOpen) {
    store.fetchLoanInterestSettings().then((s) => { loanSettings.value = s; }).catch(() => { loanSettings.value = null; });
  }
}

const profile = computed(() => store.borrowerProfile);
const loans = computed(() => store.loans);
const schedule = computed(() => store.loanSchedule);
const notifications = computed(() => store.notifications);

const portalLoanHeaders = [
  { title: "Loan", key: "id" },
  { title: "Status", key: "status" },
  { title: "Amount", key: "loanAmount" },
  { title: "Total", key: "totalAmount" },
  { title: "Due", key: "dueDate" },
  { title: "Actions", key: "actions" }
];
const portalScheduleHeaders = [
  { title: "Installment", key: "installmentNumber" },
  { title: "Due Date", key: "dueDate" },
  { title: "Amount", key: "amountDue" },
  { title: "Status", key: "status" }
];

onMounted(async () => {
  await loadPortal();
});

async function loadPortal() {
  error.value = "";
  try {
    await Promise.all([
      store.fetchMyBorrower(),
      store.fetchMyLoans(),
      store.fetchMyNotifications()
    ]);
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load client portal";
  }
}

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
    await store.fetchMyLoans();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not submit loan application";
  }
}

async function showSchedule(loanId) {
  selectedLoanId.value = loanId;
  error.value = "";
  try {
    await store.fetchLoanSchedule(loanId);
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load repayment schedule";
  }
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

function scheduleColor(status) {
  if (status === "OVERDUE") {
    return "error";
  }
  if (status === "PAID") {
    return "success";
  }
  return "warning";
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString() : "-";
}
</script>
