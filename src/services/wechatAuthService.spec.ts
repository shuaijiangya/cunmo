import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { AuthLoginResponse, AuthSession } from '@/types/auth';
import { createAuthStorage, type StorageLike } from './authStorage';
import { createAuthHttpClient } from './httpClient';
import { AuthError, createWechatAuthService } from './wechatAuthService';

const loginResponse: AuthLoginResponse = {
  token: 'business-token',
  expiresIn: 7200,
  user: {
    id: 10001,
    nickname: null,
    avatarUrl: null,
    profileCompleted: false,
    roles: ['USER'],
    permissions: ['inventory:item:read']
  }
};

const createMemoryStorage = (): StorageLike => {
  const values = new Map<string, unknown>();
  return {
    get: (key) => values.get(key),
    set: (key, value) => values.set(key, value),
    remove: (key) => values.delete(key)
  };
};

describe('wechatAuthService', () => {
  let storageLike: StorageLike;

  beforeEach(() => {
    storageLike = createMemoryStorage();
  });

  it('exchanges wx.login code and persists the returned session', async () => {
    const post = vi.fn().mockResolvedValue(loginResponse);
    const storage = createAuthStorage(storageLike);
    const service = createWechatAuthService({
      login: vi.fn().mockResolvedValue({ code: 'temporary-code' }),
      post,
      storage
    });

    const session = await service.login();

    expect(post).toHaveBeenCalledWith('/api/auth/wechat/login', { code: 'temporary-code' });
    expect(session.token).toBe('business-token');
    expect(storage.getSession()).toEqual(session);
  });

  it('rejects when wx.login does not return a code', async () => {
    const service = createWechatAuthService({
      login: vi.fn().mockResolvedValue({ code: '' }),
      post: vi.fn(),
      storage: createAuthStorage(storageLike)
    });

    await expect(service.login()).rejects.toMatchObject({
      code: 'WECHAT_LOGIN_FAILED'
    } satisfies Partial<AuthError>);
  });

  it('maps backend business errors to a stable AuthError', async () => {
    const service = createWechatAuthService({
      login: vi.fn().mockResolvedValue({ code: 'expired-code' }),
      post: vi.fn().mockRejectedValue({
        statusCode: 401,
        data: { code: 'INVALID_CODE', message: '登录凭证无效或已过期' }
      }),
      storage: createAuthStorage(storageLike)
    });

    await expect(service.login()).rejects.toMatchObject({
      code: 'INVALID_CODE',
      message: '登录凭证无效或已过期'
    } satisfies Partial<AuthError>);
  });

  it('restores an unexpired session and removes an expired session', () => {
    const storage = createAuthStorage(storageLike);
    const validSession: AuthSession = {
      ...loginResponse,
      expiresAt: Date.now() + 60_000
    };

    storage.saveSession(validSession);
    expect(storage.getSession()).toEqual(validSession);

    storage.saveSession({ ...validSession, expiresAt: Date.now() - 1 });
    expect(storage.getSession()).toBeNull();
  });

  it('normalizes roles and permissions from a legacy cached session', () => {
    storageLike.set('cunmo.auth.session', {
      ...loginResponse,
      expiresAt: Date.now() + 60_000,
      user: {
        id: 10001,
        nickname: null,
        avatarUrl: null,
        profileCompleted: false
      }
    });

    const session = createAuthStorage(storageLike).getSession();

    expect(session?.user.roles).toEqual([]);
    expect(session?.user.permissions).toEqual([]);
  });

  it('adds bearer token and clears the session after a 401 response', async () => {
    const storage = createAuthStorage(storageLike);
    storage.saveSession({
      ...loginResponse,
      expiresAt: Date.now() + 60_000
    });
    const request = vi.fn().mockRejectedValue({ statusCode: 401 });
    const onUnauthorized = vi.fn();
    const client = createAuthHttpClient({
      request,
      storage,
      onUnauthorized
    });

    await expect(client.request({ url: '/api/items', method: 'GET' })).rejects.toMatchObject({
      statusCode: 401
    });
    expect(request).toHaveBeenCalledWith(
      expect.objectContaining({
        header: expect.objectContaining({
          Authorization: 'Bearer business-token'
        })
      })
    );
    expect(storage.getSession()).toBeNull();
    expect(onUnauthorized).toHaveBeenCalledOnce();
  });
});
