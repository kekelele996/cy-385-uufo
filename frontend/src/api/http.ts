import type { ApiErrorBody } from '../types';

const BASE = '/api';

async function parseBody(res: Response): Promise<unknown> {
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...(options.headers ?? {}) },
    ...options,
  });
  const body = await parseBody(res);
  if (!res.ok) {
    const errBody = body as Partial<ApiErrorBody> | null;
    throw new Error(errBody?.message || `请求失败（${res.status}）`);
  }
  return body as T;
}
