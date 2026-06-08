import { authHttpClient, type RequestOptions } from './httpClient';

type RequestAdapter = <T>(options: RequestOptions) => Promise<T>;

/**
 * 创建调用当前 Token 退出接口的服务。
 */
export const createLogoutService = (request: RequestAdapter) => ({
  /**
   * 注销当前请求携带的业务 Token。
   */
  logout: () =>
    request<void>({
      url: '/api/auth/logout',
      method: 'POST'
    })
});

export const logoutService = createLogoutService((options) =>
  authHttpClient.request(options)
);
