import { defineStore } from "pinia";
import api from "../services/api";

const token = localStorage.getItem("loanSharkToken");
const role = localStorage.getItem("loanSharkRole");
const userId = localStorage.getItem("loanSharkUserId");
const username = localStorage.getItem("loanSharkUsername");
const borrowerId = localStorage.getItem("loanSharkBorrowerId");
const borrowerStatus = localStorage.getItem("loanSharkBorrowerStatus");

export const useAppStore = defineStore("app", {
  state: () => ({
    token,
    role,
    userId,
    username: username || null,
    borrowerId,
    borrowerStatus,
    dashboard: null,
    borrowerSummary: null,
    actions: [],
    actionsPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    borrowers: [],
    borrowersPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    users: [],
    usersPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    verifications: [],
    loans: [],
    loansPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    pendingLoans: [],
    pendingLoansPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    blacklist: [],
    blacklistPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    repayments: [],
    repaymentsPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    notifications: [],
    notificationsPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    borrowerProfile: null,
    authMe: null,
    verification: null,
    loanSchedule: [],
    setup: {
      ownerExists: true
    },
    setupLoaded: false
  }),
  actions: {
    applyPagedResult(itemsKey, pageKey, data) {
      this[itemsKey] = data.content;
      this[pageKey] = {
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages
      };
    },
    setSession(payload) {
      this.token = payload.token;
      this.role = payload.role;
      this.userId = payload.userId;
      this.username = payload.username ?? null;
      this.borrowerId = payload.borrowerId ?? null;
      localStorage.setItem("loanSharkToken", payload.token);
      localStorage.setItem("loanSharkRole", payload.role);
      localStorage.setItem("loanSharkUserId", payload.userId);
      if (payload.username != null) localStorage.setItem("loanSharkUsername", payload.username);
      localStorage.setItem("loanSharkBorrowerId", payload.borrowerId ?? "");
    },
    setBorrowerStatus(status) {
      this.borrowerStatus = status ?? null;
      if (status) {
        localStorage.setItem("loanSharkBorrowerStatus", status);
      } else {
        localStorage.removeItem("loanSharkBorrowerStatus");
      }
    },
    clearSession() {
      this.token = null;
      this.role = null;
      this.userId = null;
      this.username = null;
      this.borrowerId = null;
      this.borrowerStatus = null;
      this.dashboard = null;
      this.borrowerSummary = null;
      this.actions = [];
      this.borrowerProfile = null;
      this.authMe = null;
      this.verification = null;
      this.notifications = [];
      this.loanSchedule = [];
      localStorage.removeItem("loanSharkToken");
      localStorage.removeItem("loanSharkRole");
      localStorage.removeItem("loanSharkUserId");
      localStorage.removeItem("loanSharkUsername");
      localStorage.removeItem("loanSharkBorrowerId");
      localStorage.removeItem("loanSharkBorrowerStatus");
    },
    async fetchSetupStatus() {
      try {
        const { data } = await api.get("/auth/setup-status");
        this.setup = data;
      } catch {
        this.setup = { ownerExists: true };
      }
      this.setupLoaded = true;
    },
   async loadDocument(url) {
  const token = this.token || localStorage.getItem("loanSharkToken");

  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!res.ok) throw new Error(res.statusText);

  const blob = await res.blob();

  // ✅ Only return value — don't touch UI state
  return URL.createObjectURL(blob);
},
    async login(credentials) {
      const { data } = await api.post("/auth/login", credentials);
      this.setSession(data);
      return data;
    },
    async forgotPassword(username) {
      const { data } = await api.post("/auth/forgot-password", { username });
      return data;
    },
    async fetchMe() {
      const { data } = await api.get("/auth/me");
      this.authMe = data;
      return data;
    },
    async updateMyEmail(email) {
      await api.put("/auth/me/email", { email: email || "" });
      if (this.authMe) this.authMe = { ...this.authMe, email: email || null };
    },
    async resetPasswordWithToken(token, newPassword) {
      await api.post("/auth/reset-password", { token, newPassword });
    },
    async createOwner(payload) {
      const { data } = await api.post("/auth/register/owner", payload);
      this.setSession(data);
      this.setup = { ownerExists: true };
      this.setupLoaded = true;
      return data;
    },
    async registerBorrower(payload) {
      const formData = new FormData();
      Object.entries(payload).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          if (value instanceof File) {
            formData.append(key, value);
          } else if (Array.isArray(value) && value[0] instanceof File) {
            formData.append(key, value[0]);
          } else {
            formData.append(key, value);
          }
        }
      });
      const { data } = await api.post("/auth/register/borrower", formData, {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      });
      this.setSession(data);
      return data;
    },
    async createStaff(payload) {
      const { data } = await api.post("/auth/register/staff", payload);
      return data;
    },
    async fetchUsers(params = {}) {
      const { data } = await api.get("/users", { params });
      this.applyPagedResult("users", "usersPage", data);
    },
    async fetchManualReviewVerifications() {
      const { data } = await api.get("/verifications/manual-review");
      this.verifications = data;
    },
    async approveVerification(id, notes) {
      const { data } = await api.post(`/verifications/${id}/approve`, { notes });
      return data;
    },
    async rejectVerification(id, notes) {
      const { data } = await api.post(`/verifications/${id}/reject`, { notes });
      return data;
    },
    async fetchVerificationDocument(id, kind) {
      const { data, headers } = await api.get(`/verifications/${id}/${kind}`, {
        responseType: "blob"
      });
      return {
        blob: data,
        contentType: headers["content-type"] || data.type || "application/octet-stream"
      };
    },
    async createUser(payload) {
      const { data } = await api.post("/users", payload);
      return data;
    },
    async updateUser(id, payload) {
      const { data } = await api.put(`/users/${id}`, payload);
      return data;
    },
    async deleteUser(id) {
      await api.delete(`/users/${id}`);
    },
    async fetchDashboard() {
      const { data } = await api.get("/dashboard/summary");
      this.dashboard = data;
    },
    async fetchBorrowerSummary() {
      const { data } = await api.get("/dashboard/borrower-summary");
      this.borrowerSummary = data;
    },
    async fetchActions(params = {}) {
      const { data } = await api.get("/dashboard/actions", { params });
      this.applyPagedResult("actions", "actionsPage", data);
    },
    async fetchBorrowers(params = {}) {
      const { data } = await api.get("/borrowers", { params });
      this.applyPagedResult("borrowers", "borrowersPage", data);
    },
    async fetchBorrowerById(id) {
      const { data } = await api.get(`/borrowers/${id}`);
      return data;
    },
    async updateBorrower(id, payload) {
      const { data } = await api.put(`/borrowers/${id}`, payload);
      return data;
    },
    async deleteBorrower(id) {
      await api.delete(`/borrowers/${id}`);
    },
    async fetchVerificationByBorrowerId(borrowerId) {
      const { data } = await api.get(`/verifications/by-borrower/${borrowerId}`);
      return data;
    },
    async changePassword(payload) {
      await api.post("/auth/change-password", payload);
    },
    async resetUserPassword(userId, newPassword) {
      await api.post("/auth/reset-user-password", { userId, newPassword });
    },
    async fetchLoans(params = {}) {
      const { data } = await api.get("/loans", { params });
      this.applyPagedResult("loans", "loansPage", data);
    },
    async fetchPendingLoans(params = {}) {
      const { data } = await api.get("/loans", { params: { ...params, status: ["PENDING"] } });
      this.applyPagedResult("pendingLoans", "pendingLoansPage", data);
    },
    async updateLoan(loanId, payload) {
      const { data } = await api.put(`/loans/${loanId}`, payload);
      return data;
    },
    async deleteLoan(loanId) {
      await api.delete(`/loans/${loanId}`);
    },
    async fetchMyLoans(params = {}) {
      const { data } = await api.get("/loans/my", { params });
      this.applyPagedResult("loans", "loansPage", data);
    },
    async fetchMyBorrower() {
      const { data } = await api.get("/borrowers/me");
      this.bpaymentLinkorrowerProfile = data;
      this.setBorrowerStatus(data.status);
      return data;
    },
    async fetchMyVerification() {
      const { data } = await api.get("/verifications/me");
      this.verification = data;
      return data;
    },
   async  instantPay() {
    const businessDatails = await this.fetchLenderContact()
  const isMobile = /iPhone|iPad|iPod|Android/i.test(navigator.userAgent);
  if (!isMobile) {
    return {
      message: "Please use a mobile device with the Capitec app installed.",
      timestamp: new Date().toISOString(),
    };
  }

  return new Promise((resolve) => {
    const now = Date.now();

    // Attempt to open the app using iframe
    const iframe = document.createElement("iframe");
    iframe.style.display = "none";
    iframe.src = businessDatails.value.paymentLink;
    document.body.appendChild(iframe);

    // Timeout to check if app opened
    setTimeout(() => {
      const delta = Date.now() - now;
      document.body.removeChild(iframe);

      if (delta < 1500) {
        // App probably not installed
        resolve({
          message: "You don’t have the Capitec app installed. Please install it to pay via the app.",
          timestamp: new Date().toISOString(),
        });
      } else {
        // App probably opened successfully
        resolve({
          message: "Capitec app opened successfully.",
          timestamp: new Date().toISOString(),
        });
      }
    }, 1000);
  });
},
    async fetchLoanSchedule(loanId) {
      const id = loanId != null && String(loanId).trim() !== "" && String(loanId) !== "NaN" ? String(loanId).trim() : null;
      if (!id) {
        this.loanSchedule = [];
        return [];
      }
      const { data } = await api.get(`/loans/${id}/schedule`);
      this.loanSchedule = data;
      return data;
    },
    async fetchLenderContact() {
      const { data } = await api.get("/settings/lender-contact");
      return data;
    },
    async updateBusinessContact(payload) {
      const { data } = await api.put("/settings/lender-contact", payload);
      return data;
    },
    async fetchBlacklist(params = {}) {
      const { data } = await api.get("/blacklist", { params });
      this.applyPagedResult("blacklist", "blacklistPage", data);
    },
    async fetchRepayments(loanId, params = {}) {
      const url = loanId != null ? `/repayments/${loanId}` : "/repayments";
      const { data } = await api.get(url, { params });
      this.applyPagedResult("repayments", "repaymentsPage", data);
    },
    async fetchMyNotifications(params = {}) {
      const { data } = await api.get("/notifications/me", { params });
      this.applyPagedResult("notifications", "notificationsPage", data);
      return data;
    },
    async markNotificationRead(id) {
      await api.post(`/notifications/${id}/read`);
    },
    async fetchLoanInterestSettings() {
      const { data } = await api.get("/settings/loan-interest");
      return data;
    },
    async fetchExpectedAmountAtEndOfTerm(principal = null) {
      const params = principal != null ? { principal: Number(principal) } : {};
      const { data } = await api.get("/settings/loan-interest/expected-amount", { params });
      return data;
    },
    async updateLoanInterestSettings(payload) {
      const { data } = await api.put("/settings/loan-interest", payload);
      return data;
    },
    async fetchBusinessCapitalBalance() {
      const { data } = await api.get("/auth/business-capital");
      return data.balance;
    },
    async fetchBusinessCapitalSummary() {
      const { data } = await api.get("/auth/business-capital");
      return data;
    },
    async topUpBusinessCapital(amount) {
      await api.post("/auth/business-capital/top-up", { amount: Number(amount) });
    },
    async resetHistory() {
      const { data } = await api.post("/admin/reset-history");
      return data;
    }
  },
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isOwner: (state) => state.role === "OWNER",
    isCashier: (state) => state.role === "CASHIER",
    isBorrower: (state) => state.role === "BORROWER"
  }
});
