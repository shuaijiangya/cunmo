import type { AuthStorage } from './authStorage';
import { authStorage } from './authStorage';

export interface RequestOptions {
  url: string;
  method?: string;
  data?: unknown;
  header?: Record<string, string>;
}

export interface RequestFailure {
  statusCode?: number;
  data?: unknown;
  errMsg?: string;
}

type RequestAdapter = <T>(options: RequestOptions) => Promise<T>;

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

/**
 * 将 uni.request 包装为按 HTTP 状态码处理成功和失败的 Promise。
 */
const uniRequest: RequestAdapter = <T>(options: RequestOptions) =>
  new Promise<T>((resolve, reject) => {
    uni.request({
      ...options,
      success: (response) => {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as T);
          return;
        }
        reject({
          statusCode: response.statusCode,
          data: response.data
        } satisfies RequestFailure);
      },
      fail: reject
    });
  });

/**
 * 创建自动携带登录 Token 并统一处理 401 的 HTTP 客户端。
 */
export const createAuthHttpClient = ({
  request,
  storage,
  onUnauthorized
}: {
  request: RequestAdapter;
  storage: AuthStorage;
  onUnauthorized?: () => void;
}) => ({
  /**
   * 发起业务请求，并在登录失效时清理本地会话和通知页面。
   */
  async request<T>(options: RequestOptions): Promise<T> {
    const session = storage.getSession();
    const header = { ...options.header };
    if (session) header.Authorization = `Bearer ${session.token}`;

    try {
      return await request<T>({ ...options, header });
    } catch (error) {
      if ((error as RequestFailure)?.statusCode === 401) {
        storage.clearSession();
        try {
          onUnauthorized?.();
        } catch {
          // Preserve the original HTTP failure even if UI cleanup fails.
        }
      }
      throw error;
    }
  }
});

let unauthorizedHandler: (() => void) | undefined;

/**
 * 注册或移除全局未授权处理函数。
 */
export const setUnauthorizedHandler = (
  handler: (() => void) | undefined
) => {
  unauthorizedHandler = handler;
};

export const authHttpClient = createAuthHttpClient({
  request: <T>(options: RequestOptions) =>
    uniRequest<T>({
      ...options,
      url: `${API_BASE_URL}${options.url}`
    }),
  storage: authStorage,
  onUnauthorized: () => unauthorizedHandler?.()
});
