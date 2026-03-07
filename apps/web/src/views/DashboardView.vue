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
          <template #header-actions>
            <AppSearchField v-model="search" label="Search actions" style="min-width: 240px;" @update:model-value="handleSearch" />
          </template>
          <v-table>
            <thead>
              <tr>
                <th>Category</th>
                <th>Action</th>
                <th>Reference</th>
                <th>Amount</th>
                <th>Performed By</th>
                <th>Authorized By</th>
                <th>Details</th>
                <th>Time</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in actions" :key="`${item.category}-${item.entityId}-${item.timestamp}`">
                <td>
                  <v-chip :color="item.category === 'TRANSACTION' ? 'success' : 'info'" size="small" variant="tonal">
                    {{ item.category }}
                  </v-chip>
                </td>
                <td>{{ item.action }}</td>
                <td>{{ item.referenceNumber || (item.loanId ? `Loan #${item.loanId}` : item.entity) }}</td>
                <td>{{ item.amount ? formatCurrency(item.amount) : "-" }}</td>
                <td>{{ item.performedBy || "-" }}</td>
                <td>{{ item.authorizedBy || "-" }}</td>
                <td>
                <AppTruncateText :text="item.details" :max-chars="90" max-width="280px" />
              </td>
                <td>{{ formatDateTime(item.timestamp) }}</td>
              </tr>
            </tbody>
          </v-table>
          <AppPaginationFooter v-model="page" :total-pages="actionsPage.totalPages" :total-elements="actionsPage.totalElements" @update:model-value="loadActions" />
        </AppTableCard>
      </v-col>
    </v-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSearchField from "../components/ui/AppSearchField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTruncateText from "../components/ui/AppTruncateText.vue";
import { useAppStore } from "../store";
import { formatCurrency, formatDateTime } from "../utils/formatters";

const store = useAppStore();
const search = ref("");
const page = ref(0);

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
  if (store.isOwner) {
    await Promise.all([store.fetchDashboard(), loadActions()]);
  }
});

async function loadActions(nextPage = page.value) {
  page.value = nextPage;
  await store.fetchActions({ q: search.value, page: page.value, size: 3 });
}

async function handleSearch() {
  page.value = 0;
  await loadActions(0);
}
</script>
