package cn.cunmo.application.config;

import cn.cunmo.application.user.port.AvatarStorage;
import cn.cunmo.application.user.service.UserProfileApplicationService;
import cn.cunmo.application.inventory.query.InventoryQueryService;
import cn.cunmo.application.inventory.service.InventoryApplicationService;
import cn.cunmo.application.inventory.service.InventoryDeletionApplicationService;
import cn.cunmo.domain.auth.repository.UserRepository;
import cn.cunmo.domain.inventory.repository.InventoryDeletionRepository;
import cn.cunmo.domain.inventory.repository.InventoryRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用层可替换基础组件配置。
 */
@Configuration
public class ApplicationBeanConfiguration {
    /**
     * 提供统一 UTC 系统时钟，测试时可替换为固定时钟。
     */
    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    /**
     * 创建用户资料应用服务。
     *
     * @param userRepository 用户聚合仓储
     * @param avatarStorage 头像存储端口
     * @return 用户资料应用服务
     */
    @Bean
    UserProfileApplicationService userProfileApplicationService(
            UserRepository userRepository,
            AvatarStorage avatarStorage) {
        return new UserProfileApplicationService(
                userRepository,
                avatarStorage);
    }

    /**
     * 创建库存领域应用服务。
     */
    @Bean
    InventoryApplicationService inventoryApplicationService(
            InventoryRepository inventoryRepository,
            InventoryQueryService inventoryQueryService) {
        return new InventoryApplicationService(
                inventoryRepository,
                inventoryQueryService);
    }

    /**
     * 创建库存结构删除应用服务。
     */
    @Bean
    InventoryDeletionApplicationService
            inventoryDeletionApplicationService(
                    InventoryRepository inventoryRepository,
                    InventoryDeletionRepository deletionRepository) {
        return new InventoryDeletionApplicationService(
                inventoryRepository,
                deletionRepository);
    }
}
