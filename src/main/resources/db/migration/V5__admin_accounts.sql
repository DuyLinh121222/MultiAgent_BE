-- =====================================================================
-- V5: 5 tài khoản quản trị của nhóm MAEL (mật khẩu ban đầu giống nhau, mỗi người tự đổi sau lần đăng nhập đầu)
--   - Tài khoản admin mẫu admin@mael.vn đổi thành maeledu709@gmail.com (giữ nguyên user_id vì bài giảng mẫu tham chiếu tới)
--   - Mỗi admin có sẵn hồ sơ duyệt bài để dùng cổng giảng viên
-- =====================================================================

UPDATE user_account
SET email = 'maeledu709@gmail.com',
    display_name = 'MAEL Edu',
    password_hash = '$2a$10$z4aTrYPyrbmU3YRk2ZcBD.8YJQSAFbPBBY0H3exvkGmO05JR516KS'
WHERE user_id = 'u-admin';

UPDATE lecturer SET full_name = 'MAEL Edu' WHERE user_id = 'u-admin';

INSERT INTO user_account (user_id, email, password_hash, role, status, display_name) VALUES
 ('u-admin-khang', 'khangadmin214@mael.vn', '$2a$10$z4aTrYPyrbmU3YRk2ZcBD.8YJQSAFbPBBY0H3exvkGmO05JR516KS', 'ADMIN', 'ACTIVE', 'Khang'),
 ('u-admin-minh',  'minhadmin142@mael.vn',  '$2a$10$z4aTrYPyrbmU3YRk2ZcBD.8YJQSAFbPBBY0H3exvkGmO05JR516KS', 'ADMIN', 'ACTIVE', 'Minh'),
 ('u-admin-nhan',  'nhanadmin875@mael.vn',  '$2a$10$z4aTrYPyrbmU3YRk2ZcBD.8YJQSAFbPBBY0H3exvkGmO05JR516KS', 'ADMIN', 'ACTIVE', 'Nhân'),
 ('u-admin-quang', 'quangadmin647@mael.vn', '$2a$10$z4aTrYPyrbmU3YRk2ZcBD.8YJQSAFbPBBY0H3exvkGmO05JR516KS', 'ADMIN', 'ACTIVE', 'Quang')
ON CONFLICT (email) DO NOTHING;

INSERT INTO lecturer (lecturer_id, user_id, full_name, specialization)
SELECT gen_random_uuid()::text, u.user_id, u.display_name, 'Quản trị hệ thống'
FROM user_account u
WHERE u.role = 'ADMIN' AND NOT EXISTS (SELECT 1 FROM lecturer l WHERE l.user_id = u.user_id);
