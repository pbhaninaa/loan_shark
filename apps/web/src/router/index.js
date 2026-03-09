import { createRouter, createWebHashHistory } from "vue-router";
import LoginView from "../views/LoginView.vue";
import DashboardView from "../views/DashboardView.vue";
import BorrowersView from "../views/BorrowersView.vue";
import LoansView from "../views/LoansView.vue";
import RepaymentsView from "../views/RepaymentsView.vue";
import BlacklistView from "../views/BlacklistView.vue";
import UsersView from "../views/UsersView.vue";
import VerificationsView from "../views/VerificationsView.vue";
import LoanInterestSettingsView from "../views/LoanInterestSettingsView.vue";
import BusinessCapitalView from "../views/BusinessCapitalView.vue";
import BorrowerProfileView from "../views/BorrowerProfileView.vue";
import BorrowerLoansView from "../views/BorrowerLoansView.vue";
import BorrowerScheduleView from "../views/BorrowerScheduleView.vue";
import BorrowerPaymentHistoryView from "../views/BorrowerPaymentHistoryView.vue";
import BorrowerNotificationsView from "../views/BorrowerNotificationsView.vue";
import BorrowerHelpView from "../views/BorrowerHelpView.vue";
import BorrowerVerificationStatusView from "../views/BorrowerVerificationStatusView.vue";
import AccountView from "../views/AccountView.vue";
import ResetPasswordView from "../views/ResetPasswordView.vue";
import { useAppStore } from "../store";

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: "/login", name: "login", component: LoginView },
    { path: "/reset-password", name: "reset-password", component: ResetPasswordView },
    { path: "/account", name: "account", component: AccountView, meta: { auth: true } },
    { path: "/", redirect: "/dashboard" },
    { path: "/dashboard", name: "dashboard", component: DashboardView, meta: { auth: true } },
    { path: "/my-portal", redirect: "/my-portal/profile" },
    { path: "/my-portal/verification", name: "borrower-verification", component: BorrowerVerificationStatusView, meta: { auth: true, borrowerOnly: true } },
    { path: "/my-portal/profile", name: "borrower-profile", component: BorrowerProfileView, meta: { auth: true, borrowerOnly: true } },
    { path: "/my-portal/loans", name: "borrower-loans", component: BorrowerLoansView, meta: { auth: true, borrowerOnly: true } },
    { path: "/my-portal/schedule", name: "borrower-schedule", component: BorrowerScheduleView, meta: { auth: true, borrowerOnly: true } },
    { path: "/my-portal/payment-history", name: "borrower-payment-history", component: BorrowerPaymentHistoryView, meta: { auth: true, borrowerOnly: true } },
    { path: "/my-portal/notifications", name: "borrower-notifications", component: BorrowerNotificationsView, meta: { auth: true, borrowerOnly: true } },
    { path: "/my-portal/help", name: "borrower-help", component: BorrowerHelpView, meta: { auth: true, borrowerOnly: true } },
    { path: "/borrowers", name: "borrowers", component: BorrowersView, meta: { auth: true } },
    { path: "/loans", name: "loans", component: LoansView, meta: { auth: true } },
    { path: "/repayments", name: "repayments", component: RepaymentsView, meta: { auth: true } },
    { path: "/notifications", name: "notifications", component: BorrowerNotificationsView, meta: { auth: true, staffOnly: true } },
    { path: "/users", name: "users", component: UsersView, meta: { auth: true, ownerOnly: true } },
    { path: "/verifications", name: "verifications", component: VerificationsView, meta: { auth: true, ownerOnly: true } },
    { path: "/settings/loan-interest", name: "loan-interest-settings", component: LoanInterestSettingsView, meta: { auth: true, ownerOnly: true } },
    { path: "/settings/business-capital", name: "business-capital", component: BusinessCapitalView, meta: { auth: true, ownerOnly: true } },
    { path: "/blacklist", name: "blacklist", component: BlacklistView, meta: { auth: true, ownerOnly: true } }
  ]
});

router.beforeEach(async (to) => {
  const store = useAppStore();
  if (!store.setupLoaded) {
    await store.fetchSetupStatus(); // never blocks: on API failure still sets setupLoaded
  }

  if (to.meta.auth && !store.isAuthenticated) {
    return { name: "login" };
  }

  if (to.meta.ownerOnly && !store.isOwner) {
    return { name: "dashboard" };
  }

  if (to.meta.staffOnly && !store.isOwner && !store.isCashier) {
    return { name: "dashboard" };
  }

  if (to.meta.borrowerOnly && !store.isBorrower) {
    return { name: "dashboard" };
  }

  if (store.isBorrower && !store.borrowerStatus) {
    try {
      await store.fetchMyBorrower();
    } catch {
      return { name: "login" };
    }
  }

  if (store.isBorrower && store.borrowerStatus && store.borrowerStatus !== "ACTIVE" && to.name !== "borrower-verification") {
    return { name: "borrower-verification" };
  }

  if (store.isBorrower && ["dashboard", "borrowers", "loans", "repayments", "users", "verifications", "blacklist"].includes(String(to.name))) {
    return { name: store.borrowerStatus === "ACTIVE" ? "borrower-profile" : "borrower-verification" };
  }

  if (to.name === "login" && store.isAuthenticated) {
    return { name: store.isBorrower ? (store.borrowerStatus === "ACTIVE" ? "borrower-profile" : "borrower-verification") : "dashboard" };
  }

  return true;
});

export default router;
