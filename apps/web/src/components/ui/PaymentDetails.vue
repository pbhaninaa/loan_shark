<template>
  <div class="page-shell">
    <!-- Page Header -->
    <AppPageHeader
      title="Payment Details"
      description="Bank information for your loan payments. ⚡ Please copy carefully."
    />

    <!-- Loading Skeleton -->
    <v-skeleton-loader
      v-if="loading"
      type="card"
      class="mb-4"
      height="250px"
    ></v-skeleton-loader>

    <!-- Payment Details Card -->
    <v-card v-else class="mb-4" elevation="2">
      <v-card-title class="d-flex align-center ga-2">
        <v-icon color="primary">mdi-bank</v-icon>
        Bank Account Information
      </v-card-title>

      <v-divider />

      <v-alert type="info" variant="tonal" density="compact" class="mb-3">
        <strong>Important:</strong> Please verify the details when making payments, as we are not responsible for any mistakes.
      </v-alert>

      <v-card-text>
        <v-row dense>
          <!-- Left Column: Bank Details -->
          <v-col cols="12" md="6">
            <v-row dense>
              <v-col cols="12" v-for="(value, key) in paymentDetails" :key="key">
                <div v-if="!key.toUpperCase().includes('LINK')" class="d-flex align-center ga-1">
                  <div 
                    :class="[
                      'text-h6 font-weight-bold',
                      key.includes('Ref') ? 'blinks' : ''
                    ]"
                  >
                    {{ value }}
                  </div>
                </div>
                <div v-if="!key.toUpperCase().includes('LINK')"  class="text-caption text-medium-emphasis">{{ formatKey(key) }}</div>
              </v-col>
            </v-row>
          </v-col>

          <!-- Right Column: QR Code -->
          <v-col cols="12" md="6" class="d-flex flex-column align-center">
             <v-alert
              v-if="message"
              type="info"
              variant="tonal"
              class="mt-2 mb-5"
            >
              {{ message }}
            </v-alert>
            <v-card class="mb-4 qr-card elevation-2 primary">
              <v-card-title class="white--text">
                QR Code Payment (for Capitec Users)
              </v-card-title>
              <v-card-text class="d-flex justify-center align-center">
                <qrcode-vue
                  v-if="qrValue"
                  :value="qrValue"
                  :size="200"
                  level="H"
                  @click="instantPay"
                  style="cursor: pointer;"
                />
              </v-card-text>
            </v-card>

            <!-- Message from instantPay -->
           
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useAppStore } from "../../store";
import QrcodeVue from "qrcode.vue";

const router = useRouter();
const store = useAppStore();

const paymentDetails = ref({
  bankName: "",
  accountNumber: "",
  accountHolderName: "",
  accountType: "",
  branchCode: "",
  paymentReference: "",
  paymentLink:""
});
const loading = ref(true);
const message = ref(""); // <-- message to display after instantPay

// Navigate back
const goBack = () => router.back();

// Load payment details from API/store
const loadPaymentDetails = async () => {
  try {
    if (store && store.fetchLenderContact) {
      const data = await store.fetchLenderContact();
      Object.assign(paymentDetails.value, {
        accountNumber: data.accountNumber || "",
        bankName: data.bankName || "",
        accountHolderName: data.accountHolderName || "",
        accountType: data.accountType || "",
        branchCode: data.branchCode || "",
        paymentReference: data.paymentReference || "User ID Number as a reference",
        paymentLink:data.paymentLink || ""
      });
    }
  } catch (err) {
    console.error(err);
  } finally {
    loading.value = false;
  }
};

// Generate QR code string
const qrValue = computed(() => {
  return paymentDetails.value.paymentLink;
});

// Handle instant pay click
async function instantPay() {
  try {
    const result = await store.instantPay();
    message.value = result?.message || "Payment action completed.";
    console.log("Instant Pay result:", result);
  } catch (error) {
    message.value = "Failed to initiate Instant Pay. Please try again.";
    console.error("Instant Pay error:", error);
  }
}

// Format keys for display
const formatKey = (key) =>
  key.replace(/([A-Z])/g, " $1").replace(/^./, (str) => str.toUpperCase());

onMounted(loadPaymentDetails);
</script>

<style scoped>
.qr-card {
  background-color: rgb(63, 81, 87);
  border-radius: 12px;
  padding: 16px;
  color: white;
}

.qr-card .v-card-title {
  font-weight: bold;
  font-size: 1.2rem;
  text-align: center;
}

.qr-card .v-card-text {
  padding: 16px;
}

.blinks {
  animation: blink 1s step-start infinite;
  color: rgb(253, 249, 9);
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}
</style>