<template>
  <div class="page-shell">
    <!-- Page Header -->
    <AppPageHeader
      title="Verification Reviews"
      description="Review client submissions that require manual approval or rejection."
    />

    <!-- Success & Error Alerts -->
    <v-alert v-if="message" type="success" variant="tonal" class="mb-4">
      {{ message }}
    </v-alert>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <!-- Manual Review Table -->
    <AppTableCard title="Manual Review Queue" :count-label="`${verifications.length} cases`" chip-color="warning">
      <AppDataTable
        title=""
        :headers="verificationHeaders"
        :items="verifications"
        :items-per-page="3"
        no-data-message="No client verifications need manual review."
      >
        <template #item.borrowerId="{ item }">#{{ item.borrowerId }}</template>

        <template #item.status="{ item }">
          <v-chip size="small" color="warning" variant="tonal">
            {{ item.status }}
          </v-chip>
        </template>

        <template #item.saIdValid="{ item }">
          {{ item.saIdValid ? "Valid" : "Invalid" }}
        </template>

        <template #item.ocrConfidence="{ item }">
          {{ item.ocrConfidence ?? "-" }}
        </template>

        <template #item.faceMatchScore="{ item }">
          {{ item.faceMatchScore ?? "-" }}
        </template>

        <template #item.reviewNotes="{ item }">
          <AppTruncateText :text="item.reviewNotes" :max-chars="90" max-width="280px" />
        </template>

        <template #item.actions="{ item }">
          <div class="d-flex ga-2 flex-wrap">
            <AppActionButton
              size="small"
              color="success"
              text="Approve"
              @click="openDialog(item, 'approve')"
            />
            <AppActionButton
              size="small"
              color="error"
              variant="tonal"
              text="Reject"
              @click="openDialog(item, 'reject')"
            />
          </div>
        </template>
      </AppDataTable>
    </AppTableCard>

    <!-- Review Dialog -->
    <AppDialogCard v-model="showDialog" :title="dialogTitle" :max-width="1200">
      <v-form @submit.prevent="submitReview">
        <v-row class="mb-2">
         

          <!-- SELFIE DOCUMENT -->
          <v-col cols="12" md="6">
            <v-card variant="tonal" class="pa-3 fill-height">
              <div class="text-subtitle-1 font-weight-medium mb-2">Selfie Capture</div>
              <v-progress-linear v-if="loadingPreview" indeterminate color="primary" class="mb-3" />
              <v-img
                v-if="selectedVerification?.selfieDocumentPreviewUrl"
                :src="selectedVerification.selfieDocumentPreviewUrl"
                height="420"
                cover
                class="rounded-lg"
              ></v-img>
              <div v-else class="text-body-2 text-medium-emphasis">
                Selfie preview is not available yet.
              </div>
            </v-card>
          </v-col> <!-- ID DOCUMENT -->
          <v-col cols="12" md="6">
            <v-card variant="tonal" class="pa-3 fill-height">
              <div class="text-subtitle-1 font-weight-medium mb-2">ID Copy PDF</div>           

              <v-progress-linear v-if="loadingPreview" indeterminate color="primary" class="mb-3" />

              <iframe
                v-if="selectedVerification?.idDocumentPreviewUrl"
                :src="selectedVerification.idDocumentPreviewUrl"
                style="width:100%;min-height:420px;border:0;border-radius:12px;"
              ></iframe>

              <div v-else class="text-body-2 text-medium-emphasis">
                PDF preview is not available yet.
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- Review Notes -->
        <AppTextField
          v-model="reviewNotes"
          label="Review notes"
          prepend-inner-icon="mdi-note-text-outline"
          variant="outlined"
        />

        <!-- Actions -->
        <div class="d-flex ga-2">
          <AppActionButton
            :text="dialogAction === 'approve' ? 'Approve Verification' : 'Reject Verification'"
            type="submit"
          />
          <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="closeDialog" />
        </div>
      </v-form>
    </AppDialogCard>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
import { useAppStore } from "../store";

import AppActionButton from "../components/ui/AppActionButton.vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppDialogCard from "../components/ui/AppDialogCard.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import AppTruncateText from "../components/ui/AppTruncateText.vue";

const store = useAppStore();

const error = ref("");
const message = ref("");

const showDialog = ref(false);
const dialogAction = ref("approve");
const selectedVerification = ref(null);
const reviewNotes = ref("");

const loadingPreview = ref(false);

const verifications = computed(() => store.verifications);

const dialogTitle = computed(() =>
  dialogAction.value === "approve" ? "Approve Verification" : "Reject Verification"
);

const verificationHeaders = [
  { title: "Client", key: "borrowerId" },
  { title: "Status", key: "status" },
  { title: "SA ID", key: "saIdValid" },
  { title: "OCR", key: "ocrConfidence" },
  { title: "Face", key: "faceMatchScore" },
  { title: "Notes", key: "reviewNotes" },
  { title: "Actions", key: "actions",   sortable: false },
];

onMounted(async () => {
  await loadVerifications();
});

// Load verifications from store
async function loadVerifications() {
  try {
    await store.fetchManualReviewVerifications();
  } catch (err) {
    error.value = "Could not load verification queue";
  }
}

// Open dialog for approve/reject
function openDialog(item, action) {
  selectedVerification.value = item;
  dialogAction.value = action;
  reviewNotes.value = item.reviewNotes || "";
  showDialog.value = true;

  // Fetch secure documents with JWT
  loadIdDocument();
  loadSelfieDocument();
}

// Close dialog
function closeDialog() {
  showDialog.value = false;
  selectedVerification.value = null;
  reviewNotes.value = "";
}

// Fetch ID PDF securely
async function loadIdDocument() {
  if (!selectedVerification.value?.idDocumentUrl) return;

  loadingPreview.value = true;
  try {
    selectedVerification.value.idDocumentPreviewUrl =
      await store.loadDocument(selectedVerification.value.idDocumentUrl);
  } catch (err) {
    console.error(err);
    error.value = "Failed to load ID PDF";
  } finally {
    loadingPreview.value = false;
  }
}

// Fetch selfie image securely
async function loadSelfieDocument() {
  if (!selectedVerification.value?.selfieDocumentUrl) return;

  loadingPreview.value = true;
  try {
    selectedVerification.value.selfieDocumentPreviewUrl =
      await store.loadDocument(selectedVerification.value.selfieDocumentUrl);
  } catch (err) {
    console.error(err);
    error.value = "Failed to load selfie image";
  } finally {
    loadingPreview.value = false;
  }
}

// Submit review
async function submitReview() {
  if (!selectedVerification.value) return;

  try {
    if (dialogAction.value === "approve") {
      await store.approveVerification(selectedVerification.value.id, reviewNotes.value);
      message.value = "Verification approved.";
    } else {
      await store.rejectVerification(selectedVerification.value.id, reviewNotes.value);
      message.value = "Verification rejected.";
    }

    closeDialog();
    await loadVerifications();
  } catch (err) {
    error.value = "Could not update verification";
  }
}
</script>