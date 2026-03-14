<template>
  <v-card class="pa-4 mb-4">
    <v-file-input
      v-model="idFile"
      accept="application/pdf"
      label="ID copy (PDF only)"
      prepend-icon="mdi-file-pdf-box"
      show-size
      hint="Upload a clear PDF copy of your South African ID."
      persistent-hint
      @change="uploadFile"
    />

    <div v-if="idUrl" class="mt-2">
      Uploaded ID URL: 
      <a :href="idUrl" target="_blank">{{ idUrl }}</a>
    </div>
  </v-card>
</template>

<script setup>
import { ref, inject } from "vue";
import axios from "axios";

const borrowerForm = inject("borrowerForm"); // shared object from parent

const idFile = ref(null);
const idUrl = ref("");

async function uploadFile() {
  if (!idFile.value) return;

  const formData = new FormData();
  formData.append("idDocument", idFile.value);

  try {
    const response = await axios.post("https://your-server.com/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" }
    });
    idUrl.value = response.data.idDocUrl;
    borrowerForm.idDocumentUrl = idUrl.value;
    console.log("ID uploaded:", response.data);
  } catch (err) {
    console.error("ID upload failed:", err);
    alert("Failed to upload ID. Try again.");
  }
}
</script>

<style scoped>
</style>