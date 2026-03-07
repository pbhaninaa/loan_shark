<template>
  <div class="page-shell">
    <AppPageHeader
      title="Blacklist Control"
      description="Block risky clients from future lending and keep a clear record of why they were restricted."
    >
      <template #actions>
        <AppActionButton text="Add Entry" color="error" prepend-icon="mdi-account-cancel-outline" @click="showBlacklistDialog = true" />
      </template>
    </AppPageHeader>

    <AppTableCard title="Blacklist Register" :count-label="`${blacklist.length} entries`" chip-color="error">
      <template #header-actions>
        <AppSearchField v-model="search" label="Search blacklist" style="min-width: 240px;" @update:model-value="handleSearch" />
      </template>
        <v-table>
          <thead>
            <tr>
              <th>Entry</th>
              <th>Client</th>
              <th>Reason</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in blacklist" :key="entry.id">
              <td>#{{ entry.id }}</td>
              <td>{{ borrowerName(entry.borrowerId) }}</td>
              <td>
              <AppTruncateText :text="entry.reason" :max-chars="90" max-width="280px" />
            </td>
              <td>{{ formatDate(entry.blacklistedAt) }}</td>
            </tr>
          </tbody>
        </v-table>
      <AppPaginationFooter v-model="page" :total-pages="blacklistPage.totalPages" :total-elements="blacklistPage.totalElements" @update:model-value="loadBlacklist" />
    </AppTableCard>

    <AppDialogCard v-model="showBlacklistDialog" title="Add Borrower To Blacklist" :max-width="520">
      <v-form @submit.prevent="blacklistBorrower">
            <AppSelectField
              v-model="form.borrowerId"
              label="Client"
              prepend-inner-icon="mdi-account-cancel-outline"
              :items="borrowerOptions"
              item-title="title"
              item-value="value"
            />
            <v-textarea v-model="form.reason" label="Reason" rows="4" prepend-inner-icon="mdi-alert-circle-outline" />
            <div class="d-flex ga-2">
              <AppActionButton text="Add To Blacklist" type="submit" color="error" prepend-icon="mdi-shield-alert-outline" class="flex-1-1" />
              <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="showBlacklistDialog = false" />
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
import AppTruncateText from "../components/ui/AppTruncateText.vue";
import api from "../services/api";
import { useAppStore } from "../store";

const store = useAppStore();
const blacklist = computed(() => store.blacklist);
const blacklistPage = computed(() => store.blacklistPage);
const borrowers = computed(() => store.borrowers);
const showBlacklistDialog = ref(false);
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
  reason: "Defaulted loan"
});

onMounted(async () => {
  await Promise.all([loadBlacklist(), store.fetchBorrowers({ page: 0, size: 100 })]);
  if (!form.borrowerId && borrowerOptions.value.length) {
    form.borrowerId = borrowerOptions.value[0].value;
  }
});

async function blacklistBorrower() {
  await api.post("/blacklist", form);
  showBlacklistDialog.value = false;
  await loadBlacklist();
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString() : "-";
}

function borrowerName(borrowerId) {
  const borrower = borrowers.value.find((item) => item.id === borrowerId);
  return borrower ? `${borrower.firstName} ${borrower.lastName}` : `Client #${borrowerId}`;
}

async function loadBlacklist(nextPage = page.value) {
  page.value = nextPage;
  await store.fetchBlacklist({ q: search.value, page: page.value, size: 8 });
}

async function handleSearch() {
  page.value = 0;
  await loadBlacklist(0);
}
</script>
