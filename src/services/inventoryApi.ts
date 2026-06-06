import type {
  CursorPage,
  InventoryAnalytics,
  InventoryBootstrap,
  InventoryDeletionPreview,
  InventoryItemMutationResponse,
  InventoryItemResponse,
  DeletionStrategy,
  StockTransactionResponse
} from '@/types/inventory';
import { authHttpClient, type RequestOptions } from './httpClient';

type RequestAdapter = <T>(options: RequestOptions) => Promise<T>;

interface ItemQuery {
  spaceId?: number;
  categoryId?: number;
  keyword?: string;
  cursor?: number | null;
  size?: number;
}

const queryString = (
  values: Record<string, string | number | null | undefined>
) => {
  const entries = Object.entries(values).filter(
    ([, value]) => value !== undefined && value !== null && value !== ''
  );
  if (!entries.length) return '';
  return `?${entries
    .map(
      ([key, value]) =>
        `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`
    )
    .join('&')}`;
};

export const createInventoryApi = (request: RequestAdapter) => ({
  getBootstrap: () =>
    request<InventoryBootstrap>({
      url: '/api/inventory/bootstrap',
      method: 'GET'
    }),

  getItems: (query: ItemQuery = {}) =>
    request<CursorPage<InventoryItemResponse>>({
      url: `/api/inventory/items${queryString({
        spaceId: query.spaceId,
        categoryId: query.categoryId,
        keyword: query.keyword,
        cursor: query.cursor,
        size: query.size ?? 20
      })}`,
      method: 'GET'
    }),

  getAnalytics: (
    dimension: 'SPACE' | 'CATEGORY',
    cursor?: number | null,
    size = 20
  ) =>
    request<CursorPage<InventoryAnalytics>>({
      url: `/api/inventory/analytics${queryString({
        dimension,
        cursor,
        size
      })}`,
      method: 'GET'
    }),

  getTransactions: (cursor?: number | null, size = 20) =>
    request<CursorPage<StockTransactionResponse>>({
      url: `/api/inventory/transactions${queryString({ cursor, size })}`,
      method: 'GET'
    }),

  createSpace: (name: string) =>
    request<{ id: number }>({
      url: '/api/inventory/spaces',
      method: 'POST',
      data: { name }
    }),

  createCategory: (name: string, spaceIds: number[]) =>
    request<{ id: number }>({
      url: '/api/inventory/categories',
      method: 'POST',
      data: { name, spaceIds }
    }),

  updateCategorySpaces: (categoryId: number, spaceIds: number[]) =>
    request<void>({
      url: `/api/inventory/categories/${categoryId}/spaces`,
      method: 'PUT',
      data: { spaceIds }
    }),

  createItem: (data: {
    spaceId: number;
    categoryId: number;
    name: string;
    smallCategory: string;
    detailLocation: string;
    quantity: number;
    minimumQuantity: number;
  }) =>
    request<InventoryItemMutationResponse>({
      url: '/api/inventory/items',
      method: 'POST',
      data
    }),

  adjustStock: (itemId: number, delta: number, description?: string) =>
    request<InventoryItemMutationResponse>({
      url: `/api/inventory/items/${itemId}/adjustments`,
      method: 'POST',
      data: { delta, description }
    }),

  getItemDeletionPreview: (itemId: number) =>
    request<InventoryDeletionPreview>({
      url: `/api/inventory/items/${itemId}/deletion-preview`,
      method: 'GET'
    }),

  getSpaceDeletionPreview: (spaceId: number) =>
    request<InventoryDeletionPreview>({
      url: `/api/inventory/spaces/${spaceId}/deletion-preview`,
      method: 'GET'
    }),

  getCategoryDeletionPreview: (
    categoryId: number,
    spaceId?: number
  ) =>
    request<InventoryDeletionPreview>({
      url: `/api/inventory/categories/${categoryId}/deletion-preview${queryString({
        spaceId
      })}`,
      method: 'GET'
    }),

  deleteItem: (itemId: number) =>
    request<void>({
      url: `/api/inventory/items/${itemId}/deletion`,
      method: 'POST'
    }),

  deleteSpace: (
    spaceId: number,
    strategy: DeletionStrategy,
    targetSpaceId?: number
  ) =>
    request<void>({
      url: `/api/inventory/spaces/${spaceId}/deletion`,
      method: 'POST',
      data: { strategy, targetSpaceId }
    }),

  unbindCategory: (
    categoryId: number,
    spaceId: number,
    strategy: DeletionStrategy,
    targetCategoryId?: number
  ) =>
    request<void>({
      url: `/api/inventory/categories/${categoryId}/unbinding`,
      method: 'POST',
      data: { spaceId, strategy, targetCategoryId }
    }),

  deleteCategory: (
    categoryId: number,
    strategy: DeletionStrategy,
    targetCategoryId?: number
  ) =>
    request<void>({
      url: `/api/inventory/categories/${categoryId}/deletion`,
      method: 'POST',
      data: { strategy, targetCategoryId }
    })
});

export const inventoryApi = createInventoryApi((options) =>
  authHttpClient.request(options)
);
