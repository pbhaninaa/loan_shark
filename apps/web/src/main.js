import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import vuetify from "./plugins/vuetify";
import { useAppStore } from "./store";
import "vuetify/styles";
import "@mdi/font/css/materialdesignicons.css";
import "./styles/main.css";

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);
app.use(vuetify);
app.mount("#app");

// When token expires or 401, clear session and ensure user is logged out
window.addEventListener("auth-logout", () => {
  useAppStore().clearSession();
});
