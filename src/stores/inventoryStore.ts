import { defineStore } from 'pinia';

import { authStorage, type AuthStorage } from '@/services/authStorage';
import {
  createGuestInventory,
  filterGuestItems
} from '@/services/guestInventory';
import { inventoryApi } from '@/services/inventoryApi';
import { logoutService } from '@/services/logoutService';
import type { AuthSession, AuthUser } from '@/types/auth';
import type {
  AppView,
  AuthIntent,
  AuthMode,
  CategoryKey,
  DeletionStrategy,
  DeletionModalState,
  EmptyCavityContext,
  InventoryAnalytics,
  InventoryBootstrap,
  InventoryCategory,
  InventoryItemResponse,
  Item,
  ItemInput,
  LensTab,
  ModalState,
  SpaceCategoryMap,
  SpaceDictionary,
  SpaceKey,
  StockTransactionResponse,
  TransactionLog
} from '@/types/inventory';

interface InventoryState {
  spaces: SpaceDictionary;
  spaceToCates: SpaceCategoryMap;
  spaceIds: Record<string, number>;
  categoryIds: Record<string, number>;
  categoriesBySpace: Record<string, InventoryCategory[]>;
  bootstrap: InventoryBootstrap | null;
  items: Item[];
  logs: TransactionLog[];
  analytics: InventoryAnalytics[];
  currentFilter: { space: SpaceKey; cate: CategoryKey };
  searchQuery: string;
  activeView: AppView;
  currentLensTab: LensTab;
  isLoggedIn: boolean;
  authMode: AuthMode;
  loginVisible: boolean;
  pendingAuthIntent: AuthIntent | null;
  noticeMessage: string;
  currentUser: AuthUser | null;
  guestItems: InventoryItemResponse[];
  modal: ModalState;
  deletionModal: DeletionModalState | null;
  loading: boolean;
  loadingMoreItems: boolean;
  loadingMoreAnalytics: boolean;
  loadingMoreLogs: boolean;
  errorMessage: string;
  itemError: string;
  analyticsError: string;
  logError: string;
  itemCursor: number | null;
  analyticsCursor: number | null;
  logCursor: number | null;
  hasMoreItems: boolean;
  hasMoreAnalytics: boolean;
  hasMoreLogs: boolean;
}

/**
 * 创建仅包含“全部”选项的空空间字典。
 */
const emptySpaces = (): SpaceDictionary => ({ all: '全部' });

/**
 * 创建仅包含默认分类的空空间分类映射。
 */
const emptyCategories = (): SpaceCategoryMap => ({
  all: { all: '全部分类' }
});

/**
 * 将接口物品模型转换为页面展示模型。
 */
const toItem = (item: InventoryItemResponse): Item => ({
  id: item.id,
  spaceId: item.spaceId,
  categoryId: item.categoryId,
  name: item.name,
  bigCate: item.categoryCode,
  smallCate: item.smallCategory,
  space: item.spaceCode,
  detailSpace: item.detailLocation,
  count: item.quantity,
  minWarn: item.minimumQuantity
});

/**
 * 将库存流水转换为时间轴展示模型。
 */
const toLog = (entry: StockTransactionResponse): TransactionLog => {
  const date = new Date(entry.occurredAt);
  const today = new Date();
  const isToday = date.toDateString() === today.toDateString();
  return {
    id: entry.id,
    time: date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    }),
    date: isToday ? '今天' : date.toLocaleDateString('zh-CN'),
    itemName: entry.itemName,
    type:
      entry.type === 'WARN'
        ? 'warn'
        : entry.type === 'IN'
          ? 'in'
          : entry.type === 'MOVE'
            ? 'move'
            : entry.type === 'DELETE'
              ? 'delete'
              : 'out',
    icon:
      entry.type === 'WARN'
        ? '!'
        : entry.type === 'IN'
          ? '+'
          : entry.type === 'MOVE'
            ? '→'
            : entry.type === 'DELETE'
              ? '×'
              : '-',
    desc: entry.description,
    path: `${entry.spaceName} ➔ ${entry.detailLocation || entry.categoryName}`
  };
};

