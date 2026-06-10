/**
 * Pinia store for API mode (mock | real) and base URL configuration.
 *
 * <p>Persists mode selection to localStorage for cross-session consistency.
 * Reads VITE_API_MODE and VITE_API_BASE_URL as initial fallback values.</p>
 */
import { defineStore } from "pinia";
import { computed, ref } from "vue";

const LS_KEY = "mactav-api-mode";

export type ApiMode = "mock" | "real";

function loadInitialMode(): ApiMode {
  const stored = localStorage.getItem(LS_KEY);
  if (stored === "mock" || stored === "real") return stored;
  const env = import.meta.env.VITE_API_MODE;
  if (env === "mock" || env === "real") return env;
  return "mock";
}

export const useApiModeStore = defineStore("apiMode", () => {
  const mode = ref<ApiMode>(loadInitialMode());
  const baseUrl = ref<string>(import.meta.env.VITE_API_BASE_URL ?? "");

  const isMock = computed(() => mode.value === "mock");
  const isReal = computed(() => mode.value === "real");

  function setMode(next: ApiMode): void {
    mode.value = next;
    localStorage.setItem(LS_KEY, next);
  }

  return { mode, baseUrl, isMock, isReal, setMode };
});
