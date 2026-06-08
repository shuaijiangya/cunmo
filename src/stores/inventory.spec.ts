import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

import type { AuthSession } from '@/types/auth';

const inventoryApiMock = vi.hoisted(() => ({
  getBootstrap: vi.fn(),
  getItems: vi.fn(),
  getAnalytics: vi.fn(),
  getTransactions: vi.fn(),
  createSpace: vi.fn(),
  createCategory: vi.fn(),
  updateCategorySpaces: vi.fn(),
  createItem: vi.fn(),
  adjustStock: vi.fn(),
  getItemDeletionPreview: vi.fn(),
  getSpaceDeletionPreview: vi.fn(),
  getCategoryDeletionPreview: vi.fn(),
  deleteItem: vi.fn(),
  deleteSpace: vi.fn(),
  unbindCategory: vi.fn(),
  deleteCategory: vi.fn()
}));

vi.mock('@/services/inventoryApi', () => ({
  inventoryApi: inventoryApiMock
}));

import { useInventoryStore } from './inventoryStore';

const authSession: AuthSession = {
  token: 'test-token',
  expiresIn: 7200,
  expiresAt: Date.now() + 60_000,
  user: {
    id: 42,
    nickname: '存魔用户',
    avatarUrl: null,
    profileCompleted: true,
    roles: ['USER'],
    permissions: ['inventory:item:read']
  }
};

const bootstrap = {
  vaultId: 1,
  vaultName: '我的存魔方',
  totalQuantity: 3,
  itemLimitPerSpaceCategory: 20,
  unlimited: false,
  spaces: [
    {
      id: 11,
      code: 'bedroom',
      name: '卧室',
      categories: [
        {
          id: 21,
          code: 'digital',
          name: '电子数码',
          itemCount: 1,
          remainingCount: 19,
          capacityReached: false
        }
      ]
    }
  ]
};

const itemPage = {
  items: [
    {
      id: 31,
      name: '充电器',
      spaceId: 11,
      spaceCode: 'bedroom',
      spaceName: '卧室',
      categoryId: 21,
      categoryCode: 'digital',
      categoryName: '电子数码',
      smallCategory: '配件',
      detailLocation: '床头柜',
      quantity: 3,
      minimumQuantity: 1,
      version: 0
    }
  ],
  nextCursor: 31,
  hasMore: true
};

const emptyPage = {
  items: [],
  nextCursor: null,
  hasMore: false
};

