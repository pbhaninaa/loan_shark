<template>
  <div class="viewport-scaler-outer">
    <div class="viewport-scaler-inner" :style="scalerStyle">
  <v-app>
    <template v-if="store.isAuthenticated">
      <v-navigation-drawer v-model="drawer" color="secondary" theme="dark" rail-width="88">
        <div class="d-flex flex-column fill-height">
          <div class="px-4 pt-6 pb-4">
            <div class="text-h5 font-weight-bold">Loan Shark</div>
            <div class="text-caption text-medium-emphasis">Operations Portal</div>
          </div>

          <v-list nav density="comfortable">
            <v-list-item
              v-for="item in navItems"
              :key="item.to"
              :title="item.title"
              :prepend-icon="item.icon"
              :to="item.to"
              rounded="xl"
            >
              <template v-if="item.badgeKey && pendingCount(item.badgeKey) > 0" #append>
                <v-tooltip :text="`${pendingCount(item.badgeKey)} pending`" location="left">
                  <template #activator="{ props: tooltipProps }">
                    <v-avatar
                      v-bind="tooltipProps"
                      :size="22"
                      color="error"
                      class="text-caption font-weight-bold"
                    >
                      {{ pendingCount(item.badgeKey) > 99 ? '99+' : pendingCount(item.badgeKey) }}
                    </v-avatar>
                  </template>
                </v-tooltip>
              </template>
            </v-list-item>
          </v-list>

          <v-spacer />

          <div class="px-4 pb-6">
            <v-card color="rgba(255,255,255,0.08)" rounded="xl" class="mb-4">
              <v-card-text>
                <div class="text-subtitle-2">Signed in as</div>
                <div class="text-h6">{{ displayName }}</div>
                <div class="text-caption text-medium-emphasis mt-1">{{ roleLabel }}</div>
              </v-card-text>
            </v-card>

            <v-btn color="primary" variant="tonal" block prepend-icon="mdi-lock-reset" class="mb-2" @click="showChangePasswordDialog = true">
              Change password
            </v-btn>
            <v-btn color="error" block prepend-icon="mdi-logout" @click="logout">
              Logout
            </v-btn>
          </div>
        </div>
      </v-navigation-drawer>

      <v-dialog v-model="showChangePasswordDialog" max-width="440" persistent>
        <v-card>
          <v-card-title class="d-flex align-center">
            <v-icon start>mdi-lock-reset</v-icon>
            Change password
          </v-card-title>
          <v-divider />
          <v-card-text>
            <v-form ref="changePasswordFormRef" @submit.prevent="submitChangePassword">
              <v-text-field
                v-model="changePasswordForm.currentPassword"
                label="Current password"
                type="password"
                prepend-inner-icon="mdi-lock-outline"
                autocomplete="current-password"
                :error-messages="changePasswordError"
                class="mb-2"
              />
              <v-text-field
                v-model="changePasswordForm.newPassword"
                label="New password"
                type="password"
                prepend-inner-icon="mdi-lock-outline"
                autocomplete="new-password"
                class="mb-2"
              />
              <v-text-field
                v-model="changePasswordForm.confirmPassword"
                label="Confirm new password"
                type="password"
                prepend-inner-icon="mdi-lock-outline"
                autocomplete="new-password"
                :error-messages="changePasswordForm.newPassword && changePasswordForm.confirmPassword !== changePasswordForm.newPassword ? ['Passwords do not match'] : []"
              />
            </v-form>
          </v-card-text>
          <v-divider />
          <v-card-actions>
            <v-spacer />
            <v-btn variant="text" @click="closeChangePasswordDialog">Cancel</v-btn>
            <v-btn color="primary" :loading="changePasswordLoading" @click="submitChangePassword">Update password</v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>

      <v-app-bar flat color="background" elevation="0">
        <v-app-bar-nav-icon @click="drawer = !drawer" />
        <v-toolbar-title class="font-weight-bold">Loan Management Portal</v-toolbar-title>
        <v-spacer />
        <v-btn
          v-if="store.isAuthenticated"
          :icon="isDark ? 'mdi-weather-sunny' : 'mdi-weather-night'"
          variant="text"
          size="small"
          :title="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
          @click="toggleDarkMode"
        />
      </v-app-bar>
    </template>

    <v-main>
      <v-container fluid class="pa-4 pa-md-8">
        <router-view />
      </v-container>
    </v-main>
  </v-app>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { useTheme } from "vuetify";
import { useAppStore } from "./store";

const DRAWER_KEY = "loanSharkDarkMode";

const REF_WIDTH = 1280;
const REF_HEIGHT = 800;

const scale = ref(1);
const drawer = ref(true);
const router = useRouter();
const store = useAppStore();
const theme = useTheme();

const isDark = computed(() => theme.global.name.value === "loanSharkDark");

function toggleDarkMode() {
  const next = isDark.value ? "loanSharkTheme" : "loanSharkDark";
  theme.global.name.value = next;
  try {
    localStorage.setItem(DRAWER_KEY, next);
  } catch (_) {}
}

function applySavedTheme() {
  try {
    const saved = localStorage.getItem(DRAWER_KEY);
    if (saved === "loanSharkDark" || saved === "loanSharkTheme") {
      theme.global.name.value = saved;
    }
  } catch (_) {}
}

onMounted(() => {
  applySavedTheme();
  updateScale();
  window.addEventListener("resize", updateScale);
});
onUnmounted(() => {
  window.removeEventListener("resize", updateScale);
});

