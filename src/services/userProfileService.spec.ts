import { describe, expect, it, vi } from 'vitest';

import type { AuthSession } from '@/types/auth';
import { createAuthStorage, type StorageLike } from './authStorage';
import { createUserProfileService } from './userProfileService';

const createMemoryStorage = (): StorageLike => {
  const values = new Map<string, unknown>();
  return {
    get: (key) => values.get(key),
    set: (key, value) => values.set(key, value),
    remove: (key) => values.delete(key)
  };
};

const session: AuthSession = {
  token: 'profile-token',
  expiresIn: 7200,
  expiresAt: Date.now() + 60_000,
  user: {
    id: 42,
    nickname: null,
    avatarUrl: null,
    profileCompleted: false,
    roles: ['USER'],
    permissions: []
  }
};

describe('userProfileService', () => {
  /**
   * 验证微信临时头像会先上传，再将返回的长期地址用于资料保存。
   */
  it('uploads the chosen avatar with token and persists the updated profile', async () => {
    const storage = createAuthStorage(createMemoryStorage());
    storage.saveSession(session);
    const upload = vi.fn().mockResolvedValue({
      avatarUrl: 'http://127.0.0.1:8080/uploads/avatars/42/avatar.jpg'
    });
    const put = vi.fn().mockResolvedValue({
      id: 42,
      nickname: '存魔用户',
      avatarUrl: 'http://127.0.0.1:8080/uploads/avatars/42/avatar.jpg',
      profileCompleted: true
    });
    const service = createUserProfileService({ upload, put, storage });

    const avatarUrl = await service.uploadAvatar('wxfile://temporary-avatar');
    const user = await service.updateProfile({
      nickname: '存魔用户',
      avatarUrl
    });

    expect(upload).toHaveBeenCalledWith(
      '/api/users/me/avatar',
      'wxfile://temporary-avatar',
      'profile-token'
    );
    expect(put).toHaveBeenCalledWith('/api/users/me/profile', {
      nickname: '存魔用户',
      avatarUrl
    });
    expect(user).toMatchObject({
      nickname: '存魔用户',
      profileCompleted: true,
      roles: ['USER']
    });
    expect(storage.getSession()?.user).toEqual(user);
  });
});
