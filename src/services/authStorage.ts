import type { AuthSession } from '@/types/auth';

const AUTH_SESSION_KEY = 'cunmo.auth.session';

export interface StorageLike {
  get(key: string): unknown;
  set(key: string, value: unknown): unknown;
  remove(key: string): unknown;
}

export interface AuthStorage {
  getSession(): AuthSession | null;
  saveSession(session: AuthSession): void;
  clearSession(): void;
}

const isAuthSession = (value: unknown): value is AuthSession => {
  if (!value || typeof value !== 'object') return false;
  const session = value as Partial<AuthSession>;
  return (
    typeof session.token === 'string' &&
    typeof session.expiresIn === 'number' &&
    typeof session.expiresAt === 'number' &&
    !!session.user &&
    typeof session.user.id === 'number'
  );
};

const normalizeSession = (session: AuthSession): AuthSession => ({
  ...session,
  user: {
    ...session.user,
    roles: Array.isArray(session.user.roles) ? session.user.roles : [],
    permissions: Array.isArray(session.user.permissions)
      ? session.user.permissions
      : []
  }
});

export const createUniStorage = (): StorageLike => ({
  get: (key) => uni.getStorageSync(key),
  set: (key, value) => uni.setStorageSync(key, value),
  remove: (key) => uni.removeStorageSync(key)
});

export const createAuthStorage = (
  storage: StorageLike = createUniStorage()
): AuthStorage => ({
  getSession() {
    const session = storage.get(AUTH_SESSION_KEY);
    if (!isAuthSession(session)) {
      if (session !== undefined && session !== null && session !== '') {
        storage.remove(AUTH_SESSION_KEY);
      }
      return null;
    }
    if (session.expiresAt <= Date.now()) {
      storage.remove(AUTH_SESSION_KEY);
      return null;
    }
    return normalizeSession(session);
  },
  saveSession(session) {
    storage.set(AUTH_SESSION_KEY, session);
  },
  clearSession() {
    storage.remove(AUTH_SESSION_KEY);
  }
});

export const authStorage = createAuthStorage();
