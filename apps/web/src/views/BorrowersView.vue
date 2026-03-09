<template>
  <div class="page-shell">
    <AppPageHeader
      title="Client Management"
      description="Onboard customers with identity, income, and contact details ready for loan assessment."
    >
      <template #actions>
        <AppActionButton text="Create Client" prepend-icon="mdi-account-plus-outline" @click="showCreateDialog = true" />
      </template>
    </AppPageHeader>

    <v-alert v-if="message" type="success" variant="tonal" class="mb-4">
      {{ message }}
    </v-alert>

    <AppTableCard title="Client Portfolio" :count-label="`${borrowers.length} records`">
      <AppDataTable
        title=""
        :headers="borrowerHeaders"
        :items="borrowers"
        :loading="loading"
        show-search
        search-placeholder="Search clients"
        no-data-message="No clients."
        :items-per-page="8"
        @update:search-value="onSearch"
      >
        <template #item.name="{ item }">{{ item.firstName }} {{ item.lastName }}</template>
        <template #item.phone="{ item }">{{ item.phone }}</template>
        <template #item.status="{ item }">
          <template v-if="store.isOwner">
            <AppSelectField
              :model-value="item.status"
              :items="statusOptions"
              density="compact"
              hide-details
              style="min-width: 160px;"
              @update:model-value="updateBorrowerStatus(item.id, $event)"
            />
          </template>
          <template v-else>
            <v-chip :color="item.status === 'BLACKLISTED' ? 'error' : 'success'" size="small" variant="tonal">{{ item.status }}</v-chip>
          </template>
        </template>
        <template #item.riskScore="{ item }">
          <v-chip color="info" size="small" variant="tonal">{{ item.riskScore }}</v-chip>
        </template>
        <template #item.monthlyIncome="{ item }">{{ formatCurrency(item.monthlyIncome) }}</template>
        <template #item.actions="{ item }">
          <AppActionButton size="small" variant="tonal" text="View profile" prepend-icon="mdi-account-eye-outline" @click="openProfileDialog(item.id)" />
          <template v-if="store.isOwner">
            <AppActionButton size="small" variant="tonal" text="Edit" prepend-icon="mdi-pencil-outline" @click="openEditDialog(item)" />
            <AppActionButton size="small" color="error" variant="tonal" text="Delete" prepend-icon="mdi-delete-outline" :loading="deletingId === item.id" @click="confirmDeleteClient(item)" />
          </template>
        </template>
        <template #footer>
          <AppPaginationFooter v-model="page" :total-pages="borrowersPage.totalPages" :total-elements="borrowersPage.totalElements" @update:model-value="loadBorrowers" />
        </template>
      </AppDataTable>
    </AppTableCard>

    <AppDialogCard v-model="showProfileDialog" title="Client Profile" :max-width="560">
      <v-progress-linear v-if="profileLoading" indeterminate color="primary" class="mb-4" />
      <template v-else-if="selectedProfile">
        <div class="text-h6">{{ selectedProfile.firstName }} {{ selectedProfile.lastName }}</div>
        <div class="text-body-2 text-medium-emphasis mb-3">Client #{{ selectedProfile.id }}</div>
        <v-list density="comfortable" class="py-0">
          <v-list-item title="ID number" :subtitle="selectedProfile.idNumber || 'None'" />
          <v-list-item title="Phone" :subtitle="selectedProfile.phone || 'None'" />
          <v-list-item title="Email" :subtitle="selectedProfile.email || 'None'" />
          <v-list-item title="Address" :subtitle="selectedProfile.address || 'None'" />
          <v-list-item title="Employment status" :subtitle="selectedProfile.employmentStatus || 'None'" />
          <v-list-item title="Monthly income" :subtitle="formatCurrency(selectedProfile.monthlyIncome)" />
          <v-list-item title="Employer" :subtitle="selectedProfile.employerName || 'None'" />
        </v-list>
        <div class="d-flex ga-2 flex-wrap mt-3">
          <v-chip :color="selectedProfile.status === 'BLACKLISTED' ? 'error' : 'success'" variant="tonal">
            {{ selectedProfile.status }}
          </v-chip>
          <v-chip color="info" variant="tonal">Risk score: {{ selectedProfile.riskScore ?? 'N/A' }}</v-chip>
        </div>
        <template v-if="store.isOwner && borrowerVerification">
          <v-divider class="my-3" />
          <div class="text-subtitle-2 text-medium-emphasis mb-2">KYC documents</div>
          <div class="d-flex ga-2 flex-wrap">
            <AppActionButton
              size="small"
              variant="tonal"
              text="Download ID (PDF)"
              prepend-icon="mdi-file-download-outline"
              :loading="downloadLoading === 'id'"
              @click="downloadDocument(borrowerVerification.id, 'id', borrowerVerification.idDocumentName)"
            />
            <AppActionButton
              size="small"
              variant="tonal"
              text="Download selfie"
              prepend-icon="mdi-image-download-outline"
              :loading="downloadLoading === 'selfie'"
              @click="downloadDocument(borrowerVerification.id, 'selfie', borrowerVerification.selfieDocumentName)"
            />
          </div>
        </template>
        <v-alert v-else-if="store.isOwner && selectedProfile && !profileLoading && !borrowerVerification" type="info" variant="tonal" density="compact" class="mt-3">
          No KYC documents on file for this client.
        </v-alert>
      </template>
      <template v-else-if="profileError">
        <v-alert type="error" variant="tonal">{{ profileError }}</v-alert>
      </template>
    </AppDialogCard>

    <AppDialogCard v-model="showEditDialog" title="Edit client" :max-width="640">
      <v-alert v-if="editError" type="error" variant="tonal" density="compact" class="mb-3">
        {{ editError }}
      </v-alert>
      <v-form v-if="editingBorrower" @submit.prevent="saveEditBorrower">
        <v-row>
          <v-col cols="12" md="6">
            <AppTextField v-model="editForm.firstName" label="First name" prepend-inner-icon="mdi-account-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model="editForm.lastName" label="Last name" prepend-inner-icon="mdi-account-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model="editForm.idNumber" label="ID number" prepend-inner-icon="mdi-card-account-details-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model="editForm.phone" label="Phone" prepend-inner-icon="mdi-phone-outline" required />
          </v-col>
          <v-col cols="12">
            <AppTextField v-model="editForm.email" label="Email" prepend-inner-icon="mdi-email-outline" />
          </v-col>
          <v-col cols="12">
            <AppTextField v-model="editForm.address" label="Address" prepend-inner-icon="mdi-map-marker-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppSelectField
              v-model="editForm.employmentStatus"
              label="Employment type"
              :items="employmentTypeOptions"
              required
            />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model.number="editForm.monthlyIncome" label="Monthly income" type="number" prepend-inner-icon="mdi-cash" required />
          </v-col>
          <v-col cols="12">
            <AppTextField v-model="editForm.employerName" label="Employer name" prepend-inner-icon="mdi-domain" />
          </v-col>
        </v-row>
        <div class="d-flex ga-2">
          <AppActionButton text="Update client" type="submit" :loading="editLoading" />
          <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="showEditDialog = false" />
        </div>
      </v-form>
    </AppDialogCard>

    <v-dialog v-model="showDeleteConfirm" max-width="440" persistent>
      <v-card>
        <v-card-title>Delete client</v-card-title>
        <v-card-text>
          <template v-if="deletingBorrower">
            Remove <strong>{{ deletingBorrower.firstName }} {{ deletingBorrower.lastName }}</strong>? This also removes their login account. Cannot be undone. Clients with loan history cannot be deleted.
          </template>
        </v-card-text>
        <v-card-actions>
          <v-spacer />
          <v-btn variant="text" @click="showDeleteConfirm = false">Cancel</v-btn>
          <v-btn color="error" variant="flat" :loading="deleteLoading" @click="doDeleteBorrower">Delete client</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <AppDialogCard v-model="showCreateDialog" title="Create Client Profile" :max-width="720">
      <v-form @submit.prevent="createBorrower">
        <v-alert type="info" variant="tonal" class="mb-4">
          Upload the client's ID (PDF) and photo. The client will be created and must go through verification before they can use the system. You can upload files from your PC since the client may not be present.
        </v-alert>
        <v-row>
          <v-col cols="12" md="6">
            <AppTextField v-model="form.username" label="Client username" prepend-inner-icon="mdi-account-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model="form.password" label="Client password" type="password" prepend-inner-icon="mdi-lock-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model="form.firstName" label="First name" prepend-inner-icon="mdi-account-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model="form.lastName" label="Last name" prepend-inner-icon="mdi-account-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model="form.idNumber" label="ID number" prepend-inner-icon="mdi-card-account-details-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model="form.phone" label="Phone" prepend-inner-icon="mdi-phone-outline" required />
          </v-col>
          <v-col cols="12">
            <AppTextField v-model="form.email" label="Email" prepend-inner-icon="mdi-email-outline" />
          </v-col>
          <v-col cols="12">
            <AppTextField v-model="form.address" label="Address" prepend-inner-icon="mdi-map-marker-outline" required />
          </v-col>
          <v-col cols="12" md="6">
            <AppSelectField
              v-model="form.employmentStatus"
              label="Employment type"
              prepend-inner-icon="mdi-briefcase-outline"
              :items="employmentTypeOptions"
              required
            />
          </v-col>
          <v-col cols="12" md="6">
            <AppTextField v-model.number="form.monthlyIncome" label="Monthly income" type="number" prepend-inner-icon="mdi-cash" required />
          </v-col>
          <v-col cols="12">
            <AppTextField v-model="form.employerName" label="Employer name" prepend-inner-icon="mdi-domain" />
          </v-col>
          <v-col cols="12" md="6">
            <v-file-input
              v-model="form.idDocument"
              label="ID document (PDF)"
              accept="application/pdf,.pdf"
              prepend-inner-icon="mdi-file-pdf-box"
              density="compact"
              show-size
              clearable
              required
            />
          </v-col>
          <v-col cols="12" md="6">
            <v-file-input
              v-model="form.selfieImage"
              label="Client photo (image)"
              accept="image/jpeg,image/jpg,image/png"
              prepend-inner-icon="mdi-camera"
              density="compact"
              show-size
              clearable
              required
            />
          </v-col>
        </v-row>

        <div class="d-flex ga-2">
          <AppActionButton text="Save Client" type="submit" prepend-icon="mdi-content-save-outline" class="flex-1-1" :loading="createLoading" />
          <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="closeCreateDialog" />
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
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import api from "../services/api";
import { useAppStore } from "../store";
import { formatCurrency } from "../utils/formatters";

