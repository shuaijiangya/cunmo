import { defineStore } from 'pinia';

import { authStorage, type AuthStorage } from '@/services/authStorage';
import { inventoryApi } from '@/services/inventoryApi';
import type { AuthSession, AuthUser } from '@/types/auth';
import type {
  AppView,
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
  currentUser: AuthUser | null;
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

const emptySpaces = (): SpaceDictionary => ({ all: '全部' });
const emptyCategories = (): SpaceCategoryMap => ({
  all: { all: '全部分类' }
});

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
    currentUser: null,
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
    totalCount: (state) =>
      state.bootstrap?.totalQuantity ??
      state.items.reduce((sum, item) => sum + item.count, 0),
    activeCategories: (state) =>
      state.spaceToCates[state.currentFilter.space] ??
      state.spaceToCates.all,
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
    cubeOrder: (state) =>
      Object.keys(state.spaces).length - 1 > 3 ? 4 : 3,
    filteredItems: (state) => state.items,
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
    async refreshBootstrap() {
      const bootstrap = await inventoryApi.getBootstrap();
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
    async loadItems(reset = false) {
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
    async loadAnalytics(reset = false) {
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
    async loadTransactions(reset = false) {
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
    restoreAuth(session: AuthSession | null) {
      if (!session) return;
      this.completeLogin(session);
    },
    completeLogin(session: AuthSession) {
      this.isLoggedIn = true;
      this.currentUser = session.user;
      if (!session.user.profileCompleted) {
        this.activeView = 'profile';
      }
      void this.initializeInventory();
    },
    updateCurrentUser(user: AuthUser) {
      this.currentUser = user;
    },
    logout(storage: Pick<AuthStorage, 'clearSession'> = authStorage) {
      storage.clearSession();
      this.dispatch({ type: 'LOGOUT' });
    },
    resetInventory() {
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
    async setSpace(space: SpaceKey) {
      this.currentFilter = { space, cate: 'all' };
      this.searchQuery = '';
      await this.loadItems(true);
    },
    async setCategory(cate: CategoryKey) {
      this.currentFilter = { ...this.currentFilter, cate };
      this.searchQuery = '';
      await this.loadItems(true);
    },
    async searchItems(query: string) {
      this.searchQuery = query.trim();
      await this.loadItems(true);
    },
    async adjustItemCount(id: number, delta: number) {
      const updated = await inventoryApi.adjustStock(id, delta);
      const item = this.items.find((entry) => entry.id === id);
      if (item) item.count = updated.quantity;
      await Promise.all([
        this.refreshBootstrap(),
        this.loadAnalytics(true),
        this.loadTransactions(true)
      ]);
    },
    async createSpace(name: string) {
      await inventoryApi.createSpace(name.trim());
      await this.refreshBootstrap();
      const newest = this.bootstrap?.spaces.at(-1);
      if (newest) await this.setSpace(newest.code);
      this.modal = { kind: null, itemContext: null };
    },
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
    openDeletionModal(state: DeletionModalState) {
      this.deletionModal = state;
    },
    closeDeletionModal() {
      this.deletionModal = null;
    },
    async moveInventoryItem(
      itemId: number,
      targetSpaceId: number,
      targetCategoryId: number
    ) {
      await inventoryApi.moveItem(
        itemId,
        targetSpaceId,
        targetCategoryId
      );
      await this.refreshAfterMutation();
    },
    async deleteInventoryItem(itemId: number) {
      await inventoryApi.deleteItem(itemId);
      await this.refreshAfterMutation();
    },
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
    requestErrorMessage(error: unknown, fallback: string) {
      return (
        (error as { data?: { message?: string } })?.data?.message ??
        (error as { message?: string })?.message ??
        fallback
      );
    }
  }
});
