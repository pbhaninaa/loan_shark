<template>
  <div class="page-shell">
    <AppPageHeader
      title="User Management"
      description="The owner can create, update, disable, and delete any system user from this page."
    >
      <template #actions>
        <v-chip color="primary" variant="tonal" size="large">Owner only</v-chip>
        <AppActionButton text="Create User" prepend-icon="mdi-account-plus-outline" @click="openCreateDialog" />
      </template>
    </AppPageHeader>

    <v-alert v-if="message" type="success" variant="tonal" class="mb-4">
      {{ message }}
    </v-alert>
    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <AppTableCard title="All Users" :count-label="`${users.length} users`">
      <template #header-actions>
        <AppSearchField v-model="search" label="Search users" style="min-width: 240px;" @update:model-value="handleSearch" />
      </template>
        <v-table>
          <thead>
            <tr>
              <th>Username</th>
              <th>Role</th>
              <th>Status</th>
              <th>Client Link</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.username }}</td>
              <td>
                <v-chip color="info" size="small" variant="tonal">{{ user.role }}</v-chip>
              </td>
              <td>
                <v-chip :color="user.status === 'ACTIVE' ? 'success' : 'warning'" size="small" variant="tonal">
                  {{ user.status }}
                </v-chip>
              </td>
              <td>{{ user.borrowerId ? `Client #${user.borrowerId}` : "-" }}</td>
              <td>{{ formatDate(user.createdAt) }}</td>
              <td>
                <div class="d-flex ga-2 flex-wrap">
                  <AppActionButton size="small" variant="tonal" text="Edit" @click="startEdit(user)" />
                  <AppActionButton size="small" variant="tonal" text="Reset password" prepend-icon="mdi-lock-reset" @click="openResetPasswordDialog(user)" />
                  <AppActionButton size="small" color="error" variant="tonal" text="Delete" @click="removeUser(user.id)" />
                </div>
              </td>
            </tr>
          </tbody>
        </v-table>
      <AppPaginationFooter v-model="page" :total-pages="usersPage.totalPages" :total-elements="usersPage.totalElements" @update:model-value="loadUsers" />
    </AppTableCard>

    <AppDialogCard v-model="showFormDialog" :title="editingUserId ? 'Edit User' : 'Create User'">
      <v-form @submit.prevent="saveUser">
            <AppTextField
              v-model="userForm.username"
              label="Username"
              prepend-inner-icon="mdi-account-outline"
              required
            />
            <AppTextField
              v-model="userForm.password"
              :label="editingUserId ? 'Password (leave blank to keep)' : 'Password'"
              type="password"
              prepend-inner-icon="mdi-lock-outline"
            />
            <AppSelectField v-model="userForm.role" label="Role" :items="roleOptions" />
            <AppSelectField v-model="userForm.status" label="Status" :items="statusOptions" />
            <div class="d-flex ga-2">
              <AppActionButton :text="editingUserId ? 'Update User' : 'Create User'" type="submit" class="flex-1-1" />
              <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="closeDialog" />
            </div>
      </v-form>
    </AppDialogCard>

    <AppDialogCard v-model="showResetPasswordDialog" title="Reset password" :max-width="440">
      <template v-if="resetPasswordUser">
        <p class="text-body-2 text-medium-emphasis mb-3">Set a new password for <strong>{{ resetPasswordUser.username }}</strong>.</p>
        <v-form @submit.prevent="submitResetPassword">
          <AppTextField
            v-model="resetPasswordForm.newPassword"
            label="New password"
            type="password"
            prepend-inner-icon="mdi-lock-outline"
            required
          />
          <AppTextField
            v-model="resetPasswordForm.confirmPassword"
            label="Confirm new password"
            type="password"
            prepend-inner-icon="mdi-lock-outline"
            :error-messages="resetPasswordForm.confirmPassword && resetPasswordForm.newPassword !== resetPasswordForm.confirmPassword ? ['Passwords do not match'] : []"
          />
          <div class="d-flex ga-2 mt-3">
            <AppActionButton text="Reset password" type="submit" :loading="resetPasswordLoading" />
            <AppActionButton text="Cancel" color="secondary" variant="tonal" @click="closeResetPasswordDialog" />
          </div>
        </v-form>
      </template>
    </AppDialogCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import AppActionButton from "../components/ui/AppActionButton.vue";
