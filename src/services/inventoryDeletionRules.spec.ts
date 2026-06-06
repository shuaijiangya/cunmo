import { describe, expect, it } from 'vitest';

import {
  deletionModeFor,
  requiresMovementTarget
} from './inventoryDeletionRules';

describe('inventory deletion rules', () => {
  it('uses direct deletion for an item without movement strategy', () => {
    expect(deletionModeFor('item')).toBe('DIRECT_DELETE');
  });

  it('keeps strategy selection for spaces and categories', () => {
    expect(deletionModeFor('space')).toBe('STRATEGY_REQUIRED');
    expect(deletionModeFor('category')).toBe('STRATEGY_REQUIRED');
  });

  it('skips movement target validation for direct item deletion', () => {
    expect(requiresMovementTarget('item', 'MOVE')).toBe(false);
    expect(requiresMovementTarget('space', 'MOVE')).toBe(true);
  });
});
