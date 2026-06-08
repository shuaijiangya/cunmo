import { describe, expect, it, vi } from 'vitest';

import { createLogoutService } from './logoutService';

describe('logoutService', () => {
  it('requests current-token logout from the backend', async () => {
    const request = vi.fn().mockResolvedValue(undefined);
    const service = createLogoutService(request);

    await service.logout();

    expect(request).toHaveBeenCalledWith({
      url: '/api/auth/logout',
      method: 'POST'
    });
  });
});
