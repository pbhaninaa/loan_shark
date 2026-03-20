<template>
  <div class="page-shell">
    <AppPageHeader
      title="My account"
      description="Your login details and email. You must add your email before you can use the system; it is also used to send password reset links."
    />

    <!-- Email warning -->
    <v-alert v-if="me && !me.email?.trim()" type="warning" variant="tonal" class="mb-4" prominent>
      You must add and save your email below before you can access the rest of the system.
    </v-alert>

    <!-- Success messages -->
    <v-alert v-if="successMessage" type="success" variant="tonal" class="mb-4">
      {{ successMessage }}
    </v-alert>

    <!-- Error messages -->
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <!-- Account info -->
    <v-card>
      <v-card-title class="d-flex align-center">
        <v-icon start>mdi-account-outline</v-icon>
        Account
      </v-card-title>
      <v-divider />
      <v-card-text v-if="me">
        <v-list density="comfortable">
          <v-list-item title="Username" :subtitle="me.username" />
          <v-list-item title="Role" :subtitle="me.role" />
        </v-list>
        <v-form @submit.prevent="saveEmail" class="mt-4">
          <AppTextField
            v-model="emailInput"
            label="Email"
            type="email"
            prepend-inner-icon="mdi-email-outline"
            hint="Required for password reset. Add or update your email to receive reset links."
          />
          <div class="mt-3">
            <AppActionButton text="Save email" type="submit" :loading="saving" />
          </div>
        </v-form>
      </v-card-text>
      <v-progress-linear v-else-if="loading" indeterminate color="primary" class="my-4" />
      <v-alert v-else type="warning" variant="tonal">
        Could not load account details.
      </v-alert>
    </v-card>

    <!-- Business contact details -->
    <v-card v-if="store.isOwner" class="mt-4">
      <v-card-title class="d-flex align-center">
        <v-icon start>mdi-domain</v-icon>
        Business Details
      </v-card-title>
      <v-divider />
      <v-card-text>
        <p class="text-body-2 text-medium-emphasis mb-4">
          These details are displayed to borrowers on the Help page so they can contact you with questions about their loans.
        </p>
        <v-form @submit.prevent="saveBusinessContact">
          <v-row>
            <v-col cols="12" md="6">
              <AppTextField
                v-model="businessContactForm.businessName"
                label="Business name"
                required
                prepend-inner-icon="mdi-domain"
              />
            </v-col>
            <v-col cols="12" md="6">
              <AppTextField
                v-model="businessContactForm.phone"
                label="Phone" required
                prepend-inner-icon="mdi-phone-outline"
              />
            </v-col>
            <v-col cols="12" md="6">
              <AppTextField
                v-model="businessContactForm.email"
                label="Email" required
                type="email"
                prepend-inner-icon="mdi-email-outline"
              />
            </v-col>
            <v-col cols="12" md="6">
              <AppTextField
                v-model="businessContactForm.address"
                label="Address" required
                prepend-inner-icon="mdi-map-marker-outline"
              />
            </v-col>
           
             <v-col cols="12" md="6">
    <AppTextField
      v-model="businessContactForm.bankName"
      label="Bank Name" required
      prepend-inner-icon="mdi-bank-outline"
    />
  </v-col>

  <v-col cols="12" md="6">
    <AppTextField
      v-model="businessContactForm.accountHolderName"
      label="Account Holder Name" required
      prepend-inner-icon="mdi-account-outline"
    />
  </v-col>

  <v-col cols="12" md="6">
    <AppTextField
      v-model="businessContactForm.accountNumber"
      label="Account Number" required
      prepend-inner-icon="mdi-bank-outline"
    />
  </v-col>

  <v-col cols="12" md="6">
    <AppTextField
      v-model="businessContactForm.accountType"
      label="Account Type (Savings/Cheque)" required
      prepend-inner-icon="mdi-credit-card-outline"
    />
  </v-col>

  <v-col cols="12" md="6">
    <AppTextField
      v-model="businessContactForm.branchCode"
      label="Branch Code" required
      prepend-inner-icon="mdi-source-branch"
    />
  </v-col>

  <v-col cols="12" md=""6>
    <AppTextField
      v-model="businessContactForm.paymentReference"
      label="Payment Reference Instruction"
      prepend-inner-icon="mdi-information-outline" required
      hint="E.g. Use your ID number or Loan ID as reference"
    />
  </v-col>
            <v-col cols="12" md="6">
             
            </v-col>
          </v-row>
          <div class="mt-3">
            <AppActionButton text="Save business contact" type="submit" :loading="savingContact" prepend-icon="mdi-content-save" />
          </div>
        </v-form>
      </v-card-text>
    </v-card>

    <!-- Reset history (owner only) -->
    <v-card v-if="store.isOwner" class="mt-4">
      <v-card-title class="d-flex align-center text-error">
        <v-icon start>mdi-database-refresh</v-icon>
        Reset database (owner only)
      </v-card-title>
      <v-divider />
      <v-card-text>
        <p class="text-body-2 text-medium-emphasis mb-3">
          Remove all history (loans, repayments, blacklist, notifications, audit log, etc.). Users, clients and their profiles are kept. Business capital is set to zero.
        </p>
        <v-dialog v-model="showResetConfirm" max-width="440" persistent>
          <template #activator="{ props }">
            <AppActionButton
              color="error"
              variant="tonal"
              text="Reset history"
              prepend-icon="mdi-database-remove-outline"
              v-bind="props"
            />
          </template>
          <v-card>
            <v-card-title>Confirm reset</v-card-title>
            <v-card-text>
              This will permanently delete all loans, repayments, blacklist entries, notifications, and reset business capital. Users and clients (and their profiles) will remain. Continue?
            </v-card-text>
            <v-card-actions>
              <v-spacer />
              <v-btn variant="text" @click="showResetConfirm = false">Cancel</v-btn>
              <v-btn color="error" variant="flat" :loading="resetting" @click="confirmResetHistory">Reset</v-btn>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import { useAppStore } from "../store";