describe('inventory store actions', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    inventoryApiMock.getBootstrap.mockResolvedValue(bootstrap);
    inventoryApiMock.getItems.mockResolvedValue(itemPage);
    inventoryApiMock.getAnalytics.mockResolvedValue(emptyPage);
    inventoryApiMock.getTransactions.mockResolvedValue(emptyPage);
  });

  it('initializes dictionaries and items from backend data', async () => {
    const store = useInventoryStore();
    store.isLoggedIn = true;

    await store.initializeInventory();

    expect(store.spaces).toEqual({ all: '全部', bedroom: '卧室' });
    expect(store.spaceToCates.bedroom.digital).toBe('电子数码');
    expect(store.items[0]).toMatchObject({
      id: 31,
      name: '充电器',
      count: 3,
      space: 'bedroom',
      bigCate: 'digital'
    });
    expect(store.hasMoreItems).toBe(true);
  });

  it('adjusts stock through backend and refreshes dependent views', async () => {
    const store = useInventoryStore();
    store.isLoggedIn = true;
    store.authMode = 'authenticated';
    store.items = [
      {
        id: 31,
        name: '充电器',
        bigCate: 'digital',
        smallCate: '配件',
        space: 'bedroom',
        detailSpace: '床头柜',
        count: 3,
        minWarn: 1
      }
    ];
    inventoryApiMock.adjustStock.mockResolvedValue({
      ...itemPage.items[0],
      quantity: 4
    });

    await store.adjustItemCount(31, 1);

    expect(inventoryApiMock.adjustStock).toHaveBeenCalledWith(31, 1);
    expect(store.items[0].count).toBe(4);
    expect(inventoryApiMock.getBootstrap).toHaveBeenCalledOnce();
    expect(inventoryApiMock.getAnalytics).toHaveBeenCalledOnce();
    expect(inventoryApiMock.getTransactions).toHaveBeenCalledOnce();
  });

  it('appends the next item page with the server cursor', async () => {
    const store = useInventoryStore();
    store.isLoggedIn = true;
    store.authMode = 'authenticated';
    store.itemCursor = 31;
    store.items = [
      {
        id: 31,
        name: '充电器',
        bigCate: 'digital',
        smallCate: '配件',
        space: 'bedroom',
        detailSpace: '床头柜',
        count: 3,
        minWarn: 1
      }
    ];
    inventoryApiMock.getItems.mockResolvedValue({
      items: [{ ...itemPage.items[0], id: 30, name: '数据线' }],
      nextCursor: null,
      hasMore: false
    });

    await store.loadItems(false);

    expect(inventoryApiMock.getItems).toHaveBeenCalledWith(
      expect.objectContaining({ cursor: 31 })
    );
    expect(store.items.map((item) => item.name)).toEqual(['充电器', '数据线']);
    expect(store.hasMoreItems).toBe(false);
  });

  it('creates a category with multiple resolved space ids', async () => {
    const store = useInventoryStore();
    store.spaceIds = { bedroom: 11, living: 12 };
    inventoryApiMock.createCategory.mockResolvedValue({ id: 22 });

    await store.createCategory('常备药', ['bedroom', 'living']);

    expect(inventoryApiMock.createCategory).toHaveBeenCalledWith(
      '常备药',
      [11, 12]
    );
    expect(inventoryApiMock.getBootstrap).toHaveBeenCalledOnce();
  });

  it('restores the authenticated user and initializes inventory', async () => {
    const store = useInventoryStore();

    store.restoreAuth(authSession);
    await vi.waitFor(() => expect(inventoryApiMock.getBootstrap).toHaveBeenCalled());

    expect(store.isLoggedIn).toBe(true);
    expect(store.currentUser).toEqual(authSession.user);
  });

  it('starts guests with sample data without requesting private inventory', () => {
    const store = useInventoryStore();

    store.initializeExperience(null);

    expect(store.authMode).toBe('guest');
    expect(store.items.length).toBeGreaterThan(0);
    expect(store.bootstrap?.vaultName).toBe('访客体验魔方');
    expect(inventoryApiMock.getBootstrap).not.toHaveBeenCalled();
  });

  it('requests login for protected views and clears the intent when cancelled', () => {
    const store = useInventoryStore();
    store.initializeExperience(null);

    store.dispatch({ type: 'SET_VIEW', payload: 'profile' });

    expect(store.activeView).toBe('home');
    expect(store.loginVisible).toBe(true);
    expect(store.pendingAuthIntent).toEqual({
      kind: 'view',
      view: 'profile'
    });

    store.cancelAuthentication();

    expect(store.loginVisible).toBe(false);
    expect(store.pendingAuthIntent).toBeNull();
  });

  it('loads real inventory before continuing a protected modal intent', async () => {
    const store = useInventoryStore();
    store.initializeExperience(null);
    store.dispatch({
      type: 'OPEN_MODAL',
      payload: { kind: 'space', itemContext: null }
    });

    await store.completeLogin(authSession);

    expect(store.authMode).toBe('authenticated');
    expect(store.loginVisible).toBe(false);
    expect(store.modal.kind).toBe('space');
    expect(store.items[0].name).toBe('充电器');
    expect(inventoryApiMock.getBootstrap).toHaveBeenCalledOnce();
  });

  it('does not force users with incomplete profiles away from their intent', async () => {
    const store = useInventoryStore();
    store.initializeExperience(null);
    const incompleteSession = {
      ...authSession,
      user: { ...authSession.user, profileCompleted: false }
    };

    await store.completeLogin(incompleteSession);

    expect(store.activeView).toBe('home');
  });

  it('does not replay guest sample entity ids against real inventory', async () => {
    const store = useInventoryStore();
    store.initializeExperience(null);

    await store.adjustItemCount(301, 1);
    await store.completeLogin(authSession);

    expect(inventoryApiMock.adjustStock).not.toHaveBeenCalled();
    expect(store.activeView).toBe('home');
    expect(store.noticeMessage).toBe(
      '已切换到你的真实库存，请重新选择要操作的物品'
    );
  });

  it('clears user and inventory state after remote logout succeeds', async () => {
    const store = useInventoryStore();
    const storage = { clearSession: vi.fn() };
    const remoteLogout = vi.fn().mockResolvedValue(undefined);
    store.isLoggedIn = true;
    store.currentUser = authSession.user;
    store.items = [
      {
        id: 31,
        name: '充电器',
        bigCate: 'digital',
        smallCate: '配件',
        space: 'bedroom',
        detailSpace: '床头柜',
        count: 3,
        minWarn: 1
      }
    ];

    await store.logout(storage, remoteLogout);

    expect(store.isLoggedIn).toBe(false);
    expect(store.currentUser).toBeNull();
    expect(store.authMode).toBe('guest');
    expect(store.activeView).toBe('home');
    expect(store.items.length).toBeGreaterThan(0);
    expect(store.loginVisible).toBe(false);
    expect(storage.clearSession).toHaveBeenCalledOnce();
    expect(remoteLogout).toHaveBeenCalledOnce();
  });

  it('still clears local state when remote logout fails', async () => {
    const store = useInventoryStore();
    const storage = { clearSession: vi.fn() };
    const remoteLogout = vi.fn().mockRejectedValue(new Error('offline'));
    store.isLoggedIn = true;
    store.authMode = 'authenticated';
    store.currentUser = authSession.user;

    await store.logout(storage, remoteLogout);

    expect(storage.clearSession).toHaveBeenCalledOnce();
    expect(store.authMode).toBe('guest');
    expect(store.currentUser).toBeNull();
    expect(store.noticeMessage).toBe(
      '账号已在本机退出，服务器退出请求未完成'
    );
  });

  it('returns an expired session to guest mode without forcing login', () => {
    const store = useInventoryStore();
    store.isLoggedIn = true;
    store.authMode = 'authenticated';
    store.currentUser = authSession.user;

    store.handleUnauthorized();

    expect(store.authMode).toBe('guest');
    expect(store.currentUser).toBeNull();
    expect(store.loginVisible).toBe(false);
    expect(store.noticeMessage).toBe('登录状态已失效，请重新登录');
    expect(store.items.length).toBeGreaterThan(0);
  });

  it('deletes an item and refreshes all dependent views', async () => {
    const store = useInventoryStore();
    store.isLoggedIn = true;
    store.authMode = 'authenticated';
    store.currentFilter = { space: 'bedroom', cate: 'digital' };
    inventoryApiMock.deleteItem.mockResolvedValue(undefined);

    await store.deleteInventoryItem(31);

    expect(inventoryApiMock.deleteItem).toHaveBeenCalledWith(31);
    expect(inventoryApiMock.getBootstrap).toHaveBeenCalledOnce();
    expect(inventoryApiMock.getItems).toHaveBeenCalledOnce();
    expect(inventoryApiMock.getAnalytics).toHaveBeenCalledOnce();
    expect(inventoryApiMock.getTransactions).toHaveBeenCalledOnce();
  });

  it('falls back to all filters after deleting active space', async () => {
    const store = useInventoryStore();
    store.currentFilter = { space: 'bedroom', cate: 'digital' };
    inventoryApiMock.deleteSpace.mockResolvedValue(undefined);
    inventoryApiMock.getBootstrap.mockResolvedValue({
      ...bootstrap,
      spaces: []
    });

    await store.deleteInventorySpace(11, 'CLEAR_DELETE');

    expect(store.currentFilter).toEqual({ space: 'all', cate: 'all' });
  });
});
