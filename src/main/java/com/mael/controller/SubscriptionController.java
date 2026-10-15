package com.mael.controller;

import com.mael.dto.plan.PlanView;
import com.mael.dto.plan.SubscriptionView;
import com.mael.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /** Danh sách gói (public, không cần đăng nhập) */
    @GetMapping("/plans")
    public List<PlanView> plans() {
        return subscriptionService.activePlans();
    }

    /** Gói đang có hiệu lực, 204 nếu đã hết hạn */
    @GetMapping("/subscriptions/me")
    public ResponseEntity<SubscriptionView> mySubscription() {
        SubscriptionView view = subscriptionService.mySubscription();
        return view == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(view);
    }

    @GetMapping("/subscriptions/history")
    public List<SubscriptionView> history() {
        return subscriptionService.myHistory();
    }
}
