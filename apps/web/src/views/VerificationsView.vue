<template>
  <div class="page-shell">
    <AppPageHeader
      title="Verification Reviews"
      description="Review client submissions that require manual approval or rejection."
    />

    <v-alert v-if="message" type="success" variant="tonal" class="mb-4">
      {{ message }}
    </v-alert>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

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
          <v-chip size="small" color="warning" variant="tonal">{{ item.status }}</v-chip>
        </template>
        <template #item.saIdValid="{ item }">{{ item.saIdValid ? "Valid" : "Invalid" }}</template>
        <template #item.ocrConfidence="{ item }">{{ item.ocrConfidence ?? "-" }}</template>
        <template #item.faceMatchScore="{ item }">{{ item.faceMatchScore ?? "-" }}</template>
        <template #item.reviewNotes="{ item }">
          <AppTruncateText :text="item.reviewNotes" :max-chars="90" max-width="280px" />
        </template>
        <template #item.actions="{ item }">
          <div class="d-flex ga-2 flex-wrap">
            <AppActionButton size="small" color="success" text="Approve" @click="openDialog(item, 'approve')" />
            <AppActionButton size="small" color="error" variant="tonal" text="Reject" @click="openDialog(item, 'reject')" />
          </div>
        </template>
      </AppDataTable>
    </AppTableCard>

    <AppDialogCard v-model="showDialog" :title="dialogTitle" :max-width="1200">
      <v-form @submit.prevent="submitReview">
        <v-row class="mb-2">
          <v-col cols="12" md="6">
            <v-card variant="tonal" class="pa-3 fill-height">
              <div class="text-subtitle-1 font-weight-medium mb-2">ID Copy PDF</div>
              <div class="text-body-2 text-medium-emphasis mb-3">
                {{ selectedVerification?.idDocumentName || "Client ID document" }}
              </div>
              <v-progress-linear v-if="loadingPreview" indeterminate color="primary" class="mb-3" />
              <iframe
                v-if="idDocumentUrl"
                :src="idDocumentUrl"
                title="Client ID document preview"
                style="width: 100%; min-height: 420px; border: 0; border-radius: 12px;"
              />
              <div v-else class="text-body-2 text-medium-emphasis">
                PDF preview is not available yet.
              </div>
              <div class="mt-3 d-flex ga-2">
                <v-btn
                  v-if="idDocumentUrl"
                  color="primary"
                  variant="tonal"
                  :href="idDocumentUrl"
                  target="_blank"
                >
                  Open PDF
                </v-btn>
                <AppActionButton
                  v-if="selectedVerification"
                  size="small"
                  variant="tonal"
                  text="Download ID (PDF)"
                  prepend-icon="mdi-file-download-outline"
                  :loading="downloadLoading === 'id'"
                  @click="downloadDoc('id')"
                />
              </div>
            </v-card>
          </v-col>
          <v-col cols="12" md="6">
            <v-card variant="tonal" class="pa-3 fill-height">
              <div class="text-subtitle-1 font-weight-medium mb-2">Selfie Capture</div>
              <div class="text-body-2 text-medium-emphasis mb-3">
                {{ selectedVerification?.selfieDocumentName || "Client selfie" }}
              </div>
              <v-progress-linear v-if="loadingPreview" indeterminate color="primary" class="mb-3" />
              <v-img
                v-if="selfieDocumentUrl"
                :src="selfieDocumentUrl"
                height="420"
                cover
                class="rounded-lg"
              />
              <div v-else class="text-body-2 text-medium-emphasis">
                Selfie preview is not available yet.
              </div>
              <div class="mt-3">
                <AppActionButton
                  v-if="selectedVerification && selfieDocumentUrl"
                  size="small"
                  variant="tonal"
                  text="Download selfie"
                  prepend-icon="mdi-image-download-outline"
                  :loading="downloadLoading === 'selfie'"
                  @click="downloadDoc('selfie')"
                />
              </div>
            </v-card>
          </v-col>
        </v-row>
        <AppTextField
          v-model="reviewNotes"
          label="Review notes"
          prepend-inner-icon="mdi-note-text-outline"
          variant="outlined"
        />
        <div class="d-flex ga-2">
          <AppActionButton :text="dialogAction === 'approve' ? 'Approve Verification' : 'Reject Verification'" type="submit" />
          <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="closeDialog" />
        </div>
      </v-form>
    </AppDialogCard>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppDataTable from "../components/ui/AppDataTable.vue";
