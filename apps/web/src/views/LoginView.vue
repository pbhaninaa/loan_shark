<template>
  <v-row justify="center" align="center" class="fill-height" style="min-height: calc(100vh - 96px);">
    <v-col cols="12" sm="12" md="8" lg="7" class="d-flex justify-center">
      <v-card class="pa-6 pa-md-10 login-card" style="max-width: 540px; width: 100%;">
        <div class="mb-6">
          <div class="text-overline text-primary">{{ headingLabel }}</div>
          <div class="text-h4 font-weight-bold">
            {{ headingTitle }}
          </div>
          <div class="text-body-1 text-medium-emphasis mt-2">
            {{ headingDescription }}
          </div>
        </div>

        <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
          {{ error }}
        </v-alert>

        <v-alert v-if="infoMessage" type="info" variant="tonal" class="mb-4">
          {{ infoMessage }}
        </v-alert>

        <!-- Sign In: only sign-in fields -->
        <template v-if="store.setup.ownerExists && mode === 'login'">
          <v-form @submit.prevent="submit">
            <AppTextField
              v-model="form.username"
              label="Username"
              prepend-inner-icon="mdi-account-outline"
              required
            />
            <AppTextField
              v-model="form.password"
              type="password"
              label="Password"
              prepend-inner-icon="mdi-lock-outline"
              required
            />
            <div class="d-flex align-center justify-space-between flex-wrap mt-2 mb-3">
              <AppActionButton type="submit" color="primary" size="large" :loading="loading" text="Sign In" />
              <v-btn variant="text" color="primary" size="small" class="text-none" @click="mode = 'forgot-password'">
                Forgot password?
              </v-btn>
            </div>
          </v-form>
          <v-divider class="my-4" />
          <p class="text-body-2 text-center text-medium-emphasis">
            <v-btn variant="text" color="primary" size="small" class="text-none" @click="mode = 'borrower-register'">
              Register as client
            </v-btn>
          </p>
        </template>

        <!-- Forgot password -->
        <template v-if="store.setup.ownerExists && mode === 'forgot-password'">
          <p class="text-body-2 text-medium-emphasis mb-3">
            Enter your username and we’ll send you a link to set a new password.
          </p>
          <v-form @submit.prevent="submitForgotPassword">
            <AppTextField
              v-model="forgotPasswordUsername"
              label="Username"
              prepend-inner-icon="mdi-account-outline"
              required
            />
            <div class="d-flex ga-2 mt-3">
              <AppActionButton type="submit" color="primary" :loading="forgotPasswordLoading" text="Send reset link" />
              <v-btn variant="tonal" @click="mode = 'login'">Back to sign in</v-btn>
            </div>
          </v-form>
          <v-alert v-if="forgotPasswordMessage" type="info" variant="tonal" class="mt-4">
            {{ forgotPasswordMessage }}
          </v-alert>
          <v-alert v-if="forgotPasswordResetLink" type="success" variant="tonal" class="mt-2">
            <div class="mb-2">Use this link to set a new password (valid for 24 hours):</div>
            <a :href="forgotPasswordResetLink" target="_blank" rel="noopener">{{ forgotPasswordResetLink }}</a>
          </v-alert>
        </template>

        <v-form v-else-if="!store.setup.ownerExists" @submit.prevent="createOwner">
          <AppTextField
            v-model="ownerForm.username"
            label="Owner username"
            prepend-inner-icon="mdi-shield-crown-outline"
            required
          />
          <AppTextField
            v-model="ownerForm.password"
            type="password"
            label="Password"
            prepend-inner-icon="mdi-lock-outline"
            required
          />
          <AppActionButton type="submit" color="primary" size="large" block :loading="loading" text="Create Owner Account" />
        </v-form>

        <!-- Borrower registration: stepper and success -->
        <template v-else-if="store.setup.ownerExists && mode === 'borrower-register' && borrowerSubmitted">
          <v-alert type="success" variant="tonal" class="mb-4">
            Profile created successfully. You must wait for owner review before you can use the system. You can sign in and check your verification status.
          </v-alert>
          <AppActionButton color="primary" size="large" block text="Go to verification status" @click="goToVerificationAfterRegister" />
        </template>
        <template v-else-if="store.setup.ownerExists && mode === 'borrower-register'">
          <p class="text-body-2 text-medium-emphasis mb-3">
            <v-btn variant="text" color="primary" size="small" class="text-none px-0" @click="mode = 'login'; borrowerStep = 1">
              ← Back to Sign In
            </v-btn>
          </p>
          <v-stepper v-model="borrowerStep" alt-labels class="elevation-0 transparent mb-4">
            <v-stepper-header>
              <v-stepper-item :value="1" :complete="borrowerStep > 1" editable>Details</v-stepper-item>
              <v-divider />
              <v-stepper-item :value="2" :complete="borrowerStep > 2" editable>Location</v-stepper-item>
              <v-divider />
              <v-stepper-item :value="3" :complete="borrowerStep > 3" editable>ID PDF</v-stepper-item>
              <v-divider />
              <v-stepper-item :value="4" :complete="borrowerStep > 4" editable>Selfie</v-stepper-item>
              <v-divider />
              <v-stepper-item :value="5" editable>Review</v-stepper-item>
            </v-stepper-header>
            <v-stepper-window>
              <v-stepper-window-item :value="1">
                <v-form @submit.prevent="nextStep" class="pt-2">
                  <v-row>
                    <v-col cols="12" md="6">
                      <AppTextField v-model="borrowerForm.username" label="Username" prepend-inner-icon="mdi-account-outline" required />
                    </v-col>
                    <v-col cols="12" md="6">
                      <AppTextField v-model="borrowerForm.password" type="password" label="Password" prepend-inner-icon="mdi-lock-outline" required />
                    </v-col>
                    <v-col cols="12" md="6">
                      <AppTextField v-model="borrowerForm.firstName" label="First name" prepend-inner-icon="mdi-account-outline" required />
                    </v-col>
                    <v-col cols="12" md="6">
                      <AppTextField v-model="borrowerForm.lastName" label="Last name" prepend-inner-icon="mdi-account-outline" required />
                    </v-col>
                    <v-col cols="12" md="6">
                      <AppTextField v-model="borrowerForm.idNumber" label="ID number" prepend-inner-icon="mdi-card-account-details-outline" required />
                    </v-col>
                    <v-col cols="12" md="6">
                      <AppTextField v-model="borrowerForm.phone" label="Phone" prepend-inner-icon="mdi-phone-outline" required />
                    </v-col>
                    <v-col cols="12">
                      <AppTextField v-model="borrowerForm.email" label="Email" prepend-inner-icon="mdi-email-outline" />
                    </v-col>
                    <v-col cols="12">
                      <AppTextField v-model="borrowerForm.address" label="Address" prepend-inner-icon="mdi-map-marker-outline" required />
                    </v-col>
                    <v-col cols="12" md="6">
                      <AppSelectField
                        v-model="borrowerForm.employmentStatus"
                        label="Employment type"
                        prepend-inner-icon="mdi-briefcase-outline"
                        :items="employmentTypeOptions"
                        required
                      />
                    </v-col>
                    <v-col cols="12" md="6">
                      <AppTextField v-model.number="borrowerForm.monthlyIncome" type="number" label="Monthly income" prepend-inner-icon="mdi-cash" required />
                    </v-col>
                    <v-col cols="12">
                      <AppTextField v-model="borrowerForm.employerName" label="Employer name" prepend-inner-icon="mdi-domain" />
                    </v-col>
                    <v-col cols="12">
                      <v-alert :type="saIdValid ? 'success' : 'warning'" variant="tonal">
                        {{ saIdValid ? "South African ID number format is valid." : "Enter a valid 13-digit South African ID number." }}
                      </v-alert>
                    </v-col>
                  </v-row>
                  <div class="d-flex justify-end">
                    <AppActionButton type="submit" text="Next: Location" color="primary" />
                  </div>
                </v-form>
              </v-stepper-window-item>
              <v-stepper-window-item :value="2">
                <div class="pt-2">
                  <v-alert :type="locationAlertType" variant="tonal" class="mb-4">
                    {{ locationLabel }}
                  </v-alert>
                  <p class="text-body-2 text-medium-emphasis mb-4">
                    Your location is captured automatically when you reach this step. If you declined permission, enable it and refresh or go back and continue again.
                  </p>
                  <div class="d-flex ga-2">
                    <AppActionButton text="Back" color="secondary" variant="tonal" @click="borrowerStep = 1" />
                    <AppActionButton text="Retry location" variant="tonal" @click="captureLocation()" />
                    <AppActionButton text="Next: ID PDF" color="primary" :disabled="!borrowerForm.locationName && !locationPermissionDenied" @click="borrowerStep = 3" />
                  </div>
                </div>
              </v-stepper-window-item>
              <v-stepper-window-item :value="3">
                <div class="pt-2">
                  <v-file-input
                    v-model="borrowerForm.idDocument"
                    accept="application/pdf,.pdf"
                    label="ID copy (PDF only)"
                    prepend-icon="mdi-file-pdf-box"
                    show-size
                    hint="Upload a clear PDF copy of your South African ID."
                    persistent-hint
                    class="mb-4"
                  />
                  <div class="d-flex ga-2">
                    <AppActionButton text="Back" color="secondary" variant="tonal" @click="borrowerStep = 2" />
                    <AppActionButton text="Next: Selfie" color="primary" :disabled="!idDocumentReady" @click="borrowerStep = 4" />
                  </div>
                </div>
              </v-stepper-window-item>
              <v-stepper-window-item :value="4">
                <div class="pt-2">
                  <v-card variant="tonal" class="pa-3 mb-4">
                    <div class="text-subtitle-2 mb-2">Live Selfie Capture</div>
                    <video
                      v-if="!selfiePreviewUrl"
                      ref="selfieVideoRef"
                      autoplay
                      playsinline
                      muted
                      class="w-100 rounded-lg bg-black"
                      style="min-height: 220px; object-fit: cover;"
                    />
                    <v-img
                      v-else
                      :src="selfiePreviewUrl"
                      class="rounded-lg"
                      cover
                      height="220"
                    />
                    <div class="d-flex ga-2 mt-3 flex-wrap">
                      <AppActionButton
                        text="Capture Selfie"
                        prepend-icon="mdi-camera"
                        :disabled="!cameraReady"
                        @click="captureSelfie"
                      />
                      <AppActionButton
                        text="Retake"
                        color="secondary"
                        variant="tonal"
                        @click="resetSelfieCapture"
                      />
                    </div>
                    <div class="text-body-2 text-medium-emphasis mt-2">
                      {{ selfieStatusLabel }}
                    </div>
                    <canvas ref="selfieCanvasRef" class="d-none" />
                  </v-card>
                  <div class="d-flex ga-2">
                    <AppActionButton text="Back" color="secondary" variant="tonal" @click="borrowerStep = 3" />
                    <AppActionButton text="Next: Review" color="primary" :disabled="!borrowerForm.selfieImage" @click="borrowerStep = 5" />
                  </div>
                </div>
              </v-stepper-window-item>
              <v-stepper-window-item :value="5">
                <div class="pt-2">
                  <v-alert type="info" variant="tonal" class="mb-4">
                    Review your details below. Submit to create your profile. You will then wait for owner verification before full access.
                  </v-alert>
                  <v-sheet variant="tonal" rounded="lg" class="pa-4 mb-4">
                    <div class="text-caption text-medium-emphasis">Personal</div>
                    <div class="mb-2">{{ borrowerForm.firstName }} {{ borrowerForm.lastName }}, {{ borrowerForm.idNumber }}</div>
                    <div class="text-caption text-medium-emphasis">Contact</div>
                    <div class="mb-2">{{ borrowerForm.phone }} {{ borrowerForm.email ? `· ${borrowerForm.email}` : "" }}</div>
                    <div class="text-caption text-medium-emphasis">Address</div>
                    <div class="mb-2">{{ borrowerForm.address }}</div>
                    <div class="text-caption text-medium-emphasis">Location captured</div>
                    <div class="mb-2">{{ borrowerForm.locationName || "—" }}</div>
                    <div class="text-caption text-medium-emphasis">Documents</div>
                    <div>ID PDF and selfie attached.</div>
                  </v-sheet>
                  <v-form @submit.prevent="registerBorrower">
                    <div class="d-flex ga-2">
                      <AppActionButton text="Back" color="secondary" variant="tonal" @click="borrowerStep = 4" />
                      <AppActionButton type="submit" color="primary" size="large" :loading="loading" text="Create Client Account" />
                    </div>
                  </v-form>
                </div>
              </v-stepper-window-item>
            </v-stepper-window>
          </v-stepper>
        </template>
      </v-card>
    </v-col>
  </v-row>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import { useRouter } from "vue-router";
