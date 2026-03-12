<template>
  <v-card class="pa-4">
    <v-alert :type="locationAlertType">{{ locationLabel }}</v-alert>
    <div class="d-flex gap-2 mt-4">
      <AppActionButton text="Retry location" @click="captureLocation" />
      <AppActionButton text="Next: ID PDF" :disabled="!locationCaptured" @click="$emit('next')" />
    </div>
  </v-card>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
const props = defineProps({ form: Object });
const emit = defineEmits(["next"]);

const locationCaptured = computed(() => !!props.form.latitude && !!props.form.longitude && !!props.form.locationName);
const locationAlertType = computed(() => locationCaptured.value ? "success" : "info");
const locationLabel = computed(() => locationCaptured.value ? props.form.locationName : "Waiting for location...");

async function captureLocation() {
  if (!navigator.geolocation) return;
  navigator.geolocation.getCurrentPosition((pos) => {
    props.form.latitude = pos.coords.latitude;
    props.form.longitude = pos.coords.longitude;
    props.form.locationName = `${props.form.latitude},${props.form.longitude}`;
  });
}

onMounted(() => {
  captureLocation();
});
</script>