export type SpaceKey = 'all' | 'bedroom' | 'living' | 'office' | string;
export type CategoryKey =
  | 'all'
  | 'digital'
  | 'clothes'
  | 'household'
  | 'officeSupplies'
  | string;

export interface Item {
  id: number;
  spaceId?: number;
  categoryId?: number;
  name: string;
  bigCate: CategoryKey;
  smallCate: string;
  space: SpaceKey;
  detailSpace: string;
  count: number;
  minWarn: number;
}

export interface TransactionLog {
  id: number;
  time: string;
  date: string;
  itemName: string;
  type: 'in' | 'warn' | 'out';
  icon: string;
  desc: string;
  path: string;
}

export interface InventoryCategory {
  id: number;
  code: CategoryKey;
  name: string;
  itemCount: number;
  remainingCount: number | null;
  capacityReached: boolean;
}

export interface InventorySpace {
  id: number;
  code: SpaceKey;
  name: string;
  categories: InventoryCategory[];
}

export interface InventoryBootstrap {
  vaultId: number;
  vaultName: string;
  totalQuantity: number;
  itemLimitPerSpaceCategory: number | null;
  unlimited: boolean;
  spaces: InventorySpace[];
}

export interface InventoryItemResponse {
  id: number;
  name: string;
  spaceId: number;
  spaceCode: SpaceKey;
  spaceName: string;
  categoryId: number;
  categoryCode: CategoryKey;
  categoryName: string;
  smallCategory: string;
  detailLocation: string;
  quantity: number;
  minimumQuantity: number;
  version: number;
}

export interface InventoryItemMutationResponse {
  id: number;
  spaceId: number;
  categoryId: number;
  name: string;
  smallCategory: string;
  detailLocation: string;
  quantity: number;
  minimumQuantity: number;
  version: number;
}

export interface InventoryAnalytics {
  id: number;
  code: string;
  name: string;
  itemCount: number;
  quantity: number;
  percent: number;
}

export interface StockTransactionResponse {
  id: number;
  itemId: number;
  itemName: string;
  type: 'IN' | 'OUT' | 'WARN';
  delta: number;
  quantityBefore: number;
  quantityAfter: number;
  description: string;
  spaceName: string;
  categoryName: string;
  detailLocation: string;
  occurredAt: string;
}

export interface CursorPage<T> {
  items: T[];
  nextCursor: number | null;
  hasMore: boolean;
}

export interface GlobalState {
  currentFilter: { space: SpaceKey; cate: CategoryKey };
  searchQuery: string;
  activeView: AppView;
  currentLensTab: LensTab;
  isLoggedIn: boolean;
  currentUser: AuthUser | null;
  modal: ModalState;
}

export type AppView = 'home' | 'lens' | 'axis' | 'profile';
export type LensTab = 'space' | 'cate';
export type ModalKind = 'space' | 'category' | 'item' | null;

export interface ModalState {
  kind: ModalKind;
  itemContext: ItemCreationContext | null;
}

export type SpaceDictionary = Record<SpaceKey, string>;
export type CategoryDictionary = Record<CategoryKey, string>;
export type SpaceCategoryMap = Record<SpaceKey, CategoryDictionary>;

export interface ItemCreationContext {
  space: SpaceKey;
  cate: CategoryKey;
  prefilledName?: string;
}

export interface ItemInput {
  name: string;
  bigCate: CategoryKey;
  smallCate: string;
  space: SpaceKey;
  detailSpace: string;
  count: number;
  minWarn: number;
}

export interface EmptyCavityContext extends ItemCreationContext {
  spaceLabel: string;
  categoryLabel: string;
  isSearchEmpty: boolean;
}

export type InventoryAction =
  | { type: 'SET_FILTER'; payload: Partial<GlobalState['currentFilter']> }
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'SET_VIEW'; payload: AppView }
  | { type: 'SET_LENS_TAB'; payload: LensTab }
  | { type: 'LOGIN'; payload: AuthSession }
  | { type: 'LOGOUT' }
  | { type: 'OPEN_MODAL'; payload: ModalState }
  | { type: 'CLOSE_MODAL' };
import type { AuthSession, AuthUser } from './auth';