import { useAppStore } from "../store";
import { extractApiError } from "../utils/apiErrors";

const router = useRouter();
const store = useAppStore();
const loading = ref(false);
const error = ref("");
const infoMessage = ref("");
const mode = ref("login");
const borrowerStep = ref(1);
const borrowerSubmitted = ref(false);
const selfieVideoRef = ref(null);
const selfieCanvasRef = ref(null);
const selfieStream = ref(null);
const selfiePreviewUrl = ref("");
const cameraReady = ref(false);

const form = reactive({
  username: "",
  password: ""
});

const forgotPasswordUsername = ref("");
const forgotPasswordLoading = ref(false);
const forgotPasswordMessage = ref("");
const forgotPasswordResetLink = ref("");

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

const ownerForm = reactive({
  username: "",
  password: ""
});

const initialBorrowerForm = () => ({
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
  latitude: null,
  longitude: null,
  locationName: "",
  idDocument: null,
  selfieImage: null
});

const borrowerForm = reactive(initialBorrowerForm());
const saIdValid = computed(() => isValidSouthAfricanId(borrowerForm.idNumber));
const locationLoading = ref(false);
const locationPermissionDenied = ref(false);
const locationLabel = computed(() => {
  if (locationLoading.value) {
    return "Capturing your current location automatically...";
  }
  if (locationPermissionDenied.value) {
    return "Location permission was declined. We will not save your profile without location access.";
  }
  if (!borrowerForm.latitude || !borrowerForm.longitude || !borrowerForm.locationName) {
    return "Waiting for your current location. Keep location access enabled to continue.";
  }
  return `${borrowerForm.locationName} (${borrowerForm.latitude}, ${borrowerForm.longitude})`;
});
const locationAlertType = computed(() => {
  if (locationPermissionDenied.value) {
    return "error";
  }
  if (borrowerForm.locationName) {
    return "success";
  }
  return "info";
});
const idDocumentReady = computed(() => {
  const v = borrowerForm.idDocument;
  return Array.isArray(v) ? v.length > 0 : !!v;
});

