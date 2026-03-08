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
    actions: [],
    actionsPage: { page: 0, size: 5, totalElements: 0, totalPages: 0 },
    borrowers: [],
    borrowersPage: { page: 0, size: 8, totalElements: 0, totalPages: 0 },
    users: [],
    usersPage: { page: 0, size: 8, totalElements: 0, totalPages: 0 },
    verifications: [],
    loans: [],
    loansPage: { page: 0, size: 8, totalElements: 0, totalPages: 0 },
    blacklist: [],
    blacklistPage: { page: 0, size: 8, totalElements: 0, totalPages: 0 },
    repayments: [],
    repaymentsPage: { page: 0, size: 8, totalElements: 0, totalPages: 0 },
    notifications: [],
    notificationsPage: { page: 0, size: 8, totalElements: 0, totalPages: 0 },
    borrowerProfile: null,
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
      this.actions = [];
      this.borrowerProfile = null;
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
    async login(credentials) {
      const { data } = await api.post("/auth/login", credentials);
      this.setSession(data);
      return data;
    },
    async forgotPassword(username) {
      const { data } = await api.post("/auth/forgot-password", { username });
      return data;
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
    async fetchMyLoans(params = {}) {
      const { data } = await api.get("/loans/my", { params });
      this.applyPagedResult("loans", "loansPage", data);
    },
    async fetchMyBorrower() {
      const { data } = await api.get("/borrowers/me");
      this.borrowerProfile = data;
      this.setBorrowerStatus(data.status);
      return data;
    },
    async fetchMyVerification() {
      const { data } = await api.get("/verifications/me");
      this.verification = data;
      return data;
    },
    async fetchLoanSchedule(loanId) {
      const { data } = await api.get(`/loans/${loanId}/schedule`);
      this.loanSchedule = data;
      return data;
    },
    async fetchLenderContact() {
      const { data } = await api.get("/settings/lender-contact");
      return data;
    },
    async fetchBlacklist(params = {}) {
      const { data } = await api.get("/blacklist", { params });
      this.applyPagedResult("blacklist", "blacklistPage", data);
    },
    async fetchRepayments(loanId, params = {}) {
      const { data } = await api.get(`/repayments/${loanId}`, { params });
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
    }
  },
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isOwner: (state) => state.role === "OWNER",
    isCashier: (state) => state.role === "CASHIER",
    isBorrower: (state) => state.role === "BORROWER"
  }
});
