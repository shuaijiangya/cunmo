package cn.cunmo.trigger.http.controller;

import cn.cunmo.api.membership.model.request.CreateMembershipOrderRequest;
import cn.cunmo.api.membership.model.request.CreateUpgradeRequest;
import cn.cunmo.api.membership.model.response.MembershipDashboardResponse;
import cn.cunmo.api.membership.model.response.MembershipOrderResponse;
import cn.cunmo.api.membership.model.response.RenewalAgreementResponse;
import cn.cunmo.api.membership.model.response.UpgradeRequestResponse;
import cn.cunmo.application.auth.port.CurrentUserProvider;
import cn.cunmo.application.membership.service.MembershipApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {
    private final MembershipApplicationService service;
    private final CurrentUserProvider currentUserProvider;

    public MembershipController(
            MembershipApplicationService service,
            CurrentUserProvider currentUserProvider) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/dashboard")
    public MembershipDashboardResponse dashboard() {
        return service.dashboard(
                currentUserProvider.requireUserId());
    }

    @PostMapping("/orders")
    public MembershipOrderResponse createOrder(
            @Valid @RequestBody CreateMembershipOrderRequest request) {
        return service.createOrder(
                currentUserProvider.requireUserId(),
                request.productCode(),
                request.autoRenew());
    }

    @GetMapping("/orders/{orderNo}")
    public MembershipOrderResponse order(
            @PathVariable String orderNo) {
        return service.order(
                currentUserProvider.requireUserId(),
                orderNo);
    }

    @PostMapping("/renewal-agreements")
    public RenewalAgreementResponse createRenewalAgreement() {
        return service.createRenewalAgreement(
                currentUserProvider.requireUserId());
    }

    @PostMapping("/renewal-agreements/terminate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void terminateRenewalAgreement() {
        service.terminateRenewal(
                currentUserProvider.requireUserId());
    }

    @PostMapping("/upgrade-requests")
    public UpgradeRequestResponse submitUpgradeRequest(
            @Valid @RequestBody CreateUpgradeRequest request) {
        return service.submitUpgradeRequest(
                currentUserProvider.requireUserId(),
                request.contact(),
                request.remark());
    }
}
