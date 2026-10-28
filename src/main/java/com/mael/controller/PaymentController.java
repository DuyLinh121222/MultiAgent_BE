package com.mael.controller;

import com.mael.dto.payment.CreatePaymentRequest;
import com.mael.dto.payment.PaymentView;
import com.mael.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Tạo đơn thanh toán gói học (trạng thái PENDING) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentView create(@Valid @RequestBody CreatePaymentRequest req) {
        return paymentService.create(req);
    }

    @GetMapping
    public List<PaymentView> mine() {
        return paymentService.myPayments();
    }

    /** FE gọi để hỏi trạng thái sau khi MoMo chuyển về redirectUrl */
    @GetMapping("/{paymentId}")
    public PaymentView get(@PathVariable String paymentId) {
        return paymentService.get(paymentId);
    }

    /** Học sinh báo đã chuyển khoản qua QR MoMo, chờ admin xác nhận */
    @PostMapping("/{paymentId}/mark-paid")
    public PaymentView markPaid(@PathVariable String paymentId) {
        return paymentService.markPaid(paymentId);
    }

    /** DEV ONLY: giả lập thanh toán thành công */
    @PostMapping("/{paymentId}/mock-confirm")
    public PaymentView mockConfirm(@PathVariable String paymentId) {
        return paymentService.mockConfirm(paymentId);
    }

    /**
     * MoMo IPN - TODO khi tích hợp MoMo: kiểm chữ ký HMAC-SHA256, so orderId + amount,
     * rồi gọi paymentService.markSuccess(...). Phải trả HTTP 204 trong 15 giây.
     */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody Map<String, Object> payload) {
        log.info("Nhận MoMo IPN (chưa xử lý): orderId={}", payload.get("orderId"));
        return ResponseEntity.noContent().build();
    }
}
