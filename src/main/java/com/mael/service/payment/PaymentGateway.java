package com.mael.service.payment;

import com.mael.entity.Payment;

/**
 * Cổng thanh toán. Hiện tại là {@link MockPaymentGateway}.
 *
 * Khi tích hợp MoMo (AIO v2, requestType=captureWallet):
 *  - createPayment: ký HMAC-SHA256 chuỗi accessKey, amount, extraData, ipnUrl, orderId, orderInfo,
 *    partnerCode, redirectUrl, requestId, requestType rồi POST /v2/gateway/api/create, lấy payUrl, qrCodeUrl.
 *  - verifyIpn: kiểm chữ ký IPN, so orderId (= payment.id) và amount với DB.
 */
public interface PaymentGateway {

    PaymentInit createPayment(Payment payment, String orderInfo);

    /** @param payUrl link trang thanh toán, @param qrCodeUrl nội dung / link QR (có thể null) */
    record PaymentInit(String payUrl, String qrCodeUrl, Integer resultCode) {
    }
}