const store = useAppStore();
const message = ref("");
const statusOptions = ["ACTIVE", "BLACKLISTED"];
const showCreateDialog = ref(false);
const showEditDialog = ref(false);
const editingBorrower = ref(null);
const editForm = reactive({
  firstName: "",
  lastName: "",
  idNumber: "",
  phone: "",
  email: "",
  address: "",
  employmentStatus: "",
  monthlyIncome: 0,
  employerName: ""
});
const editError = ref("");
const editLoading = ref(false);
const showDeleteConfirm = ref(false);
const deletingBorrower = ref(null);
const deletingId = ref(null);
const deleteLoading = ref(false);
const showProfileDialog = ref(false);
const selectedProfile = ref(null);
const borrowerVerification = ref(null);
const profileLoading = ref(false);
const profileError = ref("");
const downloadLoading = ref(null);
const search = ref("");
const page = ref(0);
const loading = ref(false);

const borrowerHeaders = [
  { title: "Name", key: "name" },
  { title: "Phone", key: "phone" },
  { title: "Status", key: "status" },
  { title: "Risk", key: "riskScore" },
  { title: "Income", key: "monthlyIncome" },
  { title: "Actions", key: "actions" }
];

const employmentTypeOptions = [
  "Employed",
  "Self-employed",
  "Unemployed",
  "Student",
  "Part-time",
  "Contract",
  "Freelance",
  "Pensioner",
  "Other"
];

