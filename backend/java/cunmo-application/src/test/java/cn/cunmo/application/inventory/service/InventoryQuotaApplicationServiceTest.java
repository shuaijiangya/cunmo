package cn.cunmo.application.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.cunmo.application.exception.ApplicationException;
import cn.cunmo.application.inventory.query.InventoryQueryService;
import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.application.membership.service.MembershipQuotaPolicy;
import cn.cunmo.domain.inventory.model.aggregate.InventoryVault;
import cn.cunmo.domain.inventory.model.valueobject.VaultId;
import cn.cunmo.domain.inventory.repository.InventoryRepository;
import cn.cunmo.domain.membership.model.aggregate.MembershipEntitlement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryQuotaApplicationServiceTest {
    private static final long USER_ID = 42;
    private static final Instant NOW =
            Instant.parse("2026-06-12T08:00:00Z");
    private static final VaultId VAULT_ID = VaultId.of(7);

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private InventoryQueryService queryService;
    @Mock
    private MembershipRepository membershipRepository;

    private InventoryApplicationService service;

    @BeforeEach
    void setUp() {
        when(inventoryRepository.lockVault(USER_ID))
                .thenReturn(InventoryVault.reconstitute(
                        VAULT_ID,
                        10));
        service = new InventoryApplicationService(
                inventoryRepository,
                queryService,
                new MembershipQuotaPolicy(
                        membershipRepository,
                        Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    void rejectsFourthRootSpaceForFreeMember() {
        when(membershipRepository.findEntitlement(USER_ID))
                .thenReturn(MembershipEntitlement.free(USER_ID));
        when(inventoryRepository.countSpaces(VAULT_ID))
                .thenReturn(3);

        ApplicationException error = assertThrows(
                ApplicationException.class,
                () -> service.createSpace(USER_ID, "仓库"));

        assertEquals("ROOT_SPACE_QUOTA_EXCEEDED", error.code());
        verify(inventoryRepository, never())
                .createSpace(VAULT_ID, "仓库");
    }

    @Test
    void rejectsFourthCategoryBindingForFreeMember() {
        when(membershipRepository.findEntitlement(USER_ID))
                .thenReturn(MembershipEntitlement.free(USER_ID));
        when(inventoryRepository.countCategories(VAULT_ID, 10))
                .thenReturn(3);

        ApplicationException error = assertThrows(
                ApplicationException.class,
                () -> service.createCategory(
                        USER_ID,
                        "工具",
                        List.of(10L)));

        assertEquals("SPACE_CATEGORY_QUOTA_EXCEEDED", error.code());
    }

    @Test
    void rejectsEleventhItemForFreeMember() {
        when(membershipRepository.findEntitlement(USER_ID))
                .thenReturn(MembershipEntitlement.free(USER_ID));
        when(inventoryRepository.bindingExists(
                VAULT_ID, 10, 20))
                .thenReturn(true);
        when(inventoryRepository.countItems(
                VAULT_ID, 10, 20))
                .thenReturn(10);

        ApplicationException error = assertThrows(
                ApplicationException.class,
                () -> service.createItem(
                        USER_ID,
                        10,
                        20,
                        "螺丝刀",
                        "",
                        "",
                        1,
                        0));

        assertEquals("CAVITY_ITEM_QUOTA_EXCEEDED", error.code());
    }

    @Test
    void allowsAdditionalRootSpaceForLifetimeMember() {
        MembershipEntitlement entitlement =
                MembershipEntitlement.free(USER_ID);
        entitlement.grantLifetime(NOW, "ADMIN");
        when(membershipRepository.findEntitlement(USER_ID))
                .thenReturn(entitlement);
        when(inventoryRepository.countSpaces(VAULT_ID))
                .thenReturn(99);
        when(inventoryRepository.createSpace(VAULT_ID, "无限空间"))
                .thenReturn(100L);

        long id = service.createSpace(USER_ID, "无限空间");

        assertEquals(100L, id);
    }
}