export const useInventoryStore = defineStore('inventory', {
  /**
   * 创建库存模块的初始状态。
   */
  state: (): InventoryState => ({
    spaces: emptySpaces(),
    spaceToCates: emptyCategories(),
    spaceIds: {},
    categoryIds: {},
    categoriesBySpace: {},
    bootstrap: null,
    items: [],
    logs: [],
    analytics: [],
    currentFilter: { space: 'all', cate: 'all' },
    searchQuery: '',
    activeView: 'home',
    currentLensTab: 'space',
    isLoggedIn: false,
    authMode: 'guest',
    loginVisible: false,
    pendingAuthIntent: null,
    noticeMessage: '',
    currentUser: null,
    guestItems: [],
    modal: { kind: null, itemContext: null },
    deletionModal: null,
    loading: false,
    loadingMoreItems: false,
    loadingMoreAnalytics: false,
    loadingMoreLogs: false,
    errorMessage: '',
    itemError: '',
    analyticsError: '',
    logError: '',
    itemCursor: null,
    analyticsCursor: null,
    logCursor: null,
    hasMoreItems: false,
    hasMoreAnalytics: false,
    hasMoreLogs: false
  }),
  getters: {
    /**
     * 获取当前库存总数量。
     */
    totalCount: (state) =>
      state.bootstrap?.totalQuantity ??
      state.items.reduce((sum, item) => sum + item.count, 0),
    /**
     * 获取当前空间可用的分类字典。
     */
    activeCategories: (state) =>
      state.spaceToCates[state.currentFilter.space] ??
      state.spaceToCates.all,
    /**
     * 获取当前选中空间和分类的容量信息。
     */
    currentCapacity(state): InventoryCategory | null {
      if (
        state.currentFilter.space === 'all' ||
        state.currentFilter.cate === 'all'
      ) {
        return null;
      }
      return (
        state.categoriesBySpace[state.currentFilter.space]?.find(
          (category) => category.code === state.currentFilter.cate
        ) ?? null
      );
    },
    /**
     * 根据空间数量计算魔方展示阶数。
     */
    cubeOrder: (state) =>
      Object.keys(state.spaces).length - 1 > 3 ? 4 : 3,
    /**
     * 获取已经由数据源完成筛选的物品列表。
     */
    filteredItems: (state) => state.items,
    /**
     * 按大分类对当前物品列表进行分组。
     */
    groupedFilteredItems(): Record<CategoryKey, Item[]> {
      return this.filteredItems.reduce<Record<CategoryKey, Item[]>>(
        (groups, item) => {
          groups[item.bigCate] = groups[item.bigCate] ?? [];
          groups[item.bigCate].push(item);
          return groups;
        },
        {}
      );
    },
    /**
     * 生成空结果场景下新增物品所需的上下文。
     */
    emptyCavityContext(): EmptyCavityContext {
      const firstSpace =
        Object.keys(this.spaces).find((key) => key !== 'all') ?? 'all';
      const space =
        this.currentFilter.space !== 'all'
          ? this.currentFilter.space
          : firstSpace;
      const categories = this.spaceToCates[space] ?? { all: '全部分类' };
      const firstCategory =
        Object.keys(categories).find((key) => key !== 'all') ?? 'all';
      const cate =
        this.currentFilter.cate !== 'all'
          ? this.currentFilter.cate
          : firstCategory;
      return {
        space,
        cate,
        prefilledName: this.searchQuery,
        spaceLabel: this.spaces[space] ?? space,
        categoryLabel:
          categories[cate] ?? this.spaceToCates.all[cate] ?? cate,
        isSearchEmpty: Boolean(this.searchQuery)
      };
    },
    /**
     * 将统计接口数据转换为透视镜展示数据。
     */
    lensStats: (state) =>
      state.analytics.map((stat) => ({
        key: stat.code,
        label: stat.name,
        count: stat.quantity,
        itemCount: stat.itemCount,
        percent: stat.percent
      }))
  },
  actions: {
    /**
     * 兼容组件现有事件协议并分发 Store 操作。
     */
    dispatch(action: {
      type: string;
      payload?: any;
    }) {
      switch (action.type) {
        case 'SET_FILTER':
          this.currentFilter = {
            ...this.currentFilter,
            ...action.payload
          };
          break;
        case 'SET_SEARCH':
          this.searchQuery = String(action.payload ?? '').trim();
          break;
        case 'SET_VIEW':
          if (action.payload === 'profile' && !this.isLoggedIn) {
            this.requestAuthentication({
              kind: 'view',
              view: 'profile'
            });
            break;
          }
          this.activeView = action.payload;
          if (action.payload === 'lens' && !this.analytics.length) {
            void this.loadAnalytics(true);
          }
          if (action.payload === 'axis' && !this.logs.length) {
            void this.loadTransactions(true);
          }
          break;
        case 'SET_LENS_TAB':
          this.currentLensTab = action.payload;
          void this.loadAnalytics(true);
          break;
        case 'OPEN_MODAL':
          if (!this.isLoggedIn) {
            this.requestAuthentication({
              kind: 'modal',
              modal: action.payload
            });
            break;
          }
          this.modal = action.payload;
          break;
        case 'CLOSE_MODAL':
          this.modal = { kind: null, itemContext: null };
          break;
        case 'LOGOUT':
          this.resetInventory();
          break;
      }
    },
    /**
     * 初始化已登录用户的库存、统计和流水数据。
     */
    async initializeInventory() {
      if (!this.isLoggedIn || this.loading) return;
      this.loading = true;
      this.errorMessage = '';
      try {
        await this.refreshBootstrap();
        await Promise.all([
          this.loadItems(true),
          this.loadAnalytics(true),
          this.loadTransactions(true)
        ]);
      } catch (error) {
        this.errorMessage =
          (error as { data?: { message?: string } })?.data?.message ??
          '库存数据加载失败，请稍后重试';
      } finally {
        this.loading = false;
      }
    },
    /**
     * 刷新库存基础结构和汇总数据。
     */
    async refreshBootstrap() {
      const bootstrap = await inventoryApi.getBootstrap();
      this.applyBootstrap(bootstrap);
    },
    /**
     * 将基础结构响应转换为页面查询所需的字典。
     */
    applyBootstrap(bootstrap: InventoryBootstrap) {
      this.bootstrap = bootstrap;
      const spaces: SpaceDictionary = { all: '全部' };
      const categoryMap: SpaceCategoryMap = {
        all: { all: '全部分类' }
      };
      const spaceIds: Record<string, number> = {};
      const categoryIds: Record<string, number> = {};
      const categoriesBySpace: Record<string, InventoryCategory[]> = {};
      for (const space of bootstrap.spaces) {
        spaces[space.code] = space.name;
        spaceIds[space.code] = space.id;
        categoryMap[space.code] = {
          all: `全部${space.name}分类`
        };
        categoriesBySpace[space.code] = space.categories;
        for (const category of space.categories) {
          categoryMap[space.code][category.code] = category.name;
          categoryMap.all[category.code] = category.name;
          categoryIds[category.code] = category.id;
        }
      }
      this.spaces = spaces;
      this.spaceToCates = categoryMap;
      this.spaceIds = spaceIds;
      this.categoryIds = categoryIds;
      this.categoriesBySpace = categoriesBySpace;
    },
    /**
     * 按当前筛选条件加载物品列表或游客示例数据。
     */
    async loadItems(reset = false) {
      if (!this.isLoggedIn) {
        const filtered = filterGuestItems(this.guestItems, {
          space: this.currentFilter.space,
          cate: this.currentFilter.cate,
          keyword: this.searchQuery
        });
        this.items = filtered.map(toItem);
        this.itemCursor = null;
        this.hasMoreItems = false;
        this.itemError = '';
        return;
      }
      if (this.loadingMoreItems) return;
      this.loadingMoreItems = true;
      try {
        const page = await inventoryApi.getItems({
          spaceId:
            this.currentFilter.space === 'all'
              ? undefined
              : this.spaceIds[this.currentFilter.space],
          categoryId:
            this.currentFilter.cate === 'all'
              ? undefined
              : this.categoryIds[this.currentFilter.cate],
          keyword: this.searchQuery || undefined,
          cursor: reset ? null : this.itemCursor,
          size: 20
        });
        const items = page.items.map(toItem);
        this.items = reset ? items : [...this.items, ...items];
        this.itemCursor = page.nextCursor;
        this.hasMoreItems = page.hasMore;
        this.itemError = '';
      } catch (error) {
        this.itemError = this.requestErrorMessage(
          error,
          '物品列表加载失败，请稍后重试'
        );
      } finally {
        this.loadingMoreItems = false;
      }
    },
    /**
     * 按当前透视维度加载库存统计或游客示例数据。
     */
    async loadAnalytics(reset = false) {
      if (!this.isLoggedIn) {
        const guest = createGuestInventory();
        this.analytics =
          this.currentLensTab === 'space'
            ? guest.analytics.space
            : guest.analytics.category;
        this.analyticsCursor = null;
        this.hasMoreAnalytics = false;
        this.analyticsError = '';
        return;
      }
      if (this.loadingMoreAnalytics) return;
      this.loadingMoreAnalytics = true;
      try {
        const page = await inventoryApi.getAnalytics(
          this.currentLensTab === 'space' ? 'SPACE' : 'CATEGORY',
          reset ? null : this.analyticsCursor,
          8
        );
        this.analytics = reset
          ? page.items
          : [...this.analytics, ...page.items];
        this.analyticsCursor = page.nextCursor;
        this.hasMoreAnalytics = page.hasMore;
        this.analyticsError = '';
      } catch (error) {
        this.analyticsError = this.requestErrorMessage(
          error,
          '透视镜数据加载失败，请稍后重试'
        );
      } finally {
        this.loadingMoreAnalytics = false;
      }
    },
    /**
     * 加载库存流水或游客示例流水。
     */
    async loadTransactions(reset = false) {
      if (!this.isLoggedIn) {
        this.logs = createGuestInventory().transactions.map(toLog);
        this.logCursor = null;
        this.hasMoreLogs = false;
        this.logError = '';
        return;
      }
      if (this.loadingMoreLogs) return;
      this.loadingMoreLogs = true;
      try {
        const page = await inventoryApi.getTransactions(
          reset ? null : this.logCursor,
          10
        );
        const logs = page.items.map(toLog);
        this.logs = reset ? logs : [...this.logs, ...logs];
        this.logCursor = page.nextCursor;
        this.hasMoreLogs = page.hasMore;
        this.logError = '';
      } catch (error) {
        this.logError = this.requestErrorMessage(
          error,
          '流转轴数据加载失败，请稍后重试'
        );
      } finally {
        this.loadingMoreLogs = false;
      }
    },
    /**
     * 根据本地会话初始化登录模式或游客体验模式。
     */
    initializeExperience(session: AuthSession | null) {
      if (session) {
        void this.completeLogin(session);
        return;
      }
      this.loadGuestExperience();
    },
    /**
     * 从持久化会话恢复用户体验状态。
     */
    restoreAuth(session: AuthSession | null) {
      this.initializeExperience(session);
    },
    /**
     * 完成登录状态切换、真实数据加载和待办操作恢复。
     */
    async completeLogin(session: AuthSession) {
      this.isLoggedIn = true;
      this.authMode = 'authenticated';
      this.loginVisible = false;
      this.noticeMessage = '';
      this.currentUser = session.user;
      await this.initializeInventory();
      await this.continuePendingIntent();
    },
    /**
     * 将认证状态切换为登录处理中。
     */
    startAuthentication() {
      this.authMode = 'authenticating';
    },
    /**
     * 登录失败后恢复游客认证状态。
     */
    authenticationFailed() {
      this.authMode = 'guest';
    },
    /**
     * 记录受保护操作并展示登录授权界面。
     */
    requestAuthentication(intent: AuthIntent) {
      this.pendingAuthIntent = intent;
      this.loginVisible = true;
    },
    /**
     * 关闭登录界面并清除尚未执行的受保护操作。
     */
    cancelAuthentication() {
      this.loginVisible = false;
      this.pendingAuthIntent = null;
      if (!this.isLoggedIn) this.authMode = 'guest';
    },
    /**
     * 登录成功后继续执行用户此前请求的操作。
     */
    async continuePendingIntent() {
      const intent = this.pendingAuthIntent;
      this.pendingAuthIntent = null;
      if (!intent) return;
      if (intent.kind === 'view') {
        this.activeView = intent.view;
        return;
      }
      if (intent.kind === 'modal') {
        this.modal = intent.modal;
        return;
      }
      this.activeView = 'home';
      this.noticeMessage =
        '已切换到你的真实库存，请重新选择要操作的物品';
    },
    /**
     * 更新当前登录用户的展示资料。
     */
    updateCurrentUser(user: AuthUser) {
      this.currentUser = user;
    },
    /**
     * 注销服务端当前 Token，清理本地会话并返回游客体验。
     */
    async logout(
      storage: Pick<AuthStorage, 'clearSession'> = authStorage,
      remoteLogout: () => Promise<void> = () => logoutService.logout()
    ) {
      let noticeMessage = '';
      try {
        if (this.isLoggedIn) {
          await remoteLogout();
        }
      } catch {
        noticeMessage = '账号已在本机退出，服务器退出请求未完成';
      } finally {
        storage.clearSession();
        this.loadGuestExperience(noticeMessage);
      }
    },
    /**
     * 清空真实库存和当前用户相关状态。
     */
    clearInventory() {
      this.isLoggedIn = false;
      this.currentUser = null;
      this.activeView = 'home';
      this.spaces = emptySpaces();
      this.spaceToCates = emptyCategories();
      this.spaceIds = {};
      this.categoryIds = {};
      this.categoriesBySpace = {};
      this.bootstrap = null;
      this.items = [];
      this.logs = [];
      this.analytics = [];
      this.currentFilter = { space: 'all', cate: 'all' };
      this.searchQuery = '';
      this.currentLensTab = 'space';
      this.modal = { kind: null, itemContext: null };
      this.deletionModal = null;
      this.itemCursor = null;
      this.analyticsCursor = null;
      this.logCursor = null;
      this.hasMoreItems = false;
      this.hasMoreAnalytics = false;
      this.hasMoreLogs = false;
      this.errorMessage = '';
      this.itemError = '';
      this.analyticsError = '';
      this.logError = '';
    },
    /**
     * 加载游客示例数据，并展示可选的状态提示。
     */
    loadGuestExperience(noticeMessage = '') {
      this.clearInventory();
      const guest = createGuestInventory();
      this.authMode = 'guest';
      this.loginVisible = false;
      this.pendingAuthIntent = null;
      this.noticeMessage = noticeMessage;
      this.guestItems = guest.items;
      this.applyBootstrap(guest.bootstrap);
      this.items = guest.items.map(toItem);
      this.analytics = guest.analytics.space;
      this.logs = guest.transactions.map(toLog);
    },
    /**
     * 处理接口返回的未授权状态并回退到游客模式。
     */
    handleUnauthorized() {
      this.loadGuestExperience('登录状态已失效，请重新登录');
    },
    /**
     * 重置库存模块为默认游客体验。
     */
    resetInventory() {
      this.loadGuestExperience();
    },
    /**
     * 切换空间筛选并重新加载物品列表。
     */
    async setSpace(space: SpaceKey) {
      this.currentFilter = { space, cate: 'all' };
      this.searchQuery = '';
      await this.loadItems(true);
    },
    /**
     * 切换分类筛选并重新加载物品列表。
     */
    async setCategory(cate: CategoryKey) {
      this.currentFilter = { ...this.currentFilter, cate };
      this.searchQuery = '';
      await this.loadItems(true);
    },
    /**
     * 更新搜索关键词并重新加载物品列表。
     */
    async searchItems(query: string) {
      this.searchQuery = query.trim();
      await this.loadItems(true);
    },
    /**
     * 调整指定物品库存数量，并刷新关联统计数据。
     */
    async adjustItemCount(id: number, delta: number) {
      if (!this.isLoggedIn) {
        this.requestAuthentication({ kind: 'guest-entity-action' });
        return;
      }
      const updated = await inventoryApi.adjustStock(id, delta);
      const item = this.items.find((entry) => entry.id === id);
      if (item) item.count = updated.quantity;
      await Promise.all([
        this.refreshBootstrap(),
        this.loadAnalytics(true),
        this.loadTransactions(true)
      ]);
    },
    /**
     * 创建空间并切换到新建空间。
     */
    async createSpace(name: string) {
      await inventoryApi.createSpace(name.trim());
      await this.refreshBootstrap();
      const newest = this.bootstrap?.spaces.at(-1);
      if (newest) await this.setSpace(newest.code);
      this.modal = { kind: null, itemContext: null };
    },
    /**
     * 创建分类并绑定到选中的空间。
     */
    async createCategory(
      name: string,
      spaces: SpaceKey[]
    ) {
      const spaceIds = spaces
        .map((space) => this.spaceIds[space])
        .filter((id): id is number => Boolean(id));
      if (!spaceIds.length) {
        throw new Error('请至少选择一个有效空间');
      }
      await inventoryApi.createCategory(name.trim(), spaceIds);
      await this.refreshBootstrap();
      this.modal = { kind: null, itemContext: null };
    },
    /**
     * 创建物品并刷新当前筛选范围的库存数据。
     */
    async createItem(input: ItemInput) {
      const spaceId = this.spaceIds[input.space];
      const categoryId = this.categoryIds[input.bigCate];
      if (!spaceId || !categoryId) {
        throw new Error('空间或分类已失效，请刷新后重试');
      }
      await inventoryApi.createItem({
        spaceId,
        categoryId,
        name: input.name,
        smallCategory: input.smallCate,
        detailLocation: input.detailSpace,
        quantity: input.count,
        minimumQuantity: input.minWarn
      });
      this.currentFilter = {
        space: input.space,
        cate: input.bigCate
      };
      this.searchQuery = '';
      this.modal = { kind: null, itemContext: null };
      await Promise.all([
        this.refreshBootstrap(),
        this.loadItems(true),
        this.loadAnalytics(true),
        this.loadTransactions(true)
      ]);
    },
    /**
     * 数据变更后校正筛选条件并刷新全部关联数据。
     */
    async refreshAfterMutation() {
      await this.refreshBootstrap();
      if (
        this.currentFilter.space !== 'all' &&
        !this.spaces[this.currentFilter.space]
      ) {
        this.currentFilter = { space: 'all', cate: 'all' };
      } else if (
        this.currentFilter.cate !== 'all' &&
        !this.activeCategories[this.currentFilter.cate]
      ) {
        this.currentFilter = {
          space: this.currentFilter.space,
          cate: 'all'
        };
      }
      await Promise.all([
        this.loadItems(true),
        this.loadAnalytics(true),
        this.loadTransactions(true)
      ]);
    },
    /**
     * 在已登录状态下打开删除确认弹窗。
     */
    openDeletionModal(state: DeletionModalState) {
      if (!this.isLoggedIn) {
        this.requestAuthentication({ kind: 'guest-entity-action' });
        return;
      }
      this.deletionModal = state;
    },
    /**
     * 关闭删除确认弹窗。
     */
    closeDeletionModal() {
      this.deletionModal = null;
    },
    /**
     * 删除指定物品并刷新关联数据。
     */
    async deleteInventoryItem(itemId: number) {
      await inventoryApi.deleteItem(itemId);
      await this.refreshAfterMutation();
    },
    /**
     * 按指定策略删除空间并刷新关联数据。
     */
    async deleteInventorySpace(
      spaceId: number,
      strategy: DeletionStrategy,
      targetSpaceId?: number
    ) {
      await inventoryApi.deleteSpace(
        spaceId,
        strategy,
        targetSpaceId
      );
      await this.refreshAfterMutation();
    },
    /**
     * 按指定策略解除分类与空间的绑定。
     */
    async unbindInventoryCategory(
      categoryId: number,
      spaceId: number,
      strategy: DeletionStrategy,
      targetCategoryId?: number
    ) {
      await inventoryApi.unbindCategory(
        categoryId,
        spaceId,
        strategy,
        targetCategoryId
      );
      await this.refreshAfterMutation();
    },
    /**
     * 按指定策略删除分类并刷新关联数据。
     */
    async deleteInventoryCategory(
      categoryId: number,
      strategy: DeletionStrategy,
      targetCategoryId?: number
    ) {
      await inventoryApi.deleteCategory(
        categoryId,
        strategy,
        targetCategoryId
      );
      await this.refreshAfterMutation();
    },
    /**
     * 从接口异常中提取可展示消息，并提供默认文案。
     */
    requestErrorMessage(error: unknown, fallback: string) {
      return (
        (error as { data?: { message?: string } })?.data?.message ??
        (error as { message?: string })?.message ??
        fallback
      );
    }
  }
});
