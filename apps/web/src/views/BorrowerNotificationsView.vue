<template>
  <div class="page-shell">
    <AppPageHeader
      title="Notifications"
      description="Review system messages about approvals, repayments, and account activity."
    >
      <template #actions>
        <AppActionButton size="small" variant="tonal" color="secondary" prepend-icon="mdi-refresh" text="Refresh" @click="loadNotifications" />
      </template>
    </AppPageHeader>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <AppTableCard title="Message Inbox" :count-label="`${notifications.length} messages`" chip-color="info">
      <template #header-actions>
        <AppSearchField v-model="search" label="Search notifications" style="min-width: 240px;" @update:model-value="handleSearch" />
      </template>
      <v-list lines="two">
        <v-list-item
          v-for="notification in notifications"
          :key="notification.id"
          :title="notification.channel"
          @click="markAsRead(notification.id)"
        >
          <template #subtitle>
            <AppTruncateText :text="notification.message" :max-chars="90" fallback="" />
          </template>
          <template #append>
            <v-chip size="small" color="info" variant="tonal">{{ formatDate(notification.createdAt) }}</v-chip>
          </template>
        </v-list-item>
      </v-list>
      <div v-if="!notifications.length" class="text-medium-emphasis">No notifications yet.</div>
      <AppPaginationFooter v-model="page" :total-pages="notificationsPage.totalPages" :total-elements="notificationsPage.totalElements" @update:model-value="loadNotifications" />
    </AppTableCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSearchField from "../components/ui/AppSearchField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTruncateText from "../components/ui/AppTruncateText.vue";
import { useAppStore } from "../store";
import { formatDate } from "../utils/formatters";

const store = useAppStore();
const error = ref("");
const search = ref("");
const page = ref(0);
const notifications = computed(() => store.notifications);
const notificationsPage = computed(() => store.notificationsPage);

onMounted(async () => {
  await loadNotifications();
});

async function loadNotifications(nextPage = page.value) {
  error.value = "";
  page.value = nextPage;
  try {
    await store.fetchMyNotifications({ q: search.value, page: page.value, size: 8 });
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load notifications";
  }
}

async function markAsRead(id) {
  try {
    await store.markNotificationRead(id);
    await loadNotifications(page.value);
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not update notification";
  }
}

async function handleSearch() {
  page.value = 0;
  await loadNotifications(0);
}
</script>
