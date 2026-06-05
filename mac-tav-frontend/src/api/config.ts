export const apiMode = (import.meta.env.VITE_API_MODE ?? 'mock') as 'mock' | 'real';
export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '';
