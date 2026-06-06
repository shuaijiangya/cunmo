import type {
  ApiErrorResponse,
  AuthLoginResponse,
  AuthSession
} from '@/types/auth';
import type { AuthStorage } from './authStorage';
import { authStorage } from './authStorage';
import { authHttpClient, type RequestFailure } from './httpClient';

interface WechatLoginResult {
  code: string;
}

interface LoginDependencies {
  login: () => Promise<WechatLoginResult>;
  post: <T>(url: string, data: unknown) => Promise<T>;
  storage: AuthStorage;
}

export class AuthError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly statusCode?: number
  ) {
    super(message);
    this.name = 'AuthError';
  }
}

const toAuthError = (error: unknown): AuthError => {
  if (error instanceof AuthError) return error;
  const failure = error as RequestFailure;
  const response = failure?.data as Partial<ApiErrorResponse> | undefined;
  if (response?.code && response.message) {
    return new AuthError(response.code, response.message, failure.statusCode);
  }
  return new AuthError(
    'AUTH_REQUEST_FAILED',
    failure?.errMsg || '登录服务暂时不可用，请稍后重试',
    failure?.statusCode
  );
};

const uniLogin = (): Promise<WechatLoginResult> =>
  new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: ({ code }) => resolve({ code }),
      fail: reject
    });
  });

export const createWechatAuthService = ({
  login,
  post,
  storage
}: LoginDependencies) => ({
  async login(): Promise<AuthSession> {
    try {
      const result = await login();
      if (!result.code) {
        throw new AuthError('WECHAT_LOGIN_FAILED', '未能取得微信登录凭证');
      }
      const response = await post<AuthLoginResponse>(
        '/api/auth/wechat/login',
        { code: result.code }
      );
      const session: AuthSession = {
        ...response,
        expiresAt: Date.now() + response.expiresIn * 1000
      };
      storage.saveSession(session);
      return session;
    } catch (error) {
      throw toAuthError(error);
    }
  },
  restore(): AuthSession | null {
    return storage.getSession();
  },
  logout(): void {
    storage.clearSession();
  }
});

export const wechatAuthService = createWechatAuthService({
  login: uniLogin,
  post: <T>(url: string, data: unknown) =>
    authHttpClient.request<T>({ url, method: 'POST', data }),
  storage: authStorage
});