const selfieStatusLabel = computed(() => {
  if (selfiePreviewUrl.value) {
    return "Live selfie captured successfully.";
  }
  if (!cameraReady.value) {
    return "Waiting for camera permission so we can capture a live selfie.";
  }
  return "Use the live camera feed to capture the client's selfie.";
});

const headingLabel = computed(() => {
  if (!store.setup.ownerExists) return "Initial Setup";
  if (mode.value === "forgot-password") return "Forgot password";
  return mode.value === "login" ? "Sign In" : "Client Registration";
});

const headingTitle = computed(() => {
  if (!store.setup.ownerExists) return "Create the first owner account";
  if (mode.value === "forgot-password") return "Reset your password";
  return mode.value === "login" ? "Welcome back" : "Create your client account";
});

const headingDescription = computed(() => {
  if (!store.setup.ownerExists) return "No owner exists yet. Create one to unlock staff management and system usage.";
  if (mode.value === "forgot-password") return "Enter your username to receive a reset link.";
  return mode.value === "login"
    ? "Sign in to continue to the correct portal for your role."
    : "Clients can register here, sign in after profile creation, and wait for review before using the system.";
});

onMounted(async () => {
  if (!store.setupLoaded) {
    await store.fetchSetupStatus();
  }
  if (store.setup.ownerExists && mode.value === "borrower-register") {
    await captureLocation();
  }
});