function pendingCount(badgeKey) {
  if (!store.dashboard || !badgeKey) return 0;
  const n = Number(store.dashboard[badgeKey]);
  return Number.isFinite(n) ? n : 0;
}

onMounted(() => {
  if (store.isAuthenticated && !store.isBorrower) {
    store.fetchDashboard();
  }
});

// Refresh pending counts when visiting pages that can change them
router.afterEach((to) => {
  if (store.isAuthenticated && !store.isBorrower && ["dashboard", "loans", "verifications"].includes(String(to.name))) {
    store.fetchDashboard();
  }
});
const showChangePasswordDialog = ref(false);
const changePasswordLoading = ref(false);
const changePasswordError = ref("");
const changePasswordForm = ref({
  currentPassword: "",
  newPassword: "",
  confirmPassword: ""
});

function closeChangePasswordDialog() {
  showChangePasswordDialog.value = false;
  changePasswordForm.value = { currentPassword: "", newPassword: "", confirmPassword: "" };
  changePasswordError.value = "";
}

async function submitChangePassword() {
  changePasswordError.value = "";
  if (!changePasswordForm.value.newPassword || changePasswordForm.value.newPassword.length < 4) {
    changePasswordError.value = "New password must be at least 4 characters.";
    return;
  }
  if (changePasswordForm.value.newPassword !== changePasswordForm.value.confirmPassword) {
    changePasswordError.value = "New password and confirmation do not match.";
    return;
  }
  changePasswordLoading.value = true;
  try {
    await store.changePassword({
      currentPassword: changePasswordForm.value.currentPassword,
      newPassword: changePasswordForm.value.newPassword
    });
    closeChangePasswordDialog();
  } catch (e) {
    changePasswordError.value = e.response?.data?.message || e.message || "Failed to change password.";
  } finally {
    changePasswordLoading.value = false;
  }
}

const navItems = computed(() => {
  const items = [
    ...(store.isBorrower
      ? store.borrowerStatus && store.borrowerStatus !== "ACTIVE"
        ? [
            {
              title: "Verification Status",
              to: "/my-portal/verification",
              icon: "mdi-shield-account-outline"
            }
          ]
        : [
            { title: "My Profile", to: "/my-portal/profile", icon: "mdi-account-circle-outline" },
            { title: "My account", to: "/account", icon: "mdi-account-cog-outline" },
            { title: "My Loans", to: "/my-portal/loans", icon: "mdi-cash-multiple" },
            { title: "Repayment Schedule", to: "/my-portal/schedule", icon: "mdi-calendar-clock-outline" },
            { title: "Notifications", to: "/my-portal/notifications", icon: "mdi-bell-outline" },
            { title: "Help & Contact", to: "/my-portal/help", icon: "mdi-help-circle-outline" }
          ]
        : [
          { title: "Dashboard", to: "/dashboard", icon: "mdi-view-dashboard-outline" },

          // Core lending workflow
          { title: "Clients", to: "/borrowers", icon: "mdi-account-group-outline" },
          { title: "Loans", to: "/loans", icon: "mdi-cash-multiple", badgeKey: "pendingLoans" },
          { title: "Repayments", to: "/repayments", icon: "mdi-cash-check" }
        ])
  ];

  if (store.isOwner) {

    // Risk management
    items.push({
      title: "Verifications",
      to: "/verifications",
      icon: "mdi-shield-check-outline",
      badgeKey: "pendingVerifications"
    });

    items.push({
      title: "Blacklist",
      to: "/blacklist",
      icon: "mdi-account-cancel-outline"
    });

    // Administration
    items.push({
      title: "Users",
      to: "/users",
      icon: "mdi-account-tie-outline"
    });

    items.push({
      title: "My account",
      to: "/account",
      icon: "mdi-account-cog-outline"
    });

    // System settings
    items.push({
      title: "Loan interest & term",
      to: "/settings/loan-interest",
      icon: "mdi-percent"
    });

    items.push({
      title: "Business capital",
      to: "/settings/business-capital",
      icon: "mdi-bank-outline"
    });
  }

  return items;
});

function updateScale() {
  const w = window.innerWidth;
  const h = window.innerHeight;
  const s = Math.min(1, w / REF_WIDTH, h / REF_HEIGHT);
  scale.value = Math.max(0.5, Math.round(s * 100) / 100);
}

const scalerStyle = computed(() => ({
  transform: `scale(${scale.value})`,
  transformOrigin: "0 0",
  width: `${100 / scale.value}vw`,
  height: `${100 / scale.value}vh`
}));

const displayName = computed(() => {
  const name = store.username;
  if (name && String(name).trim()) return String(name).trim();
  const fromToken = usernameFromToken(store.token);
  if (fromToken) return fromToken;
  return "User";
});

function usernameFromToken(token) {
  if (!token || typeof token !== "string") return null;
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const payload = JSON.parse(atob(parts[1].replace(/-/g, "+").replace(/_/g, "/")));
    return payload.sub || null;
  } catch {
    return null;
  }
}

const roleLabel = computed(() => {
  if (store.isOwner) {
    return "Owner";
  }
  if (store.isCashier) {
    return "Cashier";
  }
  if (store.isBorrower) {
    return "Client";
  }
  return "User";
});

function logout() {
  store.clearSession();
  router.push("/login");
}
</script>