const initialForm = () => ({
  username: "",
  password: "",
  firstName: "",
  lastName: "",
  idNumber: "",
  phone: "",
  email: "",
  address: "",
  employmentStatus: "",
  monthlyIncome: 0,
  employerName: "",
  idDocument: null,
  selfieImage: null
});

const form = reactive(initialForm());
const createLoading = ref(false);
const borrowers = computed(() => store.borrowers);
const borrowersPage = computed(() => store.borrowersPage);

onMounted(async () => {
  await loadBorrowers();
});

function openEditDialog(borrower) {
  editingBorrower.value = borrower;
  editForm.firstName = borrower.firstName ?? "";
  editForm.lastName = borrower.lastName ?? "";
  editForm.idNumber = borrower.idNumber ?? "";
  editForm.phone = borrower.phone ?? "";
  editForm.email = borrower.email ?? "";
  editForm.address = borrower.address ?? "";
  editForm.employmentStatus = borrower.employmentStatus ?? "";
  editForm.monthlyIncome = Number(borrower.monthlyIncome) || 0;
  editForm.employerName = borrower.employerName ?? "";
  editError.value = "";
  showEditDialog.value = true;
}

async function saveEditBorrower() {
  if (!editingBorrower.value) return;
  editError.value = "";
  editLoading.value = true;
  try {
    await store.updateBorrower(editingBorrower.value.id, {
      firstName: editForm.firstName,
      lastName: editForm.lastName,
      idNumber: editForm.idNumber,
      phone: editForm.phone,
      email: editForm.email || undefined,
      address: editForm.address,
      employmentStatus: editForm.employmentStatus,
      monthlyIncome: editForm.monthlyIncome,
      employerName: editForm.employerName || undefined
    });
    showEditDialog.value = false;
    editingBorrower.value = null;
    message.value = "Client updated.";
    await loadBorrowers();
  } catch (e) {
    editError.value = e.response?.data?.message || e.message || "Failed to update client.";
  } finally {
    editLoading.value = false;
  }
}

