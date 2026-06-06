import type {
  DeletionScope,
  DeletionStrategy
} from '@/types/inventory';

export type DeletionMode = 'DIRECT_DELETE' | 'STRATEGY_REQUIRED';

export const deletionModeFor = (
  scope: DeletionScope
): DeletionMode =>
  scope === 'item' ? 'DIRECT_DELETE' : 'STRATEGY_REQUIRED';

export const requiresMovementTarget = (
  scope: DeletionScope,
  strategy: DeletionStrategy
): boolean =>
  deletionModeFor(scope) === 'STRATEGY_REQUIRED' &&
  strategy === 'MOVE';
