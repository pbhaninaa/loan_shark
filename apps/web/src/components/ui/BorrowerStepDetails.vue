<template>
  <v-form @submit.prevent="$emit('next')">
    <v-row>
      <v-col cols="12" md="6">
        <AppTextField v-model="form.username" label="Username" required />
      </v-col>
      <v-col cols="12" md="6">
        <AppTextField v-model="form.password" label="Password" type="password" required />
      </v-col>
      <!-- ...rest of personal details fields -->
      <v-col cols="12">
        <v-alert :type="saIdValid ? 'success' : 'warning'">
          {{ saIdValid ? "Valid SA ID" : "Enter a valid 13-digit South African ID" }}
        </v-alert>
      </v-col>
    </v-row>
    <div class="d-flex justify-end">
      <AppActionButton type="submit" text="Next: Location" />
    </div>
  </v-form>
</template>

<script setup>
import { computed } from "vue";
const props = defineProps({
  form: Object
});
const saIdValid = computed(() => {
  const id = props.form.idNumber || "";
  return /^\d{13}$/.test(id);
});
</script>