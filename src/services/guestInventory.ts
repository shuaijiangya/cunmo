import type {
  CategoryKey,
  InventoryAnalytics,
  InventoryBootstrap,
  InventoryItemResponse,
  SpaceKey,
  StockTransactionResponse
} from '@/types/inventory';

export interface GuestInventory {
  bootstrap: InventoryBootstrap;
  items: InventoryItemResponse[];
  analytics: {
    space: InventoryAnalytics[];
    category: InventoryAnalytics[];
  };
  transactions: StockTransactionResponse[];
}

const sample: GuestInventory = {
  bootstrap: {
    vaultId: 0,
    vaultName: '访客体验魔方',
    totalQuantity: 18,
    itemLimitPerSpaceCategory: 20,
    unlimited: false,
    spaces: [
      {
        id: 101,
        code: 'bedroom',
        name: '卧室',
        categories: [
          {
            id: 201,
            code: 'digital',
            name: '电子数码',
            itemCount: 2,
            remainingCount: 18,
            capacityReached: false
          },
          {
            id: 202,
            code: 'clothes',
            name: '衣物',
            itemCount: 1,
            remainingCount: 19,
            capacityReached: false
          }
        ]
      },
      {
        id: 102,
        code: 'living',
        name: '客厅',
        categories: [
          {
            id: 203,
            code: 'household',
            name: '家居日用',
            itemCount: 2,
            remainingCount: 18,
            capacityReached: false
          },
          {
            id: 204,
            code: 'digital',
            name: '电子数码',
            itemCount: 1,
            remainingCount: 19,
            capacityReached: false
          }
        ]
      }
    ]
  },
  items: [
    {
      id: 301,
      name: '多口充电器',
      spaceId: 101,
      spaceCode: 'bedroom',
      spaceName: '卧室',
      categoryId: 201,
      categoryCode: 'digital',
      categoryName: '电子数码',
      smallCategory: '充电配件',
      detailLocation: '床头柜',
      quantity: 3,
      minimumQuantity: 1,
      version: 0
    },
    {
      id: 302,
      name: '降噪耳机',
      spaceId: 101,
      spaceCode: 'bedroom',
      spaceName: '卧室',
      categoryId: 201,
      categoryCode: 'digital',
      categoryName: '电子数码',
      smallCategory: '音频设备',
      detailLocation: '书桌抽屉',
      quantity: 1,
      minimumQuantity: 1,
      version: 0
    },
    {
      id: 303,
      name: '换季衬衫',
      spaceId: 101,
      spaceCode: 'bedroom',
      spaceName: '卧室',
      categoryId: 202,
      categoryCode: 'clothes',
      categoryName: '衣物',
      smallCategory: '上装',
      detailLocation: '衣柜上层',
      quantity: 5,
      minimumQuantity: 0,
      version: 0
    },
    {
      id: 304,
      name: '抽纸',
      spaceId: 102,
      spaceCode: 'living',
      spaceName: '客厅',
      categoryId: 203,
      categoryCode: 'household',
      categoryName: '家居日用',
      smallCategory: '清洁耗材',
      detailLocation: '电视柜',
      quantity: 6,
      minimumQuantity: 2,
      version: 0
    },
    {
      id: 305,
      name: '遥控器电池',
      spaceId: 102,
      spaceCode: 'living',
      spaceName: '客厅',
      categoryId: 203,
      categoryCode: 'household',
      categoryName: '家居日用',
      smallCategory: '电池',
      detailLocation: '电视柜抽屉',
      quantity: 2,
      minimumQuantity: 2,
      version: 0
    },
    {
      id: 306,
      name: '游戏手柄',
      spaceId: 102,
      spaceCode: 'living',
      spaceName: '客厅',
      categoryId: 204,
      categoryCode: 'digital',
      categoryName: '电子数码',
      smallCategory: '娱乐设备',
      detailLocation: '电视柜',
      quantity: 1,
      minimumQuantity: 0,
      version: 0
    }
  ],
  analytics: {
    space: [
      {
        id: 101,
        code: 'bedroom',
        name: '卧室',
        itemCount: 3,
        quantity: 9,
        percent: 50
      },
      {
        id: 102,
        code: 'living',
        name: '客厅',
        itemCount: 3,
        quantity: 9,
        percent: 50
      }
    ],
    category: [
      {
        id: 201,
        code: 'digital',
        name: '电子数码',
        itemCount: 3,
        quantity: 5,
        percent: 28
      },
      {
        id: 202,
        code: 'clothes',
        name: '衣物',
        itemCount: 1,
        quantity: 5,
        percent: 28
      },
      {
        id: 203,
        code: 'household',
        name: '家居日用',
        itemCount: 2,
        quantity: 8,
        percent: 44
      }
    ]
  },
  transactions: [
    {
      id: 401,
      itemId: 304,
      itemName: '抽纸',
      type: 'OUT',
      delta: -1,
      quantityBefore: 7,
      quantityAfter: 6,
      description: '日常取用 1 件',
      spaceName: '客厅',
      categoryName: '家居日用',
      detailLocation: '电视柜',
      occurredAt: '2026-06-08T09:30:00+08:00'
    },
    {
      id: 402,
      itemId: 301,
      itemName: '多口充电器',
      type: 'IN',
      delta: 1,
      quantityBefore: 2,
      quantityAfter: 3,
      description: '新增备用充电器',
      spaceName: '卧室',
      categoryName: '电子数码',
      detailLocation: '床头柜',
      occurredAt: '2026-06-07T20:15:00+08:00'
    },
    {
      id: 403,
      itemId: 305,
      itemName: '遥控器电池',
      type: 'WARN',
      delta: 0,
      quantityBefore: 2,
      quantityAfter: 2,
      description: '库存已到达预警线',
      spaceName: '客厅',
      categoryName: '家居日用',
      detailLocation: '电视柜抽屉',
      occurredAt: '2026-06-06T18:40:00+08:00'
    }
  ]
};

/**
 * 创建一份可安全修改的游客体验数据副本。
 */
export const createGuestInventory = (): GuestInventory =>
  JSON.parse(JSON.stringify(sample)) as GuestInventory;

/**
 * 按空间、分类和关键词筛选游客体验物品。
 */
export const filterGuestItems = (
  items: InventoryItemResponse[],
  filter: {
    space: SpaceKey;
    cate: CategoryKey;
    keyword: string;
  }
) => {
  const keyword = filter.keyword.trim().toLocaleLowerCase('zh-CN');
  return items.filter((item) => {
    if (filter.space !== 'all' && item.spaceCode !== filter.space) {
      return false;
    }
    if (filter.cate !== 'all' && item.categoryCode !== filter.cate) {
      return false;
    }
    if (!keyword) return true;
    return [
      item.name,
      item.smallCategory,
      item.detailLocation,
      item.spaceName,
      item.categoryName
    ].some((value) => value.toLocaleLowerCase('zh-CN').includes(keyword));
  });
};
