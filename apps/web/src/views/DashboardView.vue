<template>
  <div class="page-shell">
    <AppPageHeader
      title="Operations Dashboard"
      description="Monitor lending health, cash flow, and team access from one place."
    >
      <template #actions>
        <v-chip color="primary" variant="tonal" size="large">Live overview</v-chip>
      </template>
    </AppPageHeader>

    <v-row>
      <v-col v-for="card in metricCards" :key="card.title" cols="12" sm="6" xl="4">
        <v-card>
          <v-card-text class="d-flex align-center justify-space-between">
            <div>
              <div class="text-overline text-medium-emphasis">{{ card.title }}</div>
              <div class="text-h4 font-weight-bold mt-2">{{ card.value }}</div>
              <div class="text-body-2 text-medium-emphasis mt-2">{{ card.caption }}</div>
            </div>
            <v-avatar :color="card.color" variant="tonal" size="56">
              <v-icon :icon="card.icon" />
            </v-avatar>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-row v-if="false">
      <v-col cols="12" lg="8">
        <v-card>
          <v-card-title class="d-flex justify-space-between align-center">
            <span>Operational Snapshot</span>
            <v-chip color="accent" variant="tonal">Today</v-chip>
          </v-card-title>
          <v-divider />
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6">
                <v-list lines="two">
                  <v-list-item prepend-icon="mdi-account-group-outline" title="Client portfolio">
                    <template #subtitle>{{ dashboard?.borrowers || 0 }} registered clients in the system</template>
                  </v-list-item>
                  <v-list-item prepend-icon="mdi-timer-sand" title="Pending review queue">
                    <template #subtitle>{{ dashboard?.pendingLoans || 0 }} loan applications awaiting action</template>
                  </v-list-item>
                  <v-list-item prepend-icon="mdi-cash-clock" title="Collections watch">
                    <template #subtitle>{{ dashboard?.overdueSchedules || 0 }} repayment schedules currently overdue</template>
                  </v-list-item>
                </v-list>
              </v-col>
              <v-col cols="12" md="6">
                <v-alert type="info" variant="tonal" class="mb-4">
                  Use the navigation menu to move between client onboarding, loan capture, repayments, and blacklist management.
                </v-alert>
                <v-alert type="warning" variant="tonal">
                  High-risk approval rules and audit logging remain enforced by the backend.
                </v-alert>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <v-row class="mt-1" v-if="store.isOwner">
      <v-col cols="12">
        <AppTableCard title="Recent Actions" :count-label="`${actions.length} items`" chip-color="secondary">
          <AppDataTable
            title=""
            :headers="actionHeaders"
            :items="actions"
            :loading="loading"
            show-search
            search-placeholder="Search actions"
            no-data-message="No actions."
            :items-per-page="8"
            @update:search-value="onSearch"
          >
            <template #item.category="{ item }">
              <v-chip :color="item.category === 'TRANSACTION' ? 'success' : 'info'" size="small" variant="tonal">{{ item.category }}</v-chip>
            </template>
            <template #item.action="{ item }">{{ item.action }}</template>
            <template #item.referenceNumber="{ item }">{{ item.referenceNumber || (item.loanId ? `Loan #${item.loanId}` : item.entity) }}</template>
            <template #item.amount="{ item }">{{ item.amount ? formatCurrency(item.amount) : "None" }}</template>
            <template #item.performedBy="{ item }">{{ item.performedBy || "By The System" }}</template>
            <template #item.authorizedBy="{ item }">{{ item.authorizedBy || "By The System" }}</template>
            <template #item.details="{ item }">
              <AppTruncateText :text="item.details" :max-chars="90" max-width="280px" />
            </template>
            <template #item.timestamp="{ item }">{{ formatDateTime(item.timestamp) }}</template>
            <template #footer>
              <AppPaginationFooter v-model="page" :total-pages="actionsPage.totalPages" :total-elements="actionsPage.totalElements" @update:model-value="loadActions" />
            </template>
          </AppDataTable>
        </AppTableCard>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTruncateText from "../components/ui/AppTruncateText.vue";
import { useAppStore } from "../store";
import { formatCurrency, formatDateTime } from "../utils/formatters";

const store = useAppStore();
const search = ref("");
const page = ref(0);
const loading = ref(false);

const actionHeaders = [
  { title: "Category", key: "category" },
  { title: "Action", key: "action" },
  { title: "Reference", key: "referenceNumber" },
  { title: "Amount", key: "amount" },
  { title: "Performed By", key: "performedBy" },
  { title: "Authorized By", key: "authorizedBy" },
  { title: "Details", key: "details" },
  { title: "Time", key: "timestamp" }
];

const dashboard = computed(() => store.dashboard);
const actions = computed(() => store.actions);
const actionsPage = computed(() => store.actionsPage);

const metricCards = computed(() => [
  {
    title: "Clients",
    value: dashboard.value?.borrowers ?? 0,
    caption: "Registered customer profiles",
    icon: "mdi-account-group-outline",
    color: "primary"
  },
  {
    title: "Pending Loans",
    value: dashboard.value?.pendingLoans ?? 0,
    caption: "Applications awaiting review",
    icon: "mdi-file-document-outline",
    color: "warning"
  },
  {
    title: "Active Loans",
    value: dashboard.value?.activeLoans ?? 0,
    caption: "Currently disbursed loans",
    icon: "mdi-cash-multiple",
    color: "success"
  },
  {
    title: "Overdue Schedules",
    value: dashboard.value?.overdueSchedules ?? 0,
    caption: "Repayments needing attention",
    icon: "mdi-alert-circle-outline",
    color: "error"
  },
  {
    title: "Principal Outstanding",
    value: formatCurrency(dashboard.value?.principalOutstanding ?? 0),
    caption: "Capital still out in the field",
    icon: "mdi-bank-outline",
    color: "info"
  },
  {
    title: "Repayments Captured",
    value: formatCurrency(dashboard.value?.repaymentsCaptured ?? 0),
    caption: "Collections posted so far",
    icon: "mdi-cash-check",
    color: "accent"
  }
]);

onMounted(async () => {
  await store.fetchDashboard();
  if (store.isOwner) {
    await loadActions();
  }
});

async function loadActions(nextPage = page.value) {
  page.value = nextPage;
  loading.value = true;
  try {
    await store.fetchActions({ q: search.value, page: page.value, size: 3 });
  } finally {
    loading.value = false;
  }
}

function onSearch(value) {
  search.value = value;
  page.value = 0;
  loadActions(0);
}

async function handleSearch() {
  page.value = 0;
  await loadActions(0);
}
</script>
