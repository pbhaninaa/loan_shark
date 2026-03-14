<template>
  <v-card class="pa-4">
    <div>{{ instruction }}</div>

    <video v-if="!selfiePreviewUrl" ref="videoRef" autoplay playsinline muted class="w-100" />
    <v-img v-else :src="selfiePreviewUrl" class="w-100" height="220" cover />

    <div v-if="selfieUrl" class="mt-2">
      Uploaded Selfie URL:
      <a :href="selfieUrl" target="_blank">{{ selfieUrl }}</a>
    </div>
  </v-card>
</template>

<script setup>
import { ref, onMounted } from "vue";
import * as faceLandmarksDetection from "@tensorflow-models/face-landmarks-detection";
import axios from "axios";

const props = defineProps({ form: Object });
const videoRef = ref(null);
const selfiePreviewUrl = ref("");
const selfieUrl = ref("");
const faceModel = ref(null);
const instruction = ref("Look at the camera");

const blinkDetected = ref(false);
const headTurnDetected = ref(false);
const smileDetected = ref(false);

async function loadFaceModel() {
  faceModel.value = await faceLandmarksDetection.createDetector(
    faceLandmarksDetection.SupportedModels.MediaPipeFaceMesh,
    { runtime: "mediapipe", solutionPath: "https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh" }
  );
}

async function startCamera() {
  const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "user" } });
  videoRef.value.srcObject = stream;
  await loadFaceModel();
  detectLoop();
}

let running = true;
async function detectLoop() {
  if (!running || !faceModel.value) return;

  const predictions = await faceModel.value.estimateFaces(videoRef.value);
  if (predictions.length > 0) {
    const landmarks = predictions[0].scaledMesh;
    detectBlink(landmarks);
    detectHeadTurn(landmarks);
    detectSmile(landmarks);
    updateInstruction();
    if (blinkDetected.value && headTurnDetected.value && smileDetected.value) {
      captureSelfie();
      return;
    }
  }
  requestAnimationFrame(detectLoop);
}

function detectBlink(landmarks) {
  const eyeOpen = Math.abs(landmarks[159][1] - landmarks[145][1]);
  if (eyeOpen < 3.5) blinkDetected.value = true;
}
function detectHeadTurn(landmarks) {
  if (landmarks[1][0] < landmarks[33][0]) headTurnDetected.value = true;
}
function detectSmile(landmarks) {
  if (Math.abs(landmarks[291][0] - landmarks[61][0]) > 60) smileDetected.value = true;
}
function updateInstruction() {
  if (!blinkDetected.value) instruction.value = "Please blink";
  else if (!headTurnDetected.value) instruction.value = "Turn your head left";
  else if (!smileDetected.value) instruction.value = "Smile";
  else instruction.value = "Capturing selfie...";
}

function captureSelfie() {
  const canvas = document.createElement("canvas");
  canvas.width = videoRef.value.videoWidth;
  canvas.height = videoRef.value.videoHeight;
  canvas.getContext("2d").drawImage(videoRef.value, 0, 0);

  canvas.toBlob(async (blob) => {
    const file = new File([blob], "selfie.jpg", { type: "image/jpeg" });
    selfiePreviewUrl.value = URL.createObjectURL(blob);

    // Stop camera
    videoRef.value.srcObject.getTracks().forEach(t => t.stop());

    // Upload
    const formData = new FormData();
    formData.append("selfieImage", file);

    try {
      const response = await axios.post("https://your-server.com/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" }
      });
      selfieUrl.value = response.data.selfieUrl;
      props.form.selfieUrl = selfieUrl.value;
      console.log("Selfie uploaded:", response.data);
    } catch (err) {
      console.error("Selfie upload failed:", err);
      alert("Failed to upload selfie.");
    }
  }, "image/jpeg", 0.92);
}

onMounted(startCamera);
</script>

<style scoped>
video {
  border-radius: 8px;
}
</style>