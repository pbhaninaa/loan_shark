import { createVuetify } from "vuetify";

const lightTheme = {
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
};

const darkTheme = {
  dark: true,
  colors: {
    primary: "#60a5fa",
    secondary: "#1e293b",
    accent: "#2dd4bf",
    info: "#3b82f6",
    success: "#22c55e",
    warning: "#fbbf24",
    error: "#ef4444",
    background: "#0f172a",
    surface: "#1e293b"
  }
};

export default createVuetify({
  theme: {
    defaultTheme: "loanSharkTheme",
    themes: {
      loanSharkTheme: lightTheme,
      loanSharkDark: darkTheme
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