const store = useAppStore();

const successMessage = ref("");
const error = ref("");
const loading = ref(true);
const saving = ref(false);
const savingContact = ref(false);
const resetting = ref(false);

const emailInput = ref("");
const showResetConfirm = ref(false);
const me = ref(null);

const businessContactForm = reactive({
  businessName: "",
  phone: "",
  email: "",
  address: "",
  bankName: "",
  accountHolderName: "",
  accountNumber: "",
  accountType: "",
  branchCode: "",
  paymentReference: "Use your ID number or Loan ID as reference"
});

// Load user data
watch(
  () => store.authMe,
  (v) => {
    me.value = v;
    if (v) emailInput.value = v.email || "";
  },
  { immediate: true }
);

onMounted(async () => {
  loading.value = true;
  error.value = "";
  try {
    const data = await store.fetchMe();
    me.value = data;
    emailInput.value = data?.email || "";

    if (store.isOwner) {
      try {
        const contact = await store.fetchLenderContact();
        businessContactForm.businessName = contact.businessName || "";
        businessContactForm.phone = contact.phone || "";
        businessContactForm.email = contact.email || "";
        businessContactForm.address = contact.address || "";
        businessContactForm.accountNumber = contact.accountNumber||"";
        businessContactForm.bankName = contact.bankName || "";
businessContactForm.accountHolderName = contact.accountHolderName || "";
businessContactForm.accountType = contact.accountType || "";
businessContactForm.branchCode = contact.branchCode || "";
businessContactForm.paymentReference = contact.paymentReference || "";
      } catch (contactError) {
        console.error("Failed to load business contact:", contactError);
      }
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Failed to load account";
  } finally {
    loading.value = false;
  }
});

// Save email
async function saveEmail() {
  saving.value = true;
  successMessage.value = "";
  error.value = "";
  try {
    await store.updateMyEmail(emailInput.value?.trim() || "");
    successMessage.value = "Email saved. You can use it to receive password reset links.";
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Failed to save email";
  } finally {
    saving.value = false;
  }
}

// Save business contact
async function saveBusinessContact() {
  savingContact.value = true;
  successMessage.value = "";
  error.value = "";
  try {
    await store.updateBusinessContact(businessContactForm);
    successMessage.value = "Business details saved successfully.";
  } catch (e) {
    error.value = e.response?.data?.message || e.message || "Failed to save business contact";
  } finally {
    savingContact.value = false;
  }
}

// Reset history
async function confirmResetHistory() {
  resetting.value = true;
  error.value = "";
  successMessage.value = "";

  try {
    const response = await store.resetHistory();

    console.log("Reset response:", response);

    showResetConfirm.value = false;

    successMessage.value =
      response?.message;

  } catch (e) {
    console.error("Reset history error:", e);
    error.value =
      e.response?.data?.message || e.message || "Failed to reset history";
  } finally {
    resetting.value = false;
  }
}
</script>