import AppDialogCard from "../components/ui/AppDialogCard.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import AppTruncateText from "../components/ui/AppTruncateText.vue";
import { useAppStore } from "../store";

const store = useAppStore();
const error = ref("");
const message = ref("");
const showDialog = ref(false);
const dialogAction = ref("approve");
const selectedVerification = ref(null);
const reviewNotes = ref("");
const loadingPreview = ref(false);
const downloadLoading = ref(null);
const idDocumentUrl = ref("");
const selfieDocumentUrl = ref("");

const verifications = computed(() => store.verifications);
const dialogTitle = computed(() => (dialogAction.value === "approve" ? "Approve Verification" : "Reject Verification"));

const verificationHeaders = [
  { title: "Client", key: "borrowerId" },
  { title: "Status", key: "status" },
  { title: "SA ID", key: "saIdValid" },
  { title: "OCR", key: "ocrConfidence" },
  { title: "Face", key: "faceMatchScore" },
  { title: "Notes", key: "reviewNotes" },
  { title: "Actions", key: "actions" }
];

onMounted(async () => {
  await loadVerifications();
});

async function loadVerifications() {
  error.value = "";
  try {
    await store.fetchManualReviewVerifications();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load verification queue";
  }
}

async function openDialog(item, action) {
  selectedVerification.value = item;
  dialogAction.value = action;
  reviewNotes.value = item.reviewNotes || "";
  error.value = "";
  showDialog.value = true;
  await loadDocumentPreview(item.id);
}

async function submitReview() {
  if (!selectedVerification.value) {
    return;
  }
  error.value = "";
  message.value = "";
  try {
    if (dialogAction.value === "approve") {
      await store.approveVerification(selectedVerification.value.id, reviewNotes.value || "Approved by owner");
      message.value = "Verification approved.";
    } else {
      await store.rejectVerification(selectedVerification.value.id, reviewNotes.value || "Rejected by owner");
      message.value = "Verification rejected.";
    }
    closeDialog();
    await Promise.all([store.fetchManualReviewVerifications(), store.fetchActions()]);
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not update verification";
  }
}

onBeforeUnmount(() => {
  revokePreviewUrls();
});

async function loadDocumentPreview(verificationId) {
  loadingPreview.value = true;
  revokePreviewUrls();
  try {
    const [idDocument, selfieDocument] = await Promise.all([
      store.fetchVerificationDocument(verificationId, "id-document"),
      store.fetchVerificationDocument(verificationId, "selfie-document")
    ]);
    idDocumentUrl.value = URL.createObjectURL(new Blob([idDocument.blob], { type: idDocument.contentType }));
    selfieDocumentUrl.value = URL.createObjectURL(new Blob([selfieDocument.blob], { type: selfieDocument.contentType }));
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not load verification documents";
  } finally {
    loadingPreview.value = false;
  }
}

function revokePreviewUrls() {
  if (idDocumentUrl.value) {
    URL.revokeObjectURL(idDocumentUrl.value);
    idDocumentUrl.value = "";
  }
  if (selfieDocumentUrl.value) {
    URL.revokeObjectURL(selfieDocumentUrl.value);
    selfieDocumentUrl.value = "";
  }
}

async function downloadDoc(type) {
  if (!selectedVerification.value) return;
  const verificationId = selectedVerification.value.id;
  const endpoint = type === "id" ? "id-document" : "selfie-document";
  const name = type === "id" ? (selectedVerification.value.idDocumentName || "id-document.pdf") : (selectedVerification.value.selfieDocumentName || "selfie.png");
  downloadLoading.value = type;
  try {
    const { blob } = await store.fetchVerificationDocument(verificationId, endpoint);
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  } catch (requestError) {
    message.value = requestError.response?.data?.message || requestError.message || "Download failed.";
  } finally {
    downloadLoading.value = null;
  }
}

function closeDialog() {
  showDialog.value = false;
  selectedVerification.value = null;
  reviewNotes.value = "";
  revokePreviewUrls();
}
</script>
