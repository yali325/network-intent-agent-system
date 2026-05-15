import type {
  AgentStepLog,
  ApiResult,
  ConfigSet,
  ExecutionReport,
  NetworkIntent,
  NetworkPlan,
  NetworkWorkspace,
  ValidationReport
} from '@/types';
import http from './http';

const unwrap = async <T>(request: Promise<{ data: ApiResult<T> }>): Promise<T> => {
  const { data: response } = await request;
  if (!response.success) {
    throw new Error(response.message || response.code || '请求失败');
  }
  return response.data;
};

export const runDemoTask = (rawText: string) =>
  unwrap<NetworkWorkspace>(http.post('/demo/tasks', { rawText }));

export const getWorkspace = (taskId: string) =>
  unwrap<NetworkWorkspace>(http.get(`/tasks/${taskId}`));

export const getIntent = (taskId: string) =>
  unwrap<NetworkIntent>(http.get(`/tasks/${taskId}/intent`));

export const getPlan = (taskId: string) =>
  unwrap<NetworkPlan>(http.get(`/tasks/${taskId}/plan`));

export const getConfig = (taskId: string) =>
  unwrap<ConfigSet>(http.get(`/tasks/${taskId}/config`));

export const getExecution = (taskId: string) =>
  unwrap<ExecutionReport>(http.get(`/tasks/${taskId}/execution`));

export const getValidation = (taskId: string) =>
  unwrap<ValidationReport>(http.get(`/tasks/${taskId}/validation`));

export const getLogs = (taskId: string) =>
  unwrap<AgentStepLog[]>(http.get(`/tasks/${taskId}/logs`));
