-- =====================================================================
-- V3: Mở rộng cổng Quản trị viên và Giảng viên
--   - Tên hiển thị cho tài khoản quản trị (admin không có bảng hồ sơ riêng)
--   - Thống kê số lần trả lời / trả lời sai của từng câu hỏi (giảng viên xem câu nào học sinh hay sai)
--   - Nhật ký thao tác quản trị (khoá tài khoản, tạo giảng viên, sửa gói, tặng gói...)
-- =====================================================================

ALTER TABLE user_account ADD COLUMN display_name VARCHAR(150);
UPDATE user_account SET display_name = 'Quản trị viên' WHERE role = 'ADMIN' AND display_name IS NULL;

ALTER TABLE question ADD COLUMN answered_count INT NOT NULL DEFAULT 0 CHECK (answered_count >= 0);
ALTER TABLE question ADD COLUMN wrong_count    INT NOT NULL DEFAULT 0 CHECK (wrong_count >= 0);

-- Ghi chú nội bộ cho yêu cầu tư vấn (admin ghi lại đã gọi ai, hẹn khi nào)
ALTER TABLE org_lead ADD COLUMN admin_note TEXT;

CREATE TABLE audit_log (
    log_id       VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    actor_id     VARCHAR(36)  REFERENCES user_account(user_id) ON DELETE SET NULL,
    actor_name   VARCHAR(150),
    action       VARCHAR(50)  NOT NULL,
    target_type  VARCHAR(30),
    target_id    VARCHAR(36),
    detail       TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_created ON audit_log (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_payment_status_paid ON payment (status, paid_at);
CREATE INDEX IF NOT EXISTS idx_content_review_lecturer ON content_review (lecturer_id, reviewed_at DESC);