import AppDialogCard from "../components/ui/AppDialogCard.vue";
import AppPaginationFooter from "../components/ui/AppPaginationFooter.vue";
import AppPageHeader from "../components/ui/AppPageHeader.vue";
import AppSearchField from "../components/ui/AppSearchField.vue";
import AppSelectField from "../components/ui/AppSelectField.vue";
import AppTableCard from "../components/ui/AppTableCard.vue";
import AppTextField from "../components/ui/AppTextField.vue";
import { useAppStore } from "../store";

const store = useAppStore();
const message = ref("");
const error = ref("");
const editingUserId = ref(null);
const showFormDialog = ref(false);
const showResetPasswordDialog = ref(false);
const resetPasswordUser = ref(null);
const resetPasswordForm = ref({ newPassword: "", confirmPassword: "" });
const resetPasswordLoading = ref(false);
const search = ref("");
const page = ref(0);

const roleOptions = ["OWNER", "CASHIER"];
const statusOptions = ["ACTIVE", "DISABLED"];

const initialUserForm = () => ({
  username: "",
  password: "",
  role: "CASHIER",
  status: "ACTIVE"
});

const userForm = reactive(initialUserForm());
const users = computed(() => store.users);
const usersPage = computed(() => store.usersPage);

onMounted(async () => {
  await loadUsers();
});

async function saveUser() {
  message.value = "";
  error.value = "";
  try {
    if (editingUserId.value) {
      await store.updateUser(editingUserId.value, userForm);
      message.value = "User updated successfully.";
    } else {
      await store.createUser(userForm);
      message.value = "User created successfully.";
    }
    closeDialog();
    await loadUsers();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not save user";
  }
}

function openCreateDialog() {
  resetForm();
  showFormDialog.value = true;
}

function startEdit(user) {
  editingUserId.value = user.id;
  userForm.username = user.username;
  userForm.password = "";
  userForm.role = user.role;
  userForm.status = user.status;
  showFormDialog.value = true;
}

function resetForm() {
  editingUserId.value = null;
  Object.assign(userForm, initialUserForm());
}

function closeDialog() {
  showFormDialog.value = false;
  resetForm();
}

async function removeUser(id) {
  message.value = "";
  error.value = "";
  try {
    await store.deleteUser(id);
    message.value = "User deleted successfully.";
    await loadUsers();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not delete user";
  }
}

function formatDate(value) {
  return value ? new Date(value).toLocaleDateString() : "-";
}

async function loadUsers(nextPage = page.value) {
  page.value = nextPage;
  await store.fetchUsers({ q: search.value, page: page.value, size: 8 });
}

async function handleSearch() {
  page.value = 0;
  await loadUsers(0);
}

function openResetPasswordDialog(user) {
  resetPasswordUser.value = user;
  resetPasswordForm.value = { newPassword: "", confirmPassword: "" };
  error.value = "";
  showResetPasswordDialog.value = true;
}

function closeResetPasswordDialog() {
  resetPasswordUser.value = null;
  resetPasswordForm.value = { newPassword: "", confirmPassword: "" };
  showResetPasswordDialog.value = false;
}

async function submitResetPassword() {
  if (!resetPasswordUser.value) return;
  if (!resetPasswordForm.value.newPassword || resetPasswordForm.value.newPassword.length < 4) {
    error.value = "New password must be at least 4 characters.";
    return;
  }
  if (resetPasswordForm.value.newPassword !== resetPasswordForm.value.confirmPassword) {
    error.value = "Passwords do not match.";
    return;
  }
  resetPasswordLoading.value = true;
  error.value = "";
  try {
    await store.resetUserPassword(resetPasswordUser.value.id, resetPasswordForm.value.newPassword);
    message.value = "Password reset successfully.";
    closeResetPasswordDialog();
  } catch (requestError) {
    error.value = requestError.response?.data?.message || "Could not reset password.";
  } finally {
    resetPasswordLoading.value = false;
  }
}
</script>