watch(mode, async (currentMode) => {
  if (currentMode === "borrower-register") {
    borrowerStep.value = 1;
    borrowerSubmitted.value = false;
    const tasks = [];
    if (!borrowerForm.locationName && !locationLoading.value) {
      tasks.push(captureLocation());
    }
    await Promise.all(tasks);
    return;
  }
  stopSelfieCamera();
});

watch(borrowerStep, (step) => {
  if (step === 2 && !borrowerForm.locationName && !locationLoading.value) {
    captureLocation();
  }
  if (step === 4 && mode.value === "borrower-register") {
    startSelfieCamera();
  }
  if (step !== 4) {
    stopSelfieCamera();
  }
});

onBeforeUnmount(() => {
  stopSelfieCamera();
  clearSelfiePreview();
});

async function submit() {
  loading.value = true;
  error.value = "";
  infoMessage.value = "";
  try {
    const session = await store.login(form);
    const me = await store.fetchMe();
    if (!me?.email?.trim()) {
      router.push("/account");
      return;
    }
    if (session.role === "BORROWER") {
      const borrower = await store.fetchMyBorrower();
      router.push(borrower.status === "ACTIVE" ? "/my-portal/profile" : "/my-portal/verification");
      return;
    }
    router.push("/dashboard");
  } catch (requestError) {
    error.value = extractApiError(requestError, "Login failed");
  } finally {
    loading.value = false;
  }
}

