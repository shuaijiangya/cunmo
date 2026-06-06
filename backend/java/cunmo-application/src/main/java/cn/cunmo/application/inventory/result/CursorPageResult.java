package cn.cunmo.application.inventory.result;

import java.util.List;

/**
 * 通用游标分页结果。
 *
 * @param items 当前页数据
 * @param nextCursor 下一页游标
 * @param hasMore 是否存在下一页
 */
public record CursorPageResult<T>(
        List<T> items,
        Long nextCursor,
        boolean hasMore) {

    /**
     * 复制分页数据为不可变集合。
     */
    public CursorPageResult {
        items = List.copyOf(items);
    }
}
