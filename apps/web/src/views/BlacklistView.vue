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
      <AppDataTable
        title=""
        :headers="blacklistHeaders"
        :items="blacklist"
        :loading="loading"
        show-search
        search-placeholder="Search blacklist"
        no-data-message="No blacklist entries."
        :items-per-page="8"
        @update:search-value="onSearch"
      >
        <template #item.id="{ item }">#{{ item.id }}</template>
        <template #item.borrowerId="{ item }">{{ borrowerName(item.borrowerId) }}</template>
        <template #item.reason="{ item }">
          <AppTruncateText :text="item.reason" :max-chars="90" max-width="280px" />
        </template>
        <template #item.blacklistedAt="{ item }">{{ formatDate(item.blacklistedAt) }}</template>
        <template #footer>
          <AppPaginationFooter v-model="page" :total-pages="blacklistPage.totalPages" :total-elements="blacklistPage.totalElements" @update:model-value="loadBlacklist" />
        </template>
      </AppDataTable>
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
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
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
const loading = ref(false);

const blacklistHeaders = [
  { title: "Entry", key: "id" },
  { title: "Client", key: "borrowerId" },
  { title: "Reason", key: "reason" },
  { title: "Date", key: "blacklistedAt" }
];
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
  loading.value = true;
  try {
    await store.fetchBlacklist({ q: search.value, page: page.value, size: 5});
  } finally {
    loading.value = false;
  }
}

function onSearch(value) {
  search.value = value;
  page.value = 0;
  loadBlacklist(0);
}

async function handleSearch() {
  page.value = 0;
  await loadBlacklist(0);
}
</script>
