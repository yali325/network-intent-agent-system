/**
 * @deprecated Use useApiModeStore from @/stores/apiModeStore instead.
 * apiBaseUrl is retained for backward compatibility; new code should read
 * useApiModeStore().baseUrl.
 */
export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "";
