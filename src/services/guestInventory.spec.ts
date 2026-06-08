import { describe, expect, it } from 'vitest';

import {
  createGuestInventory,
  filterGuestItems
} from './guestInventory';

describe('guest inventory experience', () => {
  it('creates an independent sample inventory for each guest session', () => {
    const first = createGuestInventory();
    const second = createGuestInventory();

    first.items[0].name = '已修改';

    expect(second.items[0].name).not.toBe('已修改');
    expect(second.bootstrap.spaces.length).toBeGreaterThan(1);
    expect(second.analytics.space.length).toBeGreaterThan(0);
    expect(second.transactions.length).toBeGreaterThan(0);
  });

  it('filters sample items by space, category, and keyword', () => {
    const guest = createGuestInventory();

    expect(
      filterGuestItems(guest.items, {
        space: 'bedroom',
        cate: 'digital',
        keyword: '充电'
      }).map((item) => item.name)
    ).toEqual(['多口充电器']);

    expect(
      filterGuestItems(guest.items, {
        space: 'living',
        cate: 'all',
        keyword: ''
      }).every((item) => item.spaceCode === 'living')
    ).toBe(true);
  });
});
