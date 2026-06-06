package cn.cunmo.trigger.http.controller;

import cn.cunmo.api.inventory.model.request.AdjustStockRequest;
import cn.cunmo.api.inventory.model.request.CreateCategoryRequest;
import cn.cunmo.api.inventory.model.request.CreateInventoryItemRequest;
import cn.cunmo.api.inventory.model.request.CreateSpaceRequest;
import cn.cunmo.api.inventory.model.request.DeleteCategoryRequest;
import cn.cunmo.api.inventory.model.request.DeleteSpaceRequest;
import cn.cunmo.api.inventory.model.request.MoveInventoryItemRequest;
import cn.cunmo.api.inventory.model.request.UnbindCategoryRequest;
import cn.cunmo.api.inventory.model.request.UpdateCategorySpacesRequest;
import cn.cunmo.api.inventory.model.response.CreatedIdResponse;
import cn.cunmo.api.inventory.model.response.InventoryItemMutationResponse;
import cn.cunmo.application.auth.port.CurrentUserProvider;
import cn.cunmo.application.inventory.result.CursorPageResult;
import cn.cunmo.application.inventory.result.InventoryAnalyticsResult;
import cn.cunmo.application.inventory.result.InventoryBootstrapResult;
import cn.cunmo.application.inventory.result.InventoryItemResult;
import cn.cunmo.application.inventory.result.InventoryDeletionPreviewResult;
import cn.cunmo.application.inventory.result.StockTransactionResult;
import cn.cunmo.application.inventory.service.InventoryApplicationService;
import cn.cunmo.application.inventory.service.InventoryDeletionApplicationService;
import cn.cunmo.domain.inventory.model.aggregate.InventoryItem;
import cn.cunmo.domain.inventory.model.enums.DeletionStrategy;
import cn.cunmo.trigger.http.converter.InventoryHttpConverter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存领域 HTTP 入口。
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private static final Logger log =
            LoggerFactory.getLogger(InventoryController.class);

    private final InventoryApplicationService inventoryService;
    private final InventoryDeletionApplicationService deletionService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * 创建库存控制器。
     */
    public InventoryController(
            InventoryApplicationService inventoryService,
            InventoryDeletionApplicationService deletionService,
            CurrentUserProvider currentUserProvider) {
        this.inventoryService = inventoryService;
        this.deletionService = deletionService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 获取当前用户库存首屏数据。
     */
    @GetMapping("/bootstrap")
    public InventoryBootstrapResult bootstrap() {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_bootstrap stage=http_request_accepted userId={}",
                userId);
        return inventoryService.bootstrap(userId);
    }

    /**
     * 创建库存空间。
     */
    @PostMapping("/spaces")
    public CreatedIdResponse createSpace(
            @Valid @RequestBody CreateSpaceRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_space_create stage=http_request_accepted userId={}",
                userId);
        long id = inventoryService.createSpace(
                userId,
                request.name());
        return new CreatedIdResponse(id);
    }

    /**
     * 创建分类并绑定多个空间。
     */
    @PostMapping("/categories")
    public CreatedIdResponse createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_category_create stage=http_request_accepted userId={} spaceCount={}",
                userId,
                request.spaceIds().size());
        long id = inventoryService.createCategory(
                userId,
                request.name(),
                request.spaceIds());
        return new CreatedIdResponse(id);
    }

    /**
     * 更新分类空间绑定。
     */
    @PutMapping("/categories/{categoryId}/spaces")
    public void updateCategorySpaces(
            @PathVariable long categoryId,
            @Valid @RequestBody UpdateCategorySpacesRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_category_binding stage=http_request_accepted userId={} categoryId={} spaceCount={}",
                userId,
                categoryId,
                request.spaceIds().size());
        inventoryService.updateCategorySpaces(
                userId,
                categoryId,
                request.spaceIds());
    }

    /**
     * 查询库存物品分页。
     */
    @GetMapping("/items")
    public CursorPageResult<InventoryItemResult> items(
            @RequestParam(required = false) Long spaceId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.items(
                currentUserProvider.requireUserId(),
                spaceId,
                categoryId,
                keyword,
                cursor,
                size);
    }

    /**
     * 创建库存物品。
     */
    @PostMapping("/items")
    public InventoryItemMutationResponse createItem(
            @Valid @RequestBody CreateInventoryItemRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_item_create stage=http_request_accepted userId={} spaceId={} categoryId={}",
                userId,
                request.spaceId(),
                request.categoryId());
        InventoryItem item = inventoryService.createItem(
                userId,
                request.spaceId(),
                request.categoryId(),
                request.name(),
                request.smallCategory(),
                request.detailLocation(),
                request.quantity(),
                request.minimumQuantity());
        return InventoryHttpConverter.toMutationResponse(item);
    }

    /**
     * 调整指定物品库存。
     */
    @PostMapping("/items/{itemId}/adjustments")
    public InventoryItemMutationResponse adjustStock(
            @PathVariable long itemId,
            @Valid @RequestBody AdjustStockRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_stock_adjust stage=http_request_accepted userId={} itemId={} delta={}",
                userId,
                itemId,
                request.delta());
        InventoryItem item = inventoryService.adjustStock(
                userId,
                itemId,
                request.delta(),
                request.description());
        return InventoryHttpConverter.toMutationResponse(item);
    }

    /** 查询物品删除影响预览。 */
    @GetMapping("/items/{itemId}/deletion-preview")
    public InventoryDeletionPreviewResult previewItemDeletion(
            @PathVariable long itemId) {
        return inventoryService.previewItemDeletion(
                currentUserProvider.requireUserId(),
                itemId);
    }

    /** 将物品迁移到目标空间分类。 */
    @PostMapping("/items/{itemId}/movement")
    public void moveItem(
            @PathVariable long itemId,
            @Valid @RequestBody MoveInventoryItemRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_item_move stage=http_request_accepted userId={} itemId={} targetSpaceId={} targetCategoryId={}",
                userId,
                itemId,
                request.targetSpaceId(),
                request.targetCategoryId());
        deletionService.moveItem(
                userId,
                itemId,
                request.targetSpaceId(),
                request.targetCategoryId());
    }

    /** 清空并逻辑删除物品。 */
    @PostMapping("/items/{itemId}/deletion")
    public void deleteItem(@PathVariable long itemId) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_item_delete stage=http_request_accepted userId={} itemId={}",
                userId,
                itemId);
        deletionService.deleteItem(userId, itemId);
    }

    /** 查询空间删除影响预览。 */
    @GetMapping("/spaces/{spaceId}/deletion-preview")
    public InventoryDeletionPreviewResult previewSpaceDeletion(
            @PathVariable long spaceId) {
        return inventoryService.previewSpaceDeletion(
                currentUserProvider.requireUserId(),
                spaceId);
    }

    /** 按策略删除空间。 */
    @PostMapping("/spaces/{spaceId}/deletion")
    public void deleteSpace(
            @PathVariable long spaceId,
            @Valid @RequestBody DeleteSpaceRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_space_delete stage=http_request_accepted userId={} spaceId={} strategy={}",
                userId,
                spaceId,
                request.strategy());
        deletionService.deleteSpace(
                userId,
                spaceId,
                toDomainStrategy(request.strategy().name()),
                request.targetSpaceId());
    }

    /** 查询分类删除或解绑影响预览。 */
    @GetMapping("/categories/{categoryId}/deletion-preview")
    public InventoryDeletionPreviewResult previewCategoryDeletion(
            @PathVariable long categoryId,
            @RequestParam(required = false) Long spaceId) {
        return inventoryService.previewCategoryDeletion(
                currentUserProvider.requireUserId(),
                categoryId,
                spaceId);
    }

    /** 按策略解除分类与当前空间绑定。 */
    @PostMapping("/categories/{categoryId}/unbinding")
    public void unbindCategory(
            @PathVariable long categoryId,
            @Valid @RequestBody UnbindCategoryRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_category_unbind stage=http_request_accepted userId={} spaceId={} categoryId={} strategy={}",
                userId,
                request.spaceId(),
                categoryId,
                request.strategy());
        deletionService.unbindCategory(
                userId,
                request.spaceId(),
                categoryId,
                toDomainStrategy(request.strategy().name()),
                request.targetCategoryId());
    }

    /** 按策略全局删除分类。 */
    @PostMapping("/categories/{categoryId}/deletion")
    public void deleteCategory(
            @PathVariable long categoryId,
            @Valid @RequestBody DeleteCategoryRequest request) {
        long userId = currentUserProvider.requireUserId();
        log.info(
                "event=inventory_category_delete stage=http_request_accepted userId={} categoryId={} strategy={}",
                userId,
                categoryId,
                request.strategy());
        deletionService.deleteCategory(
                userId,
                categoryId,
                toDomainStrategy(request.strategy().name()),
                request.targetCategoryId());
    }

    /** 将 HTTP 删除策略转换为领域枚举。 */
    private DeletionStrategy toDomainStrategy(String strategy) {
        return DeletionStrategy.valueOf(strategy);
    }

    /**
     * 查询透视镜统计分页。
     */
    @GetMapping("/analytics")
    public CursorPageResult<InventoryAnalyticsResult> analytics(
            @RequestParam(defaultValue = "SPACE") String dimension,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.analytics(
                currentUserProvider.requireUserId(),
                dimension,
                cursor,
                size);
    }

    /**
     * 查询库存流转轴分页。
     */
    @GetMapping("/transactions")
    public CursorPageResult<StockTransactionResult> transactions(
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.transactions(
                currentUserProvider.requireUserId(),
                cursor,
                size);
    }
}
