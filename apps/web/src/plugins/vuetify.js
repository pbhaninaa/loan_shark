import { createVuetify } from "vuetify";

export default createVuetify({
  theme: {
    defaultTheme: "loanSharkTheme",
    themes: {
      loanSharkTheme: {
        dark: false,
        colors: {
          primary: "#1e40af",
          secondary: "#0f172a",
          accent: "#14b8a6",
          info: "#2563eb",
          success: "#16a34a",
          warning: "#f59e0b",
          error: "#dc2626",
          background: "#f5f7fb",
          surface: "#ffffff"
        }
      }
    }
  },
  defaults: {
    VCard: {
      rounded: "xl",
      elevation: 2
    },
    VBtn: {
      rounded: "lg",
      variant: "flat"
    },
    VTextField: {
      variant: "outlined",
      density: "comfortable"
    },
    VSelect: {
      variant: "outlined",
      density: "comfortable"
    },
    VTextarea: {
      variant: "outlined",
      density: "comfortable"
    }
  }
});
