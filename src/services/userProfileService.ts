import type {
  AuthUser,
  AvatarUploadResponse,
  ProfileResponse,
  ProfileUpdateInput
} from '@/types/auth';
import type { AuthStorage } from './authStorage';
import { authStorage } from './authStorage';
import { authHttpClient, type RequestFailure } from './httpClient';
import { AuthError } from './wechatAuthService';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

interface ProfileDependencies {
  upload: (
    url: string,
    filePath: string,
    token: string
  ) => Promise<AvatarUploadResponse>;
  put: <T>(url: string, data: unknown) => Promise<T>;
  storage: AuthStorage;
}

const toProfileError = (error: unknown): AuthError => {
  if (error instanceof AuthError) return error;
  const failure = error as RequestFailure;
  const response = failure?.data as
    | { code?: string; message?: string }
    | undefined;
  return new AuthError(
    response?.code || 'PROFILE_UPDATE_FAILED',
    response?.message || failure?.errMsg || '用户资料保存失败，请稍后重试',
    failure?.statusCode
  );
};

const uploadAvatarFile = (
  url: string,
  filePath: string,
  token: string
): Promise<AvatarUploadResponse> =>
  new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${API_BASE_URL}${url}`,
      filePath,
      name: 'file',
      header: {
        Authorization: `Bearer ${token}`
      },
      success: (response) => {
        let data: unknown;
        try {
          data =
            typeof response.data === 'string'
              ? JSON.parse(response.data)
              : response.data;
        } catch {
          reject({
            statusCode: response.statusCode,
            errMsg: '头像上传响应无法解析'
          } satisfies RequestFailure);
          return;
        }
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(data as AvatarUploadResponse);
          return;
        }
        reject({
          statusCode: response.statusCode,
          data
        } satisfies RequestFailure);
      },
      fail: reject
    });
  });

export const createUserProfileService = ({
  upload,
  put,
  storage
}: ProfileDependencies) => ({
  /**
   * 上传 chooseAvatar 返回的微信临时头像文件。
   */
  async uploadAvatar(filePath: string): Promise<string> {
    const session = storage.getSession();
    if (!session) {
      throw new AuthError('UNAUTHORIZED', '登录状态已失效，请重新登录', 401);
    }
    try {
      const response = await upload(
        '/api/users/me/avatar',
        filePath,
        session.token
      );
      return response.avatarUrl;
    } catch (error) {
      throw toProfileError(error);
    }
  },

  /**
   * 保存昵称和长期头像地址，并同步更新本地登录态。
   */
  async updateProfile(input: ProfileUpdateInput): Promise<AuthUser> {
    const session = storage.getSession();
    if (!session) {
      throw new AuthError('UNAUTHORIZED', '登录状态已失效，请重新登录', 401);
    }
    try {
      const profile = await put<ProfileResponse>(
        '/api/users/me/profile',
        input
      );
      const user: AuthUser = {
        ...session.user,
        ...profile
      };
      storage.saveSession({
        ...session,
        user
      });
      return user;
    } catch (error) {
      throw toProfileError(error);
    }
  }
});

export const userProfileService = createUserProfileService({
  upload: uploadAvatarFile,
  put: <T>(url: string, data: unknown) =>
    authHttpClient.request<T>({ url, method: 'PUT', data }),
  storage: authStorage
});
