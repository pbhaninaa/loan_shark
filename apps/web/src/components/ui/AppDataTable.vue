<template>
  <div class="app-data-table">
    <div class="d-flex flex-wrap align-center justify-space-between gap-2 mb-3">
      <v-card-title v-if="title" class="pa-0">{{ title }}</v-card-title>
      <div v-if="showSearch || $slots['header-actions']" class="d-flex align-center gap-2">
        <slot name="header-actions" />
        <v-text-field
          v-if="showSearch"
          v-model="searchInput"
          :placeholder="searchPlaceholder"
          density="compact"
          hide-details
          clearable
          variant="outlined"
          class="table-search"
          style="max-width: 280px; min-width: 200px;"
          prepend-inner-icon="mdi-magnify"
        />
      </div>
    </div>

    <div v-show="loading" class="text-center py-8">
      <v-progress-circular indeterminate color="primary" size="48" width="3" />
    </div>

    <v-data-table
      v-show="!loading"
      :headers="normalizedHeaders"
      :items="items"
      :items-length="computedItemsLength"
      :items-per-page="itemsPerPage"
      :items-per-page-options="itemsPerPageOptions"
      :loading="loading"
      :page="serverPage"
      class="elevation-1 table-truncate"
      @update:options="onUpdateOptions"
    >
      <template #item="{ item }">
        <tr>
          <td
            v-for="header in normalizedHeaders"
            :key="header.key"
            class="table-cell"
          >
            <slot
              :name="`item.${header.key}`"
              :item="item"
            >
              <AppTruncateText
                :text="getCellText(item, header)"
                :max-chars="50"
              />
            </slot>
          </td>
        </tr>
      </template>

      <template #no-data>
        <tr>
          <td :colspan="(normalizedHeaders?.length ?? 0) || 1" class="text-center text-medium-emphasis py-6">
            {{ noDataMessage }}
          </td>
        </tr>
      </template>

      <template v-if="$slots.footer" #bottom>
        <slot name="footer" />
      </template>
    </v-data-table>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import AppTruncateText from "./AppTruncateText.vue";

const getCellText = (item, header) => {
  const val = header.formatter ? header.formatter(item) : (item?.[header.key] ?? "-");
  return val != null ? String(val) : "-";
};

const props = defineProps({
  title: { type: String, required: true },
  headers: { type: Array, required: true },
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  itemsPerPage: { type: Number, default: 10 },
  itemsPerPageOptions: { type: Array, default: () => [5, 10, 15, 20, 25] },
  noDataMessage: { type: String, default: "No data." },
  showSearch: { type: Boolean, default: false },
  searchPlaceholder: { type: String, default: "Search..." },
  serverItemsLength: { type: Number, default: null },
  page: { type: Number, default: 0 }
});

const emit = defineEmits(["update:searchValue", "update:options", "update:page"]);

const normalizedHeaders = computed(() =>
  (props.headers || []).map((h) => ({
    ...h,
    title: h.title ?? h.text,
    key: h.key ?? h.value ?? h.title ?? h.text
  }))
);

const computedItemsLength = computed(() => {
  if (props.serverItemsLength != null) return props.serverItemsLength;
  return props.items.length;
});

const searchInput = ref("");
let debounceTimer = null;
watch(searchInput, (val) => {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    emit("update:searchValue", val ?? "");
    debounceTimer = null;
  }, 300);
});

const serverPage = computed(() => (props.serverItemsLength != null ? props.page + 1 : undefined));

function onUpdateOptions(opts) {
  if (props.serverItemsLength == null) return;
  const page = opts?.page != null ? opts.page - 1 : props.page;
  const itemsPerPage = opts?.itemsPerPage ?? props.itemsPerPage;
  emit("update:options", { page, itemsPerPage, sortBy: opts?.sortBy, sortOrder: opts?.sortOrder });
  emit("update:page", page);
}
</script>

<style scoped>
.table-cell {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
}
.table-search :deep(.v-field__input) {
  padding-top: 0;
  padding-bottom: 0;
}
</style>
