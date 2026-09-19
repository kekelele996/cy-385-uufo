/** 统一请求封装：返回 data，失败时抛出带后端 message 的错误 */
export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  let body: unknown = null;
  try {
    body = await res.json();
  } catch {
    /* 非 JSON 响应 */
  }
  if (!res.ok || (body as { success?: boolean } | null)?.success === false) {
    const message =
      (body as { message?: string } | null)?.message || `请求失败（${res.status}）`;
    throw new Error(message);
  }
  return body as T;
}