async function submitForgotPassword() {
  forgotPasswordMessage.value = "";
  forgotPasswordResetLink.value = "";
  forgotPasswordLoading.value = true;
  error.value = "";
  try {
    const res = await store.forgotPassword(forgotPasswordUsername.value.trim());
    forgotPasswordMessage.value = res.message || "If an account exists for that username, a reset link has been sent.";
    if (res.resetLink) {
      forgotPasswordResetLink.value = res.resetLink;
    }
  } catch (requestError) {
    error.value = extractApiError(requestError, "Request failed");
  } finally {
    forgotPasswordLoading.value = false;
  }
}

async function createOwner() {
  loading.value = true;
  error.value = "";
  infoMessage.value = "";
  try {
    await store.createOwner(ownerForm);
    router.push("/account");
  } catch (requestError) {
    error.value = extractApiError(requestError, "Owner setup failed");
  } finally {
    loading.value = false;
  }
}

async function registerBorrower() {
  loading.value = true;
  error.value = "";
  infoMessage.value = "";
  if (!saIdValid.value) {
    error.value = "Please enter a valid South African ID number.";
    loading.value = false;
    return;
  }
  if (locationPermissionDenied.value || !borrowerForm.latitude || !borrowerForm.longitude || !borrowerForm.locationName) {
    error.value = "We cannot proceed without your live location permission and full location details.";
    loading.value = false;
    return;
  }
  if (!borrowerForm.idDocument || !borrowerForm.selfieImage) {
    error.value = "Please upload your ID PDF and capture a live selfie with the camera.";
    loading.value = false;
    return;
  }
  try {
    await store.registerBorrower({
      ...borrowerForm,
      idDocument: Array.isArray(borrowerForm.idDocument) ? borrowerForm.idDocument?.[0] : borrowerForm.idDocument,
      selfieImage: borrowerForm.selfieImage
    });
    await store.fetchMyBorrower();
    clearSelfiePreview();
    stopSelfieCamera();
    borrowerSubmitted.value = true;
  } catch (requestError) {
    error.value = extractApiError(requestError, "Client registration failed");
  } finally {
    loading.value = false;
  }
}

function captureLocation() {
  error.value = "";
  infoMessage.value = "";
  if (!navigator.geolocation) {
    error.value = "Geolocation is not supported by this browser.";
    return Promise.resolve();
  }
  locationLoading.value = true;
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        borrowerForm.latitude = position.coords.latitude.toFixed(7);
        borrowerForm.longitude = position.coords.longitude.toFixed(7);
        locationPermissionDenied.value = false;
        try {
          borrowerForm.locationName = await resolveLocationName(position.coords.latitude, position.coords.longitude);
          infoMessage.value = "Location captured successfully.";
        } catch {
          borrowerForm.locationName = "";
          error.value = "Could not resolve your full location name. Please retry with internet access enabled.";
        } finally {
          locationLoading.value = false;
          resolve();
        }
      },
      () => {
        locationPermissionDenied.value = true;
        borrowerForm.latitude = null;
        borrowerForm.longitude = null;
        borrowerForm.locationName = "";
        error.value = "Location access was declined. We will not proceed without it.";
        locationLoading.value = false;
        resolve();
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  });
}

