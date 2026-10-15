package com.mael.service.payment;

import com.mael.entity.Payment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Giả lập cổng thanh toán. Không có payUrl thật; FE hiển thị QR mẫu và nút
 * "Giả lập thanh toán thành công" (gọi /api/payments/{id}/mock-confirm).
 */
@Service
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mock", matchIfMissing = true)
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentInit createPayment(Payment payment, String orderInfo) {
        return new PaymentInit(null, null, 0);
    }
}
