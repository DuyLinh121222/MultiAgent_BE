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
    private final com.mael.repository.StudentRepository studentRepository;

    /**
     * Danh sách gói (public). Khách thấy cả gói học sinh, trung tâm, trường học.
     * Học sinh đã đăng nhập chỉ thấy gói dành cho học sinh.
     */
    @GetMapping("/plans")
    public List<PlanView> plans() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.mael.security.UserPrincipal p) {
            var student = studentRepository.findByUserId(p.getUserId()).orElse(null);
            if (student != null) return subscriptionService.plansFor(student);
        }
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