async function resolveLocationName(latitude, longitude) {
  const response = await fetch(
    `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${latitude}&longitude=${longitude}&localityLanguage=en`
  );
  if (!response.ok) {
    throw new Error("Reverse geocoding failed");
  }
  const data = await response.json();
  const parts = [
    data.locality,
    data.city || data.principalSubdivision,
    data.principalSubdivision,
    data.countryName
  ].filter(Boolean);
  return [...new Set(parts)].join(", ");
}

async function startSelfieCamera() {
  if (mode.value !== "borrower-register" || selfieStream.value) {
    return;
  }
  if (!navigator.mediaDevices?.getUserMedia) {
    error.value = "This browser cannot access the camera for live selfie capture.";
    return;
  }
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: "user"
      },
      audio: false
    });
    selfieStream.value = stream;
    cameraReady.value = true;
    if (selfieVideoRef.value) {
      selfieVideoRef.value.srcObject = stream;
    }
  } catch {
    cameraReady.value = false;
    error.value = "Camera permission is required. We will not proceed without a live selfie capture.";
  }
}

function captureSelfie() {
  if (!selfieVideoRef.value || !selfieCanvasRef.value) {
    return;
  }
  const video = selfieVideoRef.value;
  const canvas = selfieCanvasRef.value;
  const width = video.videoWidth || 640;
  const height = video.videoHeight || 480;
  canvas.width = width;
  canvas.height = height;
  const context = canvas.getContext("2d");
  if (!context) {
    error.value = "Could not capture the selfie frame.";
    return;
  }
  context.drawImage(video, 0, 0, width, height);
  canvas.toBlob((blob) => {
    if (!blob) {
      error.value = "Could not capture the selfie frame.";
      return;
    }
    clearSelfiePreview();
    borrowerForm.selfieImage = new File([blob], "live-selfie.jpg", { type: "image/jpeg" });
    selfiePreviewUrl.value = URL.createObjectURL(blob);
    infoMessage.value = "Live selfie captured successfully.";
    stopSelfieCamera();
  }, "image/jpeg", 0.92);
}

async function resetSelfieCapture() {
  borrowerForm.selfieImage = null;
  clearSelfiePreview();
  stopSelfieCamera();
  await startSelfieCamera();
}

function stopSelfieCamera() {
  if (selfieStream.value) {
    selfieStream.value.getTracks().forEach((track) => track.stop());
    selfieStream.value = null;
  }
  if (selfieVideoRef.value) {
    selfieVideoRef.value.srcObject = null;
  }
  cameraReady.value = false;
}

function clearSelfiePreview() {
  if (selfiePreviewUrl.value) {
    URL.revokeObjectURL(selfiePreviewUrl.value);
    selfiePreviewUrl.value = "";
  }
}

function isValidSouthAfricanId(idNumber) {
  if (!/^\d{13}$/.test(idNumber || "")) {
    return false;
  }
  const yearPart = Number(idNumber.slice(0, 2));
  const month = Number(idNumber.slice(2, 4));
  const day = Number(idNumber.slice(4, 6));
  const now = new Date();
  const currentYearPart = now.getFullYear() % 100;
  const fullYear = yearPart <= currentYearPart ? 2000 + yearPart : 1900 + yearPart;
  const date = new Date(fullYear, month - 1, day);
  if (date.getFullYear() !== fullYear || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return false;
  }
  let sumOdd = 0;
  for (let index = 0; index < 12; index += 2) {
    sumOdd += Number(idNumber[index]);
  }
  let evenDigits = "";
  for (let index = 1; index < 12; index += 2) {
    evenDigits += idNumber[index];
  }
  const doubled = String(Number(evenDigits) * 2);
  const sumEven = doubled.split("").reduce((total, digit) => total + Number(digit), 0);
  const total = sumOdd + sumEven;
  const checkDigit = (10 - (total % 10)) % 10;
  return checkDigit === Number(idNumber[12]);
}

function nextStep() {
  if (borrowerStep.value < 5) {
    borrowerStep.value++;
  }
}

function goToVerificationAfterRegister() {
  router.push("/my-portal/verification");
}
</script>
