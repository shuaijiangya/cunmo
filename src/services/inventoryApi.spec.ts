import { describe, expect, it, vi } from 'vitest';

import { createInventoryApi } from './inventoryApi';

describe('inventoryApi', () => {
  /**
   * 验证物品筛选条件和游标被转换为查询参数。
   */
  it('loads filtered item page with cursor', async () => {
    const request = vi.fn().mockResolvedValue({
      items: [],
      nextCursor: null,
      hasMore: false
    });
    const api = createInventoryApi(request);

    await api.getItems({
      spaceId: 10,
      categoryId: 20,
      keyword: '充电器',
      cursor: 99,
      size: 20
    });

    expect(request).toHaveBeenCalledWith({
      url: '/api/inventory/items?spaceId=10&categoryId=20&keyword=%E5%85%85%E7%94%B5%E5%99%A8&cursor=99&size=20',
      method: 'GET'
    });
  });

  /**
   * 验证分类创建支持一次绑定多个空间。
   */
  it('creates category with multiple space bindings', async () => {
    const request = vi.fn().mockResolvedValue({ id: 30 });
    const api = createInventoryApi(request);

    await api.createCategory('电子数码', [1, 2, 3]);

    expect(request).toHaveBeenCalledWith({
      url: '/api/inventory/categories',
      method: 'POST',
      data: {
        name: '电子数码',
        spaceIds: [1, 2, 3]
      }
    });
  });
});