function confirmDeleteClient(borrower) {
  deletingBorrower.value = borrower;
  showDeleteConfirm.value = true;
}

async function doDeleteBorrower() {
  if (!deletingBorrower.value) return;
  deletingId.value = deletingBorrower.value.id;
  deleteLoading.value = true;
  try {
    await store.deleteBorrower(deletingBorrower.value.id);
    showDeleteConfirm.value = false;
    deletingBorrower.value = null;
    message.value = "Client deleted.";
    await loadBorrowers();
  } catch (e) {
    message.value = e.response?.data?.message || e.message || "Failed to delete client.";
  } finally {
    deleteLoading.value = false;
    deletingId.value = null;
  }
}

async function createBorrower() {
  const idFile = Array.isArray(form.idDocument) ? form.idDocument?.[0] : form.idDocument;
  const selfieFile = Array.isArray(form.selfieImage) ? form.selfieImage?.[0] : form.selfieImage;
  if (!idFile || !selfieFile) {
    message.value = "";
    return;
  }
  const fd = new FormData();
  fd.append("username", form.username);
  fd.append("password", form.password);
  fd.append("firstName", form.firstName);
  fd.append("lastName", form.lastName);
  fd.append("idNumber", form.idNumber);
  fd.append("phone", form.phone);
  if (form.email) fd.append("email", form.email);
  fd.append("address", form.address);
  fd.append("employmentStatus", form.employmentStatus);
  fd.append("monthlyIncome", String(form.monthlyIncome ?? 0));
  if (form.employerName) fd.append("employerName", form.employerName);
  fd.append("idDocument", idFile);
  fd.append("selfieImage", selfieFile);
  createLoading.value = true;
  try {
    await api.post("/borrowers/with-documents", fd, {
      headers: { "Content-Type": "multipart/form-data" }
    });
    Object.assign(form, initialForm());
    showCreateDialog.value = false;
    message.value = "Client created. They must complete verification (owner review) before using the system.";
    await loadBorrowers();
  } catch (e) {
    message.value = e.response?.data?.message || e.message || "Failed to create client.";
  } finally {
    createLoading.value = false;
  }
}

async function updateBorrowerStatus(id, status) {
  await api.put(`/borrowers/${id}/status`, { status });
  message.value = `Client status updated to ${status}.`;
  await loadBorrowers();
}

function closeCreateDialog() {
  showCreateDialog.value = false;
  Object.assign(form, initialForm());
}

async function openProfileDialog(borrowerId) {
  showProfileDialog.value = true;
  selectedProfile.value = null;
  borrowerVerification.value = null;
  profileError.value = "";
  profileLoading.value = true;
  try {
    selectedProfile.value = await store.fetchBorrowerById(borrowerId);
    if (store.isOwner) {
      try {
        borrowerVerification.value = await store.fetchVerificationByBorrowerId(borrowerId);
      } catch {
        borrowerVerification.value = null;
      }
    }
  } catch (e) {
    profileError.value = e.response?.data?.message || e.message || "Failed to load client profile.";
  } finally {
    profileLoading.value = false;
  }
}

async function downloadDocument(verificationId, type, suggestedName) {
  const endpoint = type === "id" ? "id-document" : "selfie-document";
  const fallbackName = type === "id" ? "id-document.pdf" : "selfie.png";
  downloadLoading.value = type;
  try {
    const { data, headers } = await api.get(`/verifications/${verificationId}/${endpoint}`, { responseType: "blob" });
    const name = suggestedName || fallbackName;
    const url = URL.createObjectURL(data);
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  } catch (e) {
    message.value = e.response?.data?.message || e.message || "Download failed.";
  } finally {
    downloadLoading.value = null;
  }
}

async function loadBorrowers(nextPage = page.value) {
  page.value = nextPage;
  loading.value = true;
  try {
    await store.fetchBorrowers({ q: search.value, page: page.value, size: 8 });
  } finally {
    loading.value = false;
  }
}

function onSearch(value) {
  search.value = value;
  page.value = 0;
  loadBorrowers(0);
}

async function handleSearch() {
  page.value = 0;
  await loadBorrowers(0);
}
</script>
