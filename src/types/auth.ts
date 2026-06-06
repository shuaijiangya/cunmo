export interface AuthUser {
  id: number;
  nickname: string | null;
  avatarUrl: string | null;
  profileCompleted: boolean;
  roles: string[];
  permissions: string[];
}

export interface AuthLoginResponse {
  token: string;
  expiresIn: number;
  user: AuthUser;
}

export interface AuthSession extends AuthLoginResponse {
  expiresAt: number;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
}

export interface ProfileUpdateInput {
  nickname: string;
  avatarUrl: string;
}

export interface ProfileResponse {
  id: number;
  nickname: string | null;
  avatarUrl: string | null;
  profileCompleted: boolean;
}

export interface AvatarUploadResponse {
  avatarUrl: string;
}
