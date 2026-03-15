<template>
  <v-stepper v-model="step">
    <v-stepper-header>
      <v-stepper-item :value="1" editable>Details</v-stepper-item>
      <v-stepper-item :value="2" editable>Location</v-stepper-item>
      <v-stepper-item :value="3" editable>ID PDF</v-stepper-item>
      <v-stepper-item :value="4" editable>Selfie</v-stepper-item>
      <v-stepper-item :value="5" editable>Review</v-stepper-item>
    </v-stepper-header>

    <v-stepper-window>
      <v-stepper-window-item :value="1">
        <BorrowerStepDetails :form="form" @next="step++" />
      </v-stepper-window-item>
      <v-stepper-window-item :value="2">
        <BorrowerStepLocation :form="form" @next="step++" />
      </v-stepper-window-item>
      <v-stepper-window-item :value="4">
        <BorrowerStepSelfie :form="form" @next="step++" />
      </v-stepper-window-item>
    </v-stepper-window>
  </v-stepper>
</template>

<script setup>
import { ref, reactive } from "vue";
import BorrowerStepDetails from "./BorrowerStepDetails.vue";
import BorrowerStepLocation from "./BorrowerStepLocation.vue";
import BorrowerStepSelfie from "./BorrowerStepSelfie.vue";

const step = ref(1);
const form = reactive({
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
</script>