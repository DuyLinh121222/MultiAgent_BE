-- =====================================================================
-- V4: Thanh toán bằng QR MoMo (quỹ "Mael") + admin xác nhận thủ công
--   - transfer_code: mã nội dung chuyển khoản riêng cho từng đơn (VD MAEL7K3Q) để admin đối chiếu
--   - customer_paid_at: lúc học sinh bấm "Tôi đã chuyển khoản"
--   - confirmed_by / admin_note: ai xác nhận hoặc từ chối, ghi chú / lý do
-- Cập nhật thông tin liên hệ: bỏ số điện thoại, email mới maeledu709@gmail.com
-- =====================================================================

ALTER TABLE payment ADD COLUMN transfer_code    VARCHAR(20) UNIQUE;
ALTER TABLE payment ADD COLUMN customer_paid_at TIMESTAMP;
ALTER TABLE payment ADD COLUMN confirmed_by     VARCHAR(36) REFERENCES user_account(user_id) ON DELETE SET NULL;
ALTER TABLE payment ADD COLUMN admin_note       TEXT;

CREATE INDEX IF NOT EXISTS idx_payment_waiting ON payment (status, customer_paid_at);

UPDATE blog_post
SET content = replace(content, 'Hotline: 0342 764 001 • Email: tduyminh0112@gmail.com', 'Email: maeledu709@gmail.com')
WHERE content LIKE '%0342 764 001%';

-- Tạo sẵn hồ sơ duyệt bài cho tài khoản admin (admin cũng duyệt nội dung được)
INSERT INTO lecturer (lecturer_id, user_id, full_name, specialization)
SELECT gen_random_uuid()::text, u.user_id, coalesce(u.display_name, 'Quản trị viên'), 'Quản trị hệ thống'
FROM user_account u
WHERE u.role = 'ADMIN' AND NOT EXISTS (SELECT 1 FROM lecturer l WHERE l.user_id = u.user_id);
