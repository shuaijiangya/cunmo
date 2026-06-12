package cn.cunmo.trigger.http.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.cunmo.api.membership.model.response.MembershipDashboardResponse;
import cn.cunmo.api.membership.model.response.MembershipOrderResponse;
import cn.cunmo.application.auth.port.CurrentUserProvider;
import cn.cunmo.application.membership.service.MembershipApplicationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MembershipControllerTest {
    @Mock
    private MembershipApplicationService service;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.requireUserId()).thenReturn(42L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new MembershipController(
                        service,
                        currentUserProvider))
                .build();
    }

    @Test
    void returnsDashboardContractUsedByTypescript() throws Exception {
        when(service.dashboard(42)).thenReturn(
                new MembershipDashboardResponse(
                        new MembershipDashboardResponse.Plan(
                                "FREE",
                                "免费基础版",
                                null,
                                null),
                        new MembershipDashboardResponse.Quota(
                                new MembershipDashboardResponse.QuotaUsage(
                                        2, 3, 67, false),
                                List.of(),
                                10,
                                List.of()),
                        List.of(new MembershipDashboardResponse.Product(
                                "MONTHLY_PRO",
                                "月度 PRO",
                                990,
                                "¥9.9/月",
                                30,
                                true)),
                        new MembershipDashboardResponse.Renewal(
                                true,
                                false,
                                "NONE",
                                null),
                        new MembershipDashboardResponse.Contact(
                                true,
                                "https://example.com/qr.png",
                                "13800000000")));

        mockMvc.perform(get("/api/membership/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.type").value("FREE"))
                .andExpect(jsonPath("$.quota.rootSpaces.used").value(2))
                .andExpect(jsonPath("$.quota.itemCapacityLimit").value(10))
                .andExpect(jsonPath("$.products[0].code")
                        .value("MONTHLY_PRO"))
                .andExpect(jsonPath("$.renewal.supported").value(true))
                .andExpect(jsonPath("$.contact.phone")
                        .value("13800000000"));
    }

    @Test
    void mapsOrderRequestFieldsWithoutRenaming() throws Exception {
        when(service.createOrder(42, "MONTHLY_PRO", true))
                .thenReturn(new MembershipOrderResponse(
                        "CM100",
                        "PAYING",
                        null));

        mockMvc.perform(post("/api/membership/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productCode": "MONTHLY_PRO",
                                  "autoRenew": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNo").value("CM100"))
                .andExpect(jsonPath("$.status").value("PAYING"));

        verify(service).createOrder(42, "MONTHLY_PRO", true);
    }
}
