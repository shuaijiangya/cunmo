package cn.cunmo.trigger.http.controller;

import cn.cunmo.api.membership.model.request.ApproveUpgradeRequest;
import cn.cunmo.api.membership.model.request.RejectUpgradeRequest;
import cn.cunmo.application.auth.port.CurrentPermissionProvider;
import cn.cunmo.application.auth.port.CurrentUserProvider;
import cn.cunmo.application.membership.port.MembershipRepository;
import cn.cunmo.application.membership.service.AdminMembershipApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/membership")
public class AdminMembershipController {
    private static final String REVIEW_PERMISSION =
            "membership:upgrade:review";

    private final AdminMembershipApplicationService service;
    private final CurrentUserProvider currentUserProvider;
    private final CurrentPermissionProvider permissionProvider;

    public AdminMembershipController(
            AdminMembershipApplicationService service,
            CurrentUserProvider currentUserProvider,
            CurrentPermissionProvider permissionProvider) {
        this.service = service;
        this.currentUserProvider = currentUserProvider;
        this.permissionProvider = permissionProvider;
    }

    @GetMapping("/upgrade-requests")
    public List<MembershipRepository.UpgradeState> requests(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "50") int limit) {
        permissionProvider.requirePermission(REVIEW_PERMISSION);
        return service.requests(status, limit);
    }

    @PostMapping("/upgrade-requests/{requestId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(
            @PathVariable long requestId,
            @Valid @RequestBody ApproveUpgradeRequest request) {
        permissionProvider.requirePermission(REVIEW_PERMISSION);
        service.approve(
                requestId,
                currentUserProvider.requireUserId(),
                request.grantType(),
                request.months(),
                request.reviewNote());
    }

    @PostMapping("/upgrade-requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
            @PathVariable long requestId,
            @Valid @RequestBody RejectUpgradeRequest request) {
        permissionProvider.requirePermission(REVIEW_PERMISSION);
        service.reject(
                requestId,
                currentUserProvider.requireUserId(),
                request.reviewNote());
    }
}
