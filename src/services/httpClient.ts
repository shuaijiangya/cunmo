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

export const createAuthHttpClient = ({
  request,
  storage
}: {
  request: RequestAdapter;
  storage: AuthStorage;
}) => ({
  async request<T>(options: RequestOptions): Promise<T> {
    const session = storage.getSession();
    const header = { ...options.header };
    if (session) header.Authorization = `Bearer ${session.token}`;

    try {
      return await request<T>({ ...options, header });
    } catch (error) {
      if ((error as RequestFailure)?.statusCode === 401) {
        storage.clearSession();
      }
      throw error;
    }
  }
});

export const authHttpClient = createAuthHttpClient({
  request: <T>(options: RequestOptions) =>
    uniRequest<T>({
      ...options,
      url: `${API_BASE_URL}${options.url}`
    }),
  storage: authStorage
});
