-- =====================================================================
--  MAEL - Dữ liệu mẫu (Flyway tự chạy 1 lần, ngay sau V1__schema.sql)
--
--  Tài khoản demo (mật khẩu đều là: 123456)
--    admin@mael.vn       ADMIN
--    giangvien@mael.vn   LECTURER
--    hocsinh@mael.vn     STUDENT (lớp 7, đang dùng thử 7 ngày)
--    lop9@mael.vn        STUDENT (lớp 9, đã qua 2 ải đầu Chương I, có 1 thú đồng hành)
--  Học sinh mới tự đăng ký sẽ được đưa qua màn chào mừng 3 bước (onboarded = false).
-- =====================================================================


-- ---------------------------------------------------------------------
-- Module A: tài khoản, gói, đăng ký
-- ---------------------------------------------------------------------
INSERT INTO user_account (user_id, email, password_hash, role, status) VALUES
 ('u-admin',    'admin@mael.vn',     '$2a$10$Ehxad5PZjSPcP/3gmF1H/.iVknCeRtLRtFiz1a6ROZXzpXZUQFoAu', 'ADMIN',    'ACTIVE'),
 ('u-lecturer', 'giangvien@mael.vn', '$2a$10$Ehxad5PZjSPcP/3gmF1H/.iVknCeRtLRtFiz1a6ROZXzpXZUQFoAu', 'LECTURER', 'ACTIVE'),
 ('u-student',  'hocsinh@mael.vn',   '$2a$10$Ehxad5PZjSPcP/3gmF1H/.iVknCeRtLRtFiz1a6ROZXzpXZUQFoAu', 'STUDENT',  'ACTIVE'),
 ('u-student9', 'lop9@mael.vn',      '$2a$10$Ehxad5PZjSPcP/3gmF1H/.iVknCeRtLRtFiz1a6ROZXzpXZUQFoAu', 'STUDENT',  'ACTIVE');

INSERT INTO lecturer (lecturer_id, user_id, full_name, specialization, academic_degree, workplace) VALUES
 ('lec-1', 'u-lecturer', 'Nguyễn Thị Lan', 'Đại số, Hình học THCS', 'Thạc sĩ', 'Trường THCS Nguyễn Du');

-- Danh mục thú đồng hành (FE vẽ theo pet_code). 3 thú khởi đầu, 5 thú trong rương ải trùm, 2 thú ở cửa hàng xu
INSERT INTO pet (pet_code, name, rarity, description, price_coins, is_starter, sort_order) VALUES
 ('meo-mun',   'Mèo Mun',       'COMMON',    'Lanh lợi, thích đếm số và chạy nhảy trên trục số.',            150, TRUE,  1),
 ('cun-vang',  'Cún Vàng',      'COMMON',    'Trung thành, luôn nhắc bạn kiểm tra lại đáp án.',              150, TRUE,  2),
 ('tho-bong',  'Thỏ Bông',      'COMMON',    'Nhanh nhẹn, mê mẹo tính nhẩm siêu tốc.',                       150, TRUE,  3),
 ('cu-tri',    'Cú Trí Tuệ',    'RARE',      'Phần thưởng phá đảo Chương I - người gác cổng hệ phương trình.', NULL, FALSE, 4),
 ('cao-lua',   'Cáo Lửa',       'RARE',      'Phần thưởng phá đảo Chương II - không bao giờ quên đổi chiều bất đẳng thức.', NULL, FALSE, 5),
 ('gau-truc',  'Gấu Trúc Căn',  'EPIC',      'Phần thưởng phá đảo Chương III - khai căn nhanh như ăn trúc.', NULL, FALSE, 6),
 ('rua-xanh',  'Rùa Lượng Giác','EPIC',      'Phần thưởng phá đảo Chương IV - chậm mà chắc, nhớ sin cos tan cot.', NULL, FALSE, 7),
 ('rong-con',  'Rồng Đường Tròn','LEGENDARY','Phần thưởng phá đảo Chương V - cuộn mình thành đường tròn hoàn hảo.', NULL, FALSE, 8),
 ('chim-canh-cut', 'Chim Cánh Cụt', 'RARE',  'Bình tĩnh giữa băng giá, giải đề không hoảng.',               300, FALSE, 9),
 ('ky-lan',    'Kỳ Lân Sao',    'LEGENDARY', 'Chỉ dành cho nhà vô địch tích đủ 800 xu.',                     800, FALSE, 10);

INSERT INTO student (student_id, user_id, full_name, phone, grade_level, date_of_birth, onboarded, learning_goal, coins, xp, active_pet_code, streak_days, best_streak, last_active_date, streak_shields, energy, energy_updated_at) VALUES
 ('stu-1', 'u-student',  'Minh Quân', '0901234567', 7, '2014-05-20', TRUE, 'FOLLOW_CLASS', 40, 120, 'cun-vang', 0, 2, CURRENT_DATE - 5, 0, 5, now()),
 ('stu-9', 'u-student9', 'Minh Anh',  '0907654321', 9, '2011-09-02', TRUE, 'EXAM_10',      50, 60,  'meo-mun',  2, 2, CURRENT_DATE - 1, 1, 5, now());

INSERT INTO student_pet (student_id, pet_code, source) VALUES
 ('stu-1', 'cun-vang', 'STARTER'),
 ('stu-9', 'meo-mun',  'STARTER');

-- Gói học sinh: mua trực tiếp qua MoMo. Gói trung tâm / trường học: giá theo học sinh / tháng, để lại thông tin để tư vấn.
-- Giá B2B là giá ĐỀ XUẤT, nhóm chỉnh lại theo kết quả khảo sát.
INSERT INTO subscription_plan (plan_id, name, description, price, duration_days, max_ai_sessions, audience, price_unit, min_seats, features, is_featured, sort_order, is_active) VALUES
 ('plan-trial',    'Dùng thử 7 ngày', 'Trải nghiệm đầy đủ bản đồ ải, phòng học 3 AI, giải toán bằng ảnh trong 7 ngày.', 0, 7, NULL,
  'STUDENT', 'PACKAGE', NULL, E'Đi ải và nhận thưởng\nPhòng học 3 AI\nGiải toán từ ảnh chụp', FALSE, 1, TRUE),
 ('plan-monthly',  'Gói tháng',       'Học không giới hạn cùng Thầy AI và 2 bạn AI, mở toàn bộ ải, quiz theo điểm yếu.', 99000, 30, NULL,
  'STUDENT', 'PACKAGE', NULL, E'Mở khoá toàn bộ ải Học kỳ 1\nPhòng học 3 AI không giới hạn, có giọng đọc\nGiải toán từ ảnh chụp\nQuiz củng cố theo điểm yếu', FALSE, 2, TRUE),
 ('plan-semester', 'Gói học kỳ',      'Trọn 1 học kỳ (5 tháng), tiết kiệm khoảng 20% so với mua theo tháng.', 399000, 150, NULL,
  'STUDENT', 'PACKAGE', NULL, E'Mọi quyền lợi của Gói tháng\nTiết kiệm 96.000đ so với 5 gói tháng\nTrọn một học kỳ, không lo gia hạn giữa chừng', TRUE, 3, TRUE),
 ('plan-center',   'Gói Trung tâm',   'Dành cho trung tâm dạy thêm: giáo viên giao ải cho lớp, theo dõi tiến độ từng học sinh.', 49000, 30, NULL,
  'CENTER', 'PER_STUDENT_MONTH', 30, E'Tài khoản học sinh đầy đủ quyền lợi Gói tháng\nBảng điều khiển giáo viên theo lớp\nGiao ải và quiz cho cả lớp\nBáo cáo điểm yếu từng học sinh\nGắn logo trung tâm trên trang học', TRUE, 4, TRUE),
 ('plan-school',   'Gói Trường học',  'Triển khai cho cả khối lớp, đồng bộ theo năm học, hỗ trợ tập huấn giáo viên.', 29000, 270, NULL,
  'SCHOOL', 'PER_STUDENT_MONTH', 200, E'Toàn bộ quyền lợi Gói Trung tâm\nQuản lý theo khối, lớp, giáo viên bộ môn\nThống kê toàn trường cho Ban giám hiệu\nTập huấn giáo viên khi triển khai\nHỗ trợ ưu tiên, xuất hoá đơn VAT', FALSE, 5, TRUE);

INSERT INTO subscription (subscription_id, student_id, plan_id, start_date, end_date, status, auto_renew) VALUES
 ('sub-1', 'stu-1', 'plan-trial', now() - interval '1 day', now() + interval '6 days', 'ACTIVE', FALSE),
 ('sub-9', 'stu-9', 'plan-trial', now() - interval '2 days', now() + interval '5 days', 'ACTIVE', FALSE);

INSERT INTO notification (student_id, type, title, content) VALUES
 ('stu-1', 'SYSTEM', 'Chào mừng bạn đến với MAEL!', 'Bạn có 7 ngày dùng thử miễn phí. Vào Phòng học nhóm AI để bắt đầu nhé.'),
 ('stu-9', 'SYSTEM', 'Chào mừng bạn đến với MAEL!', 'Bản đồ ải Toán 9 - Học kỳ 1 đã sẵn sàng. Qua ải để nhận xu và thú đồng hành!'),
 ('stu-1', 'QUIZ',   'Có bài luyện tập mới', 'Thầy AI đã chuẩn bị bài luyện tập Giá trị tuyệt đối cho bạn.');

-- ---------------------------------------------------------------------
-- Module B: lộ trình, chủ đề, bài giảng
-- ---------------------------------------------------------------------
INSERT INTO learning_path (path_id, name, grade_level, description) VALUES
 ('path-6', 'Toán lớp 6', 6, 'Số tự nhiên, số nguyên, phân số và hình học trực quan.'),
 ('path-7', 'Toán lớp 7', 7, 'Số hữu tỉ, số thực, tam giác và biểu thức đại số.'),
 ('path-8', 'Toán lớp 8', 8, 'Đa thức, hằng đẳng thức, phân thức và phương trình bậc nhất.'),
 ('path-9', 'Toán lớp 9', 9, 'Học kỳ 1: hệ phương trình, bất phương trình, căn thức, hệ thức lượng và đường tròn.');

INSERT INTO topic (topic_id, path_id, name, description, order_index) VALUES
 ('topic-6-1', 'path-6', 'Tập hợp các số tự nhiên', 'Tập hợp, phép tính, lũy thừa, thứ tự thực hiện phép tính.', 1),
 ('topic-6-2', 'path-6', 'Số nguyên', 'Số nguyên âm, cộng trừ nhân chia số nguyên.', 2),
 ('topic-6-3', 'path-6', 'Phân số', 'Phân số bằng nhau, so sánh và các phép tính với phân số.', 3),
 ('topic-6-4', 'path-6', 'Hình học trực quan', 'Tam giác đều, hình vuông, hình chữ nhật, hình thoi.', 4),

 ('topic-7-1', 'path-7', 'Số hữu tỉ', 'Tập hợp Q, cộng trừ nhân chia, lũy thừa số hữu tỉ.', 1),
 ('topic-7-2', 'path-7', 'Số thực & Giá trị tuyệt đối', 'Số vô tỉ, căn bậc hai số học, giá trị tuyệt đối.', 2),
 ('topic-7-3', 'path-7', 'Góc và đường thẳng song song', 'Góc kề bù, đối đỉnh, tia phân giác, hai đường thẳng song song.', 3),
 ('topic-7-4', 'path-7', 'Tam giác bằng nhau', 'Tổng ba góc, các trường hợp bằng nhau của tam giác.', 4),
 ('topic-7-5', 'path-7', 'Biểu thức đại số', 'Đơn thức, đa thức một biến, cộng trừ nhân chia đa thức.', 5),

 ('topic-8-1', 'path-8', 'Đa thức', 'Đơn thức, đa thức nhiều biến và các phép tính.', 1),
 ('topic-8-2', 'path-8', 'Hằng đẳng thức đáng nhớ', '7 hằng đẳng thức và phân tích đa thức thành nhân tử.', 2),
 ('topic-8-3', 'path-8', 'Phân thức đại số', 'Rút gọn, cộng trừ nhân chia phân thức.', 3),
 ('topic-8-4', 'path-8', 'Phương trình bậc nhất một ẩn', 'Giải phương trình và giải bài toán bằng cách lập phương trình.', 4);


-- Lớp 9 - Học kỳ 1 (SGK Kết nối tri thức với cuộc sống, Tập 1): mỗi chương là một vùng trên bản đồ ải
INSERT INTO topic (topic_id, path_id, name, description, order_index, semester, reward_pet_code) VALUES
 ('topic-9-1', 'path-9', 'Chương I. Phương trình và hệ hai phương trình bậc nhất hai ẩn', 'Khái niệm, cách giải hệ bằng phương pháp thế, phương pháp cộng đại số và giải bài toán bằng cách lập hệ.', 1, 1, 'cu-tri'),
 ('topic-9-2', 'path-9', 'Chương II. Phương trình và bất phương trình bậc nhất một ẩn', 'Phương trình tích, phương trình chứa ẩn ở mẫu, bất đẳng thức và bất phương trình bậc nhất một ẩn.', 2, 1, 'cao-lua'),
 ('topic-9-3', 'path-9', 'Chương III. Căn bậc hai và căn bậc ba', 'Căn bậc hai, căn thức bậc hai, khai căn với phép nhân, phép chia, rút gọn biểu thức chứa căn và căn bậc ba.', 3, 1, 'gau-truc'),
 ('topic-9-4', 'path-9', 'Chương IV. Hệ thức lượng trong tam giác vuông', 'Tỉ số lượng giác của góc nhọn, hệ thức giữa cạnh và góc trong tam giác vuông và ứng dụng thực tế.', 4, 1, 'rua-xanh'),
 ('topic-9-5', 'path-9', 'Chương V. Đường tròn', 'Đường tròn, cung và dây, độ dài cung, hình quạt, vị trí tương đối của đường thẳng và đường tròn, của hai đường tròn.', 5, 1, 'rong-con');

INSERT INTO lesson (lesson_id, topic_id, title, content, duration_minutes, order_index, created_by, review_status, reviewed_by, reviewed_at) VALUES
 -- Lớp 7
 ('lesson-7-1-1', 'topic-7-1', 'Tập hợp các số hữu tỉ',
  E'Số hữu tỉ là số viết được dưới dạng phân số a/b với a, b là số nguyên và b ≠ 0.\nTập hợp các số hữu tỉ kí hiệu là Q.\n\nVí dụ: 3 = 3/1; -0,5 = -1/2; 1 2/3 = 5/3 đều là số hữu tỉ.\n\nMỗi số hữu tỉ được biểu diễn bởi một điểm trên trục số. Hai số hữu tỉ đối nhau nằm cách đều điểm 0.',
  15, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '20 days'),
 ('lesson-7-1-2', 'topic-7-1', 'Cộng, trừ, nhân, chia số hữu tỉ',
  E'Muốn cộng, trừ hai số hữu tỉ, ta viết chúng dưới dạng phân số cùng mẫu dương rồi cộng, trừ tử số.\n\nQuy tắc chuyển vế: khi chuyển một số hạng từ vế này sang vế kia của một đẳng thức, ta phải đổi dấu số hạng đó.\n\nVí dụ: x + 2/3 = 1/2  ⇒  x = 1/2 - 2/3 = -1/6.',
  20, 2, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '20 days'),
 ('lesson-7-2-1', 'topic-7-2', 'Số vô tỉ và căn bậc hai số học',
  E'Số vô tỉ là số viết được dưới dạng số thập phân vô hạn không tuần hoàn, ví dụ √2 = 1,41421356...\n\nCăn bậc hai số học của số a không âm là số x không âm sao cho x² = a. Kí hiệu √a.\n\nVí dụ: √16 = 4 vì 4 ≥ 0 và 4² = 16.',
  15, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '15 days'),
 ('lesson-7-2-2', 'topic-7-2', 'Giá trị tuyệt đối của một số thực',
  E'Giá trị tuyệt đối của số a, kí hiệu |a|, là khoảng cách từ điểm a đến điểm 0 trên trục số.\n\n• |a| = a nếu a ≥ 0\n• |a| = -a nếu a < 0\n• |a| luôn ≥ 0\n\nGiải phương trình |A| = m:\n• Nếu m < 0: vô nghiệm.\n• Nếu m ≥ 0: A = m hoặc A = -m.\n\nVí dụ: |2x - 3| = 7  ⇒  2x - 3 = 7 hoặc 2x - 3 = -7  ⇒  x = 5 hoặc x = -2.\n\nBẫy thường gặp: quên trường hợp A = -m, hoặc chuyển vế mà không đổi dấu.',
  20, 2, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '15 days'),
 ('lesson-7-3-1', 'topic-7-3', 'Góc ở vị trí đặc biệt',
  E'Hai góc kề bù có tổng số đo bằng 180°.\nHai góc đối đỉnh thì bằng nhau.\n\nTia phân giác của một góc là tia nằm giữa hai cạnh và tạo với hai cạnh hai góc bằng nhau.',
  15, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('lesson-7-3-2', 'topic-7-3', 'Hai đường thẳng song song',
  E'Nếu đường thẳng c cắt hai đường thẳng a, b và trong các góc tạo thành có một cặp góc so le trong bằng nhau (hoặc đồng vị bằng nhau) thì a // b.\n\nTiên đề Euclid: qua một điểm ở ngoài một đường thẳng chỉ có một đường thẳng song song với đường thẳng đó.',
  20, 2, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('lesson-7-4-1', 'topic-7-4', 'Tổng các góc trong một tam giác',
  E'Tổng ba góc của một tam giác bằng 180°.\nTrong tam giác vuông, hai góc nhọn phụ nhau (tổng bằng 90°).',
  15, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '5 days'),
 ('lesson-7-4-2', 'topic-7-4', 'Các trường hợp bằng nhau của tam giác',
  E'Ba trường hợp bằng nhau: cạnh - cạnh - cạnh (c.c.c), cạnh - góc - cạnh (c.g.c), góc - cạnh - góc (g.c.g).',
  25, 2, 'u-admin', 'PENDING_REVIEW', NULL, NULL),
 ('lesson-7-5-1', 'topic-7-5', 'Biểu thức đại số và đơn thức',
  E'Biểu thức đại số là biểu thức gồm các số và chữ nối với nhau bởi các phép tính.\nĐơn thức là biểu thức chỉ gồm một số, một biến hoặc tích giữa các số và biến.',
  15, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '3 days'),
 -- Lớp 6
 ('lesson-6-1-1', 'topic-6-1', 'Tập hợp và phần tử', E'Tập hợp là một nhóm các đối tượng xác định. Kí hiệu a ∈ A nghĩa là a thuộc tập hợp A.', 15, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '30 days'),
 ('lesson-6-3-1', 'topic-6-3', 'Phân số bằng nhau', E'Hai phân số a/b và c/d bằng nhau nếu a·d = b·c.', 15, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '30 days'),
 -- Lớp 8
 ('lesson-8-2-1', 'topic-8-2', 'Bình phương của một tổng, một hiệu', E'(a + b)² = a² + 2ab + b²\n(a - b)² = a² - 2ab + b²\na² - b² = (a - b)(a + b)', 20, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '30 days'),
 ('lesson-8-4-1', 'topic-8-4', 'Phương trình bậc nhất một ẩn', E'Phương trình ax + b = 0 (a ≠ 0) có nghiệm duy nhất x = -b/a.', 20, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '30 days');

-- Lớp 9 - Học kỳ 1: mỗi bài là 1 ải, có 3 câu thử thách và danh sách lỗi sai hay gặp cho bạn AI phản biện
INSERT INTO lesson (lesson_id, topic_id, title, content, duration_minutes, order_index, misconceptions, created_by, review_status, reviewed_by, reviewed_at) VALUES
 ('lesson-9-1-1', 'topic-9-1', 'Khái niệm phương trình và hệ hai phương trình bậc nhất hai ẩn',
  E'Phương trình bậc nhất hai ẩn x, y có dạng ax + by = c, trong đó a, b, c là các số đã biết và a, b không đồng thời bằng 0.\n\nCặp số (x₀; y₀) là một nghiệm của phương trình nếu ax₀ + by₀ = c.\n• Phương trình bậc nhất hai ẩn luôn có VÔ SỐ nghiệm.\n• Trên mặt phẳng toạ độ, tập nghiệm được biểu diễn bởi một đường thẳng.\n\nHệ hai phương trình bậc nhất hai ẩn gồm hai phương trình bậc nhất hai ẩn. Nghiệm của hệ là nghiệm CHUNG của cả hai phương trình.\n\nVí dụ: Cặp số (2; 1) là nghiệm của hệ { x + y = 3 ; x − y = 1 } vì 2 + 1 = 3 và 2 − 1 = 1.',
  20, 1, '[{"wrong": "Phương trình 2x + 3y = 7 chỉ có đúng một nghiệm là (2; 1) thôi!", "why": "Nhầm phương trình bậc nhất hai ẩn với phương trình một ẩn.", "fix": "Phương trình bậc nhất hai ẩn có vô số nghiệm: (2; 1) là một nghiệm, (−1; 3) cũng là nghiệm vì 2·(−1) + 3·3 = 7."}, {"wrong": "Cặp số (1; 2) là nghiệm của hệ x + y = 3 và x − y = 1 vì nó thoả mãn phương trình đầu rồi.", "why": "Chỉ kiểm tra một phương trình của hệ.", "fix": "Nghiệm của hệ phải thoả mãn cả hai phương trình. (1; 2) có 1 − 2 = −1 ≠ 1 nên không phải nghiệm; nghiệm đúng là (2; 1)."}, {"wrong": "0x + 0y = 5 cũng là phương trình bậc nhất hai ẩn mà.", "why": "Quên điều kiện a, b không đồng thời bằng 0.", "fix": "Phương trình bậc nhất hai ẩn cần a và b không đồng thời bằng 0, nên 0x + 0y = 5 không phải phương trình bậc nhất hai ẩn."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-1-2', 'topic-9-1', 'Giải hệ hai phương trình bậc nhất hai ẩn',
  E'Phương pháp thế:\n1. Từ một phương trình, biểu diễn một ẩn theo ẩn kia.\n2. Thế vào phương trình CÒN LẠI để được phương trình một ẩn, giải ra.\n3. Thay ngược lại để tìm ẩn còn lại.\n\nPhương pháp cộng đại số:\n1. Nhân hai vế mỗi phương trình với số thích hợp để hệ số của một ẩn bằng nhau hoặc đối nhau.\n2. Cộng (hoặc trừ) từng vế hai phương trình để khử ẩn đó.\n3. Giải phương trình một ẩn rồi suy ra ẩn còn lại.\n\nVí dụ: { x + y = 5 ; 2x − y = 1 }. Cộng vế với vế: 3x = 6 ⇒ x = 2, suy ra y = 3. Nghiệm (2; 3).\n\nMột hệ có thể có một nghiệm duy nhất, vô nghiệm hoặc vô số nghiệm.',
  25, 2, '[{"wrong": "Hệ x + y = 5 và 2x − y = 1: cộng hai phương trình được 3x = 4 nên x = 4/3.", "why": "Cộng vế trái nhưng quên cộng vế phải 5 + 1.", "fix": "Cộng vế với vế: 3x = 5 + 1 = 6, nên x = 2 và y = 3."}, {"wrong": "Rút y = 5 − x từ x + y = 5 rồi thế lại vào chính phương trình x + y = 5 là ra nghiệm.", "why": "Thế vào chính phương trình vừa rút.", "fix": "Phải thế vào phương trình còn lại 2x − y = 1; thế vào chính nó chỉ được 5 = 5, không tìm được x."}, {"wrong": "Khi trừ hai phương trình cho nhau, chỉ cần trừ vế trái, vế phải giữ nguyên.", "why": "Biến đổi không đồng thời cả hai vế.", "fix": "Trừ từng vế: vế trái trừ vế trái, vế phải trừ vế phải thì mới được phương trình tương đương."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-1-3', 'topic-9-1', 'Giải bài toán bằng cách lập hệ phương trình',
  E'Các bước giải:\nBước 1. Chọn hai ẩn và đặt điều kiện thích hợp cho ẩn; biểu diễn các đại lượng chưa biết theo ẩn; lập hệ phương trình.\nBước 2. Giải hệ phương trình.\nBước 3. Kiểm tra nghiệm có thoả mãn điều kiện không rồi kết luận.\n\nVí dụ: Mua 3 bút và 2 vở hết 26 nghìn đồng; mua 1 bút và 2 vở hết 18 nghìn đồng. Tính giá mỗi bút, mỗi vở.\nGọi x, y (nghìn đồng) là giá một bút và một vở (x > 0, y > 0).\nTa có hệ { 3x + 2y = 26 ; x + 2y = 18 }. Trừ vế với vế: 2x = 8 ⇒ x = 4, y = 7.\nKiểm tra: 3·4 + 2·7 = 26 đúng. Vậy bút giá 4 nghìn, vở giá 7 nghìn đồng.',
  25, 3, '[{"wrong": "Giải xong hệ là kết luận luôn, không cần đối chiếu điều kiện của ẩn.", "why": "Bỏ qua bước kiểm tra điều kiện.", "fix": "Phải kiểm tra điều kiện (số người là số tự nhiên, giá tiền dương, ...) rồi mới kết luận."}, {"wrong": "Bài bút và vở: giải được x = 4, y = 7 nghĩa là bút giá 7 nghìn, vở giá 4 nghìn.", "why": "Nhầm lẫn ẩn đã đặt.", "fix": "Ta đặt x là giá bút nên bút 4 nghìn, vở 7 nghìn. Kiểm tra: 3·4 + 2·7 = 26 đúng, còn 3·7 + 2·4 = 29 sai."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-2-1', 'topic-9-2', 'Phương trình quy về phương trình bậc nhất một ẩn',
  E'Phương trình tích: (ax + b)(cx + d) = 0 ⇔ ax + b = 0 hoặc cx + d = 0.\nVí dụ: (x − 2)(2x + 6) = 0 ⇔ x − 2 = 0 hoặc 2x + 6 = 0 ⇔ x = 2 hoặc x = −3.\n\nPhương trình chứa ẩn ở mẫu:\nBước 1. Tìm điều kiện xác định (ĐKXĐ): các mẫu khác 0.\nBước 2. Quy đồng mẫu rồi khử mẫu.\nBước 3. Giải phương trình vừa nhận được.\nBước 4. Đối chiếu ĐKXĐ, loại giá trị không thoả mãn rồi kết luận.\n\nVí dụ: (x + 1)/(x − 1) = 2/(x − 1). ĐKXĐ: x ≠ 1. Khử mẫu: x + 1 = 2 ⇒ x = 1 (loại). Phương trình vô nghiệm.',
  25, 1, '[{"wrong": "(x − 2)(x + 3) = 0 thì chỉ cần x − 2 = 0, nên x = 2 là xong.", "why": "Bỏ sót một thừa số.", "fix": "Tích bằng 0 khi một trong các thừa số bằng 0, nên x = 2 hoặc x = −3."}, {"wrong": "Giải (x + 1)/(x − 1) = 2/(x − 1): khử mẫu được x = 1, vậy nghiệm là x = 1.", "why": "Quên đối chiếu điều kiện xác định.", "fix": "ĐKXĐ là x ≠ 1 nên x = 1 bị loại, phương trình vô nghiệm."}, {"wrong": "(x − 2)(x + 3) = 6 thì x − 2 = 6 hoặc x + 3 = 6.", "why": "Áp dụng quy tắc phương trình tích khi vế phải khác 0.", "fix": "Quy tắc tách chỉ dùng khi tích bằng 0. Ở đây phải khai triển: x² + x − 12 = 0 rồi mới giải."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-2-2', 'topic-9-2', 'Bất đẳng thức và tính chất',
  E'Hệ thức a > b, a < b, a ≥ b, a ≤ b gọi là bất đẳng thức.\n\nTính chất:\n• Cộng cùng một số vào hai vế: a > b ⇒ a + c > b + c (giữ nguyên chiều).\n• Nhân với số dương: a > b, c > 0 ⇒ ac > bc (giữ nguyên chiều).\n• Nhân với số âm: a > b, c < 0 ⇒ ac < bc (ĐỔI chiều).\n• Bắc cầu: a > b và b > c ⇒ a > c.\n\nVí dụ: Từ 3 < 5 nhân hai vế với −2 được −6 > −10.',
  20, 2, '[{"wrong": "Từ a > b nhân cả hai vế với −2 ta được −2a > −2b.", "why": "Quên đổi chiều khi nhân với số âm.", "fix": "Nhân hai vế với số âm phải đổi chiều: −2a < −2b."}, {"wrong": "a > b thì a² > b² luôn đúng.", "why": "Bình phương hai vế không giữ chiều với số âm.", "fix": "Ví dụ 1 > −3 nhưng 1² = 1 < 9 = (−3)². Chỉ đúng khi a, b không âm."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-2-3', 'topic-9-2', 'Bất phương trình bậc nhất một ẩn',
  E'Bất phương trình bậc nhất một ẩn có dạng ax + b > 0 (hoặc <, ≥, ≤), với a ≠ 0.\n\nQuy tắc chuyển vế: chuyển một hạng tử sang vế kia thì phải đổi dấu hạng tử đó.\nQuy tắc nhân: nhân (chia) hai vế với số dương thì giữ chiều; với số âm thì đổi chiều.\n\nVí dụ: −2x + 6 > 0 ⇔ −2x > −6 ⇔ x < 3 (chia cho −2 nên đổi chiều).',
  25, 3, '[{"wrong": "−2x > −6, chia hai vế cho −2 được x > 3.", "why": "Quên đổi chiều khi chia cho số âm.", "fix": "Chia hai vế cho số âm phải đổi chiều: x < 3."}, {"wrong": "−2x + 6 > 0, chuyển 6 sang vế phải giữ nguyên dấu được −2x > 6.", "why": "Chuyển vế không đổi dấu.", "fix": "Chuyển vế phải đổi dấu: −2x > −6."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-3-1', 'topic-9-3', 'Căn bậc hai và căn thức bậc hai',
  E'Căn bậc hai của số a không âm là số x sao cho x² = a.\n• Số a > 0 có đúng hai căn bậc hai là √a và −√a.\n• Số 0 có một căn bậc hai là 0. Số âm không có căn bậc hai.\n• Kí hiệu √a chỉ căn bậc hai KHÔNG ÂM (căn bậc hai số học) của a. Ví dụ √9 = 3.\n\nCăn thức bậc hai √A xác định khi A ≥ 0.\nHằng đẳng thức: √(A²) = |A|. Ví dụ √((−5)²) = |−5| = 5.',
  20, 1, '[{"wrong": "√9 = ±3 vì cả 3 và −3 bình phương lên đều bằng 9 mà!", "why": "Nhầm \"căn bậc hai của 9\" với kí hiệu √9.", "fix": "9 có hai căn bậc hai là 3 và −3, nhưng kí hiệu √9 chỉ căn bậc hai không âm, nên √9 = 3."}, {"wrong": "√((−5)²) = −5.", "why": "Quên hằng đẳng thức √(A²) = |A|.", "fix": "√((−5)²) = |−5| = 5. Kết quả của căn bậc hai số học luôn không âm."}, {"wrong": "√(x − 3) xác định khi x > 3.", "why": "Bỏ trường hợp biểu thức dưới căn bằng 0.", "fix": "√(x − 3) xác định khi x − 3 ≥ 0, tức x ≥ 3 (x = 3 vẫn được vì √0 = 0)."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-3-2', 'topic-9-3', 'Khai căn bậc hai với phép nhân và phép chia',
  E'Với A ≥ 0, B ≥ 0: √(A·B) = √A · √B.\nVới A ≥ 0, B > 0: √(A/B) = √A / √B.\n\nVí dụ: √(9·25) = 3·5 = 15; √50 · √2 = √100 = 10; √(49/64) = 7/8.\n\nLưu ý: KHÔNG có quy tắc khai căn một tổng hay hiệu: √(a + b) ≠ √a + √b.',
  20, 2, '[{"wrong": "√(9 + 16) = √9 + √16 = 3 + 4 = 7.", "why": "Áp dụng sai quy tắc cho phép cộng.", "fix": "Không có quy tắc khai căn một tổng: √(9 + 16) = √25 = 5."}, {"wrong": "√(−4) · √(−9) = √36 = 6.", "why": "Dùng quy tắc nhân khi biểu thức dưới căn âm.", "fix": "Quy tắc chỉ dùng khi A, B ≥ 0; √(−4) không xác định trong tập số thực."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-3-3', 'topic-9-3', 'Biến đổi đơn giản và rút gọn biểu thức chứa căn thức bậc hai',
  E'Đưa thừa số ra ngoài dấu căn: √(A²·B) = |A|·√B (B ≥ 0). Ví dụ √12 = √(4·3) = 2√3.\nĐưa thừa số vào trong dấu căn: A√B = √(A²B) nếu A ≥ 0.\nTrục căn thức ở mẫu: A/√B = A√B / B (B > 0). Ví dụ 6/√3 = 6√3/3 = 2√3.\nDùng biểu thức liên hợp: 1/(√a − √b) = (√a + √b)/(a − b).\n\nRút gọn: đưa các căn về căn đồng dạng rồi cộng, trừ hệ số.\nVí dụ: √12 + √27 = 2√3 + 3√3 = 5√3.',
  30, 3, '[{"wrong": "√12 + √27 = √39.", "why": "Cộng trực tiếp các biểu thức dưới dấu căn.", "fix": "Chỉ cộng được các căn đồng dạng: √12 + √27 = 2√3 + 3√3 = 5√3."}, {"wrong": "Trục căn thức ở mẫu: 1/√3 = 1/3.", "why": "Nhân mẫu mà quên nhân tử.", "fix": "Nhân cả tử và mẫu với √3: 1/√3 = √3/3."}, {"wrong": "√(5x²) = x√5 với mọi x.", "why": "Quên giá trị tuyệt đối khi đưa thừa số ra ngoài.", "fix": "√(5x²) = |x|√5; chỉ bằng x√5 khi x ≥ 0."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-3-4', 'topic-9-3', 'Căn bậc ba và căn thức bậc ba',
  E'Căn bậc ba của số a là số x sao cho x³ = a, kí hiệu ∛a.\n• Mọi số thực đều có duy nhất một căn bậc ba (kể cả số âm).\n• ∛(a³) = a.\nVí dụ: ∛8 = 2; ∛(−27) = −3 vì (−3)³ = −27.\n\nCăn thức bậc ba ∛A xác định với mọi giá trị của A.',
  15, 4, '[{"wrong": "Số âm không có căn bậc ba, giống như căn bậc hai.", "why": "Nhầm tính chất của căn bậc hai sang căn bậc ba.", "fix": "Số âm vẫn có căn bậc ba: ∛(−27) = −3 vì (−3)³ = −27."}, {"wrong": "∛8 = ±2.", "why": "Nghĩ căn bậc ba có hai giá trị như căn bậc hai.", "fix": "Mỗi số chỉ có một căn bậc ba: ∛8 = 2, vì (−2)³ = −8 chứ không bằng 8."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-4-1', 'topic-9-4', 'Tỉ số lượng giác của góc nhọn',
  E'Cho tam giác ABC vuông tại A, xét góc nhọn B:\n• sin B = cạnh đối / cạnh huyền = AC/BC\n• cos B = cạnh kề / cạnh huyền = AB/BC\n• tan B = cạnh đối / cạnh kề = AC/AB\n• cot B = cạnh kề / cạnh đối = AB/AC\nMẹo nhớ: "Sin đi học (đối/huyền), Cos không hư (kề/huyền), Tan đoàn kết (đối/kề), Cot kết đoàn (kề/đối)".\n\nHai góc phụ nhau (tổng 90°): sin góc này = cos góc kia, tan góc này = cot góc kia.\nGiá trị đặc biệt: sin 30° = 1/2, cos 30° = √3/2, tan 45° = 1, sin 60° = √3/2, cos 60° = 1/2.\nVới góc nhọn: 0 < sin, cos < 1.',
  25, 1, '[{"wrong": "sin B bằng cạnh kề chia cạnh huyền.", "why": "Nhầm sin với cos.", "fix": "sin B = cạnh đối / cạnh huyền; cạnh kề / cạnh huyền là cos B."}, {"wrong": "sin 30° = cos 30° vì cùng là một góc.", "why": "Nghĩ sin và cos của cùng một góc bằng nhau.", "fix": "sin 30° = 1/2 còn cos 30° = √3/2. Ta có sin 30° = cos 60° vì 30° và 60° phụ nhau."}, {"wrong": "Có góc nhọn có sin lớn hơn 1, ví dụ sin 80°.", "why": "Quên cạnh huyền là cạnh lớn nhất.", "fix": "Cạnh đối luôn nhỏ hơn cạnh huyền nên 0 < sin của góc nhọn < 1."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-4-2', 'topic-9-4', 'Hệ thức giữa cạnh và góc trong tam giác vuông và ứng dụng',
  E'Trong tam giác vuông, mỗi cạnh góc vuông bằng:\n• cạnh huyền nhân sin góc đối, hoặc nhân cos góc kề;\n• cạnh góc vuông kia nhân tan góc đối, hoặc nhân cot góc kề.\n\nTam giác ABC vuông tại A: AC = BC·sin B = BC·cos C; AB = BC·cos B = AC·cot B.\nVí dụ: BC = 10, góc B = 30° ⇒ AC = 10·sin 30° = 5; AB = 10·cos 30° = 5√3.\n\nỨng dụng: đo chiều cao cây. Đứng cách gốc cây d mét, nhìn ngọn cây dưới góc α so với mặt đất thì chiều cao h = d·tan α (bỏ qua chiều cao của mắt).',
  30, 2, '[{"wrong": "AC = BC·cos B vì AC nằm đối diện góc B.", "why": "Dùng cos cho cạnh đối.", "fix": "Cạnh đối diện góc B dùng sin: AC = BC·sin B; cos B dùng cho cạnh kề AB."}, {"wrong": "Đứng cách cây 10 m, nhìn ngọn cây dưới góc 45° thì cây cao 10·sin 45°.", "why": "Dùng sin khi biết cạnh kề.", "fix": "Biết cạnh kề (khoảng cách) tìm cạnh đối (chiều cao) phải dùng tan: h = 10·tan 45° = 10 m."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-5-1', 'topic-9-5', 'Mở đầu về đường tròn',
  E'Đường tròn tâm O bán kính R (R > 0), kí hiệu (O; R), là hình gồm các điểm cách O một khoảng bằng R.\n\nVị trí của điểm M với (O; R):\n• OM < R: M nằm trong đường tròn.\n• OM = R: M nằm trên đường tròn.\n• OM > R: M nằm ngoài đường tròn.\n\nTâm O là tâm đối xứng của đường tròn; mọi đường kính đều là trục đối xứng (đường tròn có vô số trục đối xứng).\nĐường kính dài 2R và là dây lớn nhất của đường tròn.',
  20, 1, '[{"wrong": "Điểm M có OM = R thì M nằm trong đường tròn.", "why": "Nhầm vị trí \"trên\" với \"trong\".", "fix": "OM = R thì M nằm trên đường tròn; chỉ khi OM < R thì M mới nằm trong."}, {"wrong": "Đường tròn chỉ có một trục đối xứng thôi.", "why": "Nghĩ chỉ có một đường kính.", "fix": "Mọi đường kính đều là trục đối xứng nên đường tròn có vô số trục đối xứng."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-5-2', 'topic-9-5', 'Cung và dây của một đường tròn',
  E'Góc ở tâm là góc có đỉnh trùng với tâm đường tròn.\n• Số đo cung nhỏ bằng số đo góc ở tâm chắn cung đó.\n• Số đo cung lớn = 360° − số đo cung nhỏ. Nửa đường tròn có số đo 180°.\n\nĐường kính và dây:\n• Đường kính vuông góc với một dây thì đi qua trung điểm của dây ấy.\n• Ví dụ: (O; 5 cm), dây AB = 8 cm. Kẻ OH ⊥ AB thì HA = 4 cm, OH = √(5² − 4²) = 3 cm.',
  25, 2, '[{"wrong": "Góc ở tâm AOB = 60° thì cung lớn AB cũng bằng 60°.", "why": "Không phân biệt cung nhỏ và cung lớn.", "fix": "Cung nhỏ AB = 60°, còn cung lớn AB = 360° − 60° = 300°."}, {"wrong": "Đường kính vuông góc với dây thì không đi qua trung điểm của dây.", "why": "Nhớ ngược tính chất.", "fix": "Đường kính vuông góc với một dây thì đi qua trung điểm của dây đó."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-5-3', 'topic-9-5', 'Độ dài cung tròn, diện tích hình quạt tròn và hình vành khuyên',
  E'Độ dài đường tròn: C = 2πR. Độ dài cung n°: l = πRn/180.\nDiện tích hình tròn: S = πR². Diện tích hình quạt tròn n°: S = πR²n/360 = l·R/2.\nHình vành khuyên giới hạn bởi (O; R) và (O; r) với R > r: S = π(R² − r²).\n\nVí dụ: Cung 60° của đường tròn bán kính 6 cm dài π·6·60/180 = 2π cm.',
  25, 3, '[{"wrong": "Độ dài cung 90° của đường tròn bán kính 4 là π·4·90/360 = π.", "why": "Nhầm công thức độ dài cung với diện tích quạt.", "fix": "l = πRn/180 = π·4·90/180 = 2π. Mẫu 360 là của diện tích hình quạt."}, {"wrong": "Diện tích hình vành khuyên bằng π(R − r)².", "why": "Trừ bán kính trước rồi mới bình phương.", "fix": "S = πR² − πr² = π(R² − r²). Ví dụ R = 5, r = 3 cho 16π chứ không phải 4π."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-5-4', 'topic-9-5', 'Vị trí tương đối của đường thẳng và đường tròn',
  E'Gọi d là khoảng cách từ tâm O đến đường thẳng a, R là bán kính:\n• d < R: a cắt đường tròn tại 2 điểm.\n• d = R: a tiếp xúc với đường tròn (1 điểm chung), a là tiếp tuyến.\n• d > R: a và đường tròn không giao nhau.\n\nTiếp tuyến vuông góc với bán kính tại tiếp điểm.\nHai tiếp tuyến cắt nhau tại M (tiếp điểm A, B): MA = MB, MO là tia phân giác của góc AMB.',
  25, 4, '[{"wrong": "d = R thì đường thẳng cắt đường tròn tại hai điểm.", "why": "Nhầm trường hợp tiếp xúc với cắt nhau.", "fix": "d = R thì đường thẳng tiếp xúc với đường tròn, chỉ có 1 điểm chung."}, {"wrong": "Tiếp tuyến tạo với bán kính tại tiếp điểm một góc 45°.", "why": "Quên tính chất vuông góc.", "fix": "Tiếp tuyến luôn vuông góc với bán kính đi qua tiếp điểm (góc 90°)."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('lesson-9-5-5', 'topic-9-5', 'Vị trí tương đối của hai đường tròn',
  E'Cho (O; R) và (O''; r) với R ≥ r, d = OO'':\n• Cắt nhau (2 điểm chung): R − r < d < R + r.\n• Tiếp xúc ngoài: d = R + r. Tiếp xúc trong: d = R − r > 0.\n• Ở ngoài nhau: d > R + r. Đựng nhau: d < R − r. Đồng tâm: d = 0.\n\nVí dụ: R = 5, r = 3, d = 8 = R + r nên hai đường tròn tiếp xúc ngoài.',
  25, 5, '[{"wrong": "R = 5, r = 3, d = 8 thì hai đường tròn cắt nhau.", "why": "Không so sánh d với R + r.", "fix": "d = R + r = 8 nên hai đường tròn tiếp xúc ngoài (1 điểm chung)."}, {"wrong": "Hai đường tròn tiếp xúc trong khi d = R + r.", "why": "Nhầm điều kiện tiếp xúc trong và tiếp xúc ngoài.", "fix": "Tiếp xúc trong khi d = R − r; còn d = R + r là tiếp xúc ngoài."}]'::jsonb, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days');


-- Lỗi sai hay gặp của các bài lớp 6-8 (để bạn AI phản biện nói sai đúng chủ đề bài)
UPDATE lesson SET misconceptions = '[{"wrong": "0,5 không phải số hữu tỉ vì nó là số thập phân chứ không phải phân số.", "why": "Nghĩ số thập phân không viết được dưới dạng phân số.", "fix": "0,5 = 1/2 nên là số hữu tỉ. Mọi số thập phân hữu hạn đều là số hữu tỉ."}, {"wrong": "Số 3 là số nguyên nên không thuộc tập Q.", "why": "Không biết N ⊂ Z ⊂ Q.", "fix": "3 = 3/1 nên 3 ∈ Q. Mọi số nguyên đều là số hữu tỉ."}]'::jsonb WHERE lesson_id = 'lesson-7-1-1';
UPDATE lesson SET misconceptions = '[{"wrong": "Khi chuyển một số hạng sang vế kia mình giữ nguyên dấu cũng được mà, kết quả vẫn vậy thôi!", "why": "Quên quy tắc chuyển vế.", "fix": "Chuyển vế phải đổi dấu: x + 2/3 = 1/2 ⇒ x = 1/2 − 2/3 = −1/6."}, {"wrong": "1/2 + 1/3 = 2/5 vì cộng tử với tử, mẫu với mẫu.", "why": "Cộng phân số không quy đồng.", "fix": "Phải quy đồng: 1/2 + 1/3 = 3/6 + 2/6 = 5/6."}]'::jsonb WHERE lesson_id = 'lesson-7-1-2';
UPDATE lesson SET misconceptions = '[{"wrong": "√16 = ±4 vì cả 4 và −4 bình phương đều bằng 16.", "why": "Nhầm căn bậc hai số học.", "fix": "√16 chỉ căn bậc hai không âm nên √16 = 4."}, {"wrong": "√2 = 1,41 nên √2 là số hữu tỉ.", "why": "Lấy giá trị làm tròn thay cho giá trị đúng.", "fix": "√2 = 1,41421356... là số thập phân vô hạn không tuần hoàn nên là số vô tỉ; 1,41 chỉ là giá trị gần đúng."}]'::jsonb WHERE lesson_id = 'lesson-7-2-1';
UPDATE lesson SET misconceptions = '[{"wrong": "Mình nghĩ giá trị tuyệt đối của số âm vẫn là số âm, ví dụ |-3| = -3 đúng không các bạn?", "why": "Quên |a| là khoảng cách.", "fix": "|a| luôn ≥ 0, nên |−3| = 3."}, {"wrong": "Phương trình |x| = -5 có nghiệm x = -5 chứ, vì -5 là số âm mà.", "why": "Không xét điều kiện m < 0.", "fix": "|x| ≥ 0 nên |x| = −5 vô nghiệm."}, {"wrong": "|2x − 3| = 7 chỉ cần giải 2x − 3 = 7 là đủ, được x = 5.", "why": "Bỏ sót trường hợp A = −m.", "fix": "Phải xét cả 2x − 3 = 7 và 2x − 3 = −7, nên x = 5 hoặc x = −2."}]'::jsonb WHERE lesson_id = 'lesson-7-2-2';
UPDATE lesson SET misconceptions = '[{"wrong": "Hai góc kề bù thì luôn bằng nhau, mỗi góc 90° đúng không?", "why": "Nhầm \"kề bù\" với \"bằng nhau\".", "fix": "Hai góc kề bù có TỔNG bằng 180°, không nhất thiết bằng nhau, ví dụ 120° và 60°."}, {"wrong": "Hai góc bằng nhau thì chắc chắn là hai góc đối đỉnh.", "why": "Đảo ngược tính chất.", "fix": "Hai góc đối đỉnh thì bằng nhau, nhưng hai góc bằng nhau chưa chắc đối đỉnh."}, {"wrong": "Góc xOy = 40° thì góc kề bù với nó cũng bằng 40°.", "why": "Nhầm kề bù với đối đỉnh.", "fix": "Góc kề bù với góc 40° bằng 180° − 40° = 140°; góc đối đỉnh mới bằng 40°."}]'::jsonb WHERE lesson_id = 'lesson-7-3-1';
UPDATE lesson SET misconceptions = '[{"wrong": "Hai góc so le trong bằng nhau nhưng chưa đủ để kết luận a // b.", "why": "Không nhớ dấu hiệu nhận biết.", "fix": "Chỉ cần một cặp góc so le trong bằng nhau là a // b."}, {"wrong": "Qua một điểm ở ngoài đường thẳng có thể vẽ được hai đường thẳng song song với nó.", "why": "Không nhớ tiên đề Euclid.", "fix": "Theo tiên đề Euclid, chỉ có đúng một đường thẳng song song."}]'::jsonb WHERE lesson_id = 'lesson-7-3-2';
UPDATE lesson SET misconceptions = '[{"wrong": "Tam giác có hai góc 100° và 90° cũng được mà.", "why": "Quên tổng ba góc bằng 180°.", "fix": "100° + 90° đã vượt 180°, nên không tồn tại tam giác như vậy."}, {"wrong": "Trong tam giác vuông, hai góc nhọn bù nhau (tổng 180°).", "why": "Nhầm phụ nhau với bù nhau.", "fix": "Hai góc nhọn của tam giác vuông phụ nhau: tổng bằng 90°."}]'::jsonb WHERE lesson_id = 'lesson-7-4-1';
UPDATE lesson SET misconceptions = '[{"wrong": "x + y là một đơn thức.", "why": "Nhầm đơn thức với đa thức.", "fix": "Đơn thức chỉ gồm một số, một biến hoặc tích các số và biến; x + y là tổng nên là đa thức."}, {"wrong": "Đơn thức 3x²y có hệ số là 2.", "why": "Nhầm số mũ với hệ số.", "fix": "Hệ số của 3x²y là 3; 2 là số mũ của x."}]'::jsonb WHERE lesson_id = 'lesson-7-5-1';
UPDATE lesson SET misconceptions = '[{"wrong": "Tập hợp {1; 2; 2; 3} có 4 phần tử.", "why": "Đếm phần tử lặp lại.", "fix": "Mỗi phần tử chỉ viết một lần; tập hợp đó là {1; 2; 3} có 3 phần tử."}, {"wrong": "3 ∈ {1; 2} vì 3 lớn hơn 2.", "why": "Hiểu sai kí hiệu thuộc.", "fix": "3 không có trong tập hợp nên 3 ∉ {1; 2}."}]'::jsonb WHERE lesson_id = 'lesson-6-1-1';
UPDATE lesson SET misconceptions = '[{"wrong": "1/2 và 2/4 khác nhau vì tử và mẫu khác nhau.", "why": "Không dùng quy tắc a·d = b·c.", "fix": "1·4 = 2·2 nên 1/2 = 2/4."}, {"wrong": "Cộng cùng một số vào tử và mẫu thì được phân số bằng phân số đã cho.", "why": "Nhầm cộng với nhân.", "fix": "Chỉ nhân (hoặc chia) cả tử và mẫu với cùng một số khác 0 mới được phân số bằng nó. 1/2 ≠ (1+1)/(2+1) = 2/3."}]'::jsonb WHERE lesson_id = 'lesson-6-3-1';
UPDATE lesson SET misconceptions = '[{"wrong": "(a + b)² = a² + b² thôi, đâu cần thêm 2ab đâu nhỉ?", "why": "Quên hạng tử 2ab.", "fix": "(a + b)² = a² + 2ab + b²; ví dụ (1 + 2)² = 9 còn 1² + 2² = 5."}, {"wrong": "(a − b)² = a² − b².", "why": "Nhầm với hiệu hai bình phương.", "fix": "(a − b)² = a² − 2ab + b²; còn a² − b² = (a − b)(a + b)."}]'::jsonb WHERE lesson_id = 'lesson-8-2-1';
UPDATE lesson SET misconceptions = '[{"wrong": "Phương trình 2x + 6 = 0 có nghiệm x = 3.", "why": "Chuyển vế không đổi dấu.", "fix": "2x = −6 ⇒ x = −3."}, {"wrong": "0x + 5 = 0 là phương trình bậc nhất một ẩn.", "why": "Quên điều kiện a ≠ 0.", "fix": "Phương trình bậc nhất một ẩn cần a ≠ 0."}]'::jsonb WHERE lesson_id = 'lesson-8-4-1';

INSERT INTO lesson_progress (student_id, lesson_id, status, progress_percent, last_accessed_at, completed_at) VALUES
 ('stu-1', 'lesson-7-1-1', 'COMPLETED',   100, now() - interval '6 days', now() - interval '6 days'),
 ('stu-1', 'lesson-7-1-2', 'COMPLETED',   100, now() - interval '5 days', now() - interval '5 days'),
 ('stu-1', 'lesson-7-2-1', 'COMPLETED',   100, now() - interval '2 days', now() - interval '2 days'),
 ('stu-1', 'lesson-7-2-2', 'IN_PROGRESS',  60, now() - interval '1 hour', NULL),
 ('stu-9', 'lesson-9-1-1', 'COMPLETED',   100, now() - interval '2 days', now() - interval '2 days'),
 ('stu-9', 'lesson-9-1-2', 'COMPLETED',   100, now() - interval '1 day',  now() - interval '1 day');

INSERT INTO stage_result (student_id, lesson_id, stars, best_correct, total_questions, attempts, first_cleared_at) VALUES
 ('stu-9', 'lesson-9-1-1', 3, 3, 3, 1, now() - interval '2 days'),
 ('stu-9', 'lesson-9-1-2', 2, 2, 3, 2, now() - interval '1 day');

-- ---------------------------------------------------------------------
-- Module C: câu hỏi, quiz, điểm yếu
-- ---------------------------------------------------------------------
INSERT INTO question (question_id, topic_id, content, question_type, options, correct_answer, explanation, difficulty, source, created_by, review_status, reviewed_by, reviewed_at) VALUES
 ('q-7-2-1', 'topic-7-2', 'Tìm x biết |2x - 3| = 7', 'SINGLE_CHOICE',
  '["A. x = 5", "B. x = 5 hoặc x = -2", "C. x = 5 hoặc x = 2", "D. x = -2"]', 'B',
  '2x - 3 = 7 ⇒ x = 5; 2x - 3 = -7 ⇒ x = -2.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('q-7-2-2', 'topic-7-2', 'Giá trị của |-4,5| là', 'SINGLE_CHOICE',
  '["A. -4,5", "B. 4,5", "C. 0", "D. 45"]', 'B',
  'Giá trị tuyệt đối luôn không âm: |-4,5| = 4,5.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('q-7-2-3', 'topic-7-2', 'Phương trình |x + 1| = -3 có bao nhiêu nghiệm?', 'SINGLE_CHOICE',
  '["A. 0 nghiệm", "B. 1 nghiệm", "C. 2 nghiệm", "D. Vô số nghiệm"]', 'A',
  'Giá trị tuyệt đối không thể âm nên phương trình vô nghiệm.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('q-7-2-4', 'topic-7-2', 'Căn bậc hai số học của 49 là', 'SINGLE_CHOICE',
  '["A. -7", "B. 7 và -7", "C. 7", "D. 24,5"]', 'C',
  'Căn bậc hai số học là số không âm: √49 = 7.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('q-7-1-1', 'topic-7-1', 'Kết quả của 1/2 - 2/3 là', 'SINGLE_CHOICE',
  '["A. -1/6", "B. 1/6", "C. -1/5", "D. 1"]', 'A',
  '1/2 - 2/3 = 3/6 - 4/6 = -1/6.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('q-7-1-2', 'topic-7-1', 'Số nào sau đây không phải số hữu tỉ?', 'SINGLE_CHOICE',
  '["A. -3", "B. 0,25", "C. √2", "D. 5/7"]', 'C',
  '√2 là số vô tỉ.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('q-7-3-1', 'topic-7-3', 'Hai góc kề bù có tổng số đo bằng', 'SINGLE_CHOICE',
  '["A. 90°", "B. 180°", "C. 360°", "D. 120°"]', 'B',
  'Hai góc kề bù có tổng bằng 180°.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 ('q-7-3-2', 'topic-7-3', 'Cho góc xOy = 40°. Góc đối đỉnh với góc xOy có số đo là', 'SINGLE_CHOICE',
  '["A. 140°", "B. 40°", "C. 50°", "D. 80°"]', 'B',
  'Hai góc đối đỉnh thì bằng nhau.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '10 days'),
 -- Câu chờ giảng viên duyệt (AI sinh)
 ('q-7-4-1', 'topic-7-4', 'Tam giác ABC có góc A = 50°, góc B = 70°. Số đo góc C là', 'SINGLE_CHOICE',
  '["A. 60°", "B. 70°", "C. 50°", "D. 120°"]', 'A',
  'C = 180° - 50° - 70° = 60°.', 'EASY', 'AI', NULL, 'PENDING_REVIEW', NULL, NULL);

-- Câu thử thách của các ải Lớp 9 - Học kỳ 1 (3 câu / ải, ải trùm lấy ngẫu nhiên 5 câu trong chương)
INSERT INTO question (question_id, topic_id, lesson_id, content, question_type, options, correct_answer, explanation, difficulty, source, created_by, review_status, reviewed_by, reviewed_at) VALUES
 ('q-9-1-1-1', 'topic-9-1', 'lesson-9-1-1', 'Phương trình nào sau đây là phương trình bậc nhất hai ẩn?', 'SINGLE_CHOICE',
  '["A. x² + y = 1", "B. 2x − 3y = 5", "C. 0x + 0y = 2", "D. xy = 4"]', 'B', '2x − 3y = 5 có dạng ax + by = c với a = 2, b = −3 không đồng thời bằng 0.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-1-1-2', 'topic-9-1', 'lesson-9-1-1', 'Cặp số nào là nghiệm của phương trình 2x + y = 5?', 'SINGLE_CHOICE',
  '["A. (1; 2)", "B. (2; 1)", "C. (0; 4)", "D. (3; 0)"]', 'B', 'Thay (2; 1): 2·2 + 1 = 5. Các cặp còn lại cho 4, 4, 6.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-1-1-3', 'topic-9-1', 'lesson-9-1-1', 'Cặp số (2; 1) là nghiệm của hệ phương trình nào?', 'SINGLE_CHOICE',
  '["A. x + y = 3 và x − y = 1", "B. x + y = 3 và x − y = 3", "C. x + 2y = 3 và x − y = 1", "D. 2x + y = 4 và x − y = 1"]', 'A', '(2; 1) thoả mãn cả hai: 2 + 1 = 3 và 2 − 1 = 1.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-1-2-1', 'topic-9-1', 'lesson-9-1-2', 'Nghiệm của hệ x + y = 5 và x − y = 1 là', 'SINGLE_CHOICE',
  '["A. (3; 2)", "B. (2; 3)", "C. (4; 1)", "D. (1; 4)"]', 'A', 'Cộng hai phương trình: 2x = 6 ⇒ x = 3, y = 2.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-1-2-2', 'topic-9-1', 'lesson-9-1-2', 'Giải hệ 2x + y = 7 và x − y = 2 bằng phương pháp cộng, ta được', 'SINGLE_CHOICE',
  '["A. x = 3", "B. x = 5", "C. x = 9", "D. x = 2,5"]', 'A', 'Cộng vế với vế: 3x = 9 ⇒ x = 3, khi đó y = 1.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-1-2-3', 'topic-9-1', 'lesson-9-1-2', 'Hệ x + y = 2 và x + y = 5 có bao nhiêu nghiệm?', 'SINGLE_CHOICE',
  '["A. Một nghiệm", "B. Hai nghiệm", "C. Vô nghiệm", "D. Vô số nghiệm"]', 'C', 'Không có cặp số nào để x + y vừa bằng 2 vừa bằng 5, nên hệ vô nghiệm.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-1-3-1', 'topic-9-1', 'lesson-9-1-3', 'Tổng hai số là 20, hiệu của chúng là 4. Hai số đó là', 'SINGLE_CHOICE',
  '["A. 12 và 8", "B. 14 và 6", "C. 10 và 10", "D. 16 và 4"]', 'A', 'x + y = 20, x − y = 4 ⇒ 2x = 24 ⇒ x = 12, y = 8.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-1-3-2', 'topic-9-1', 'lesson-9-1-3', 'Lớp 9A có 40 học sinh, số nữ nhiều hơn số nam 4 bạn. Số học sinh nam là', 'SINGLE_CHOICE',
  '["A. 18", "B. 22", "C. 20", "D. 16"]', 'A', 'Gọi nam x, nữ y: x + y = 40, y − x = 4 ⇒ x = 18, y = 22.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-1-3-3', 'topic-9-1', 'lesson-9-1-3', 'Bước cuối cùng khi giải bài toán bằng cách lập hệ phương trình là', 'SINGLE_CHOICE',
  '["A. Chọn ẩn", "B. Lập hệ phương trình", "C. Giải hệ phương trình", "D. Đối chiếu điều kiện và kết luận"]', 'D', 'Sau khi giải hệ phải kiểm tra điều kiện của ẩn rồi mới trả lời.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-1-1', 'topic-9-2', 'lesson-9-2-1', 'Nghiệm của phương trình (x − 3)(2x + 4) = 0 là', 'SINGLE_CHOICE',
  '["A. x = 3", "B. x = −2", "C. x = 3 hoặc x = −2", "D. x = −3 hoặc x = 2"]', 'C', 'x − 3 = 0 ⇒ x = 3; 2x + 4 = 0 ⇒ x = −2.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-1-2', 'topic-9-2', 'lesson-9-2-1', 'Điều kiện xác định của phương trình 1/(x − 2) + 3 = x/(x + 1) là', 'SINGLE_CHOICE',
  '["A. x ≠ 2", "B. x ≠ −1", "C. x ≠ 2 và x ≠ −1", "D. Mọi số thực x"]', 'C', 'Các mẫu x − 2 và x + 1 phải khác 0.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-1-3', 'topic-9-2', 'lesson-9-2-1', 'Phương trình (x + 1)/(x − 1) = 2/(x − 1) có nghiệm là', 'SINGLE_CHOICE',
  '["A. x = 1", "B. x = −1", "C. Vô nghiệm", "D. x = 3"]', 'C', 'Khử mẫu được x = 1 nhưng ĐKXĐ là x ≠ 1 nên loại; phương trình vô nghiệm.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-2-1', 'topic-9-2', 'lesson-9-2-2', 'Cho a > b. Khẳng định nào đúng?', 'SINGLE_CHOICE',
  '["A. a + 3 > b + 3", "B. −a > −b", "C. 2a < 2b", "D. a − 1 < b − 1"]', 'A', 'Cộng cùng một số vào hai vế thì giữ nguyên chiều.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-2-2', 'topic-9-2', 'lesson-9-2-2', 'Cho a < b. So sánh −3a và −3b.', 'SINGLE_CHOICE',
  '["A. −3a < −3b", "B. −3a > −3b", "C. −3a = −3b", "D. Không so sánh được"]', 'B', 'Nhân hai vế của a < b với −3 (số âm) phải đổi chiều.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-2-3', 'topic-9-2', 'lesson-9-2-2', 'Biết 3 < 5. Khẳng định nào đúng?', 'SINGLE_CHOICE',
  '["A. 3·(−2) < 5·(−2)", "B. 3·(−2) > 5·(−2)", "C. 3 + (−2) > 5 + (−2)", "D. 3 − 7 > 5 − 7"]', 'B', '3·(−2) = −6 > −10 = 5·(−2): nhân với số âm thì đổi chiều.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-3-1', 'topic-9-2', 'lesson-9-2-3', 'Nghiệm của bất phương trình 2x − 4 > 0 là', 'SINGLE_CHOICE',
  '["A. x > 2", "B. x < 2", "C. x > −2", "D. x < −2"]', 'A', '2x > 4 ⇒ x > 2 (chia cho số dương giữ chiều).', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-3-2', 'topic-9-2', 'lesson-9-2-3', 'Nghiệm của bất phương trình −3x + 9 ≥ 0 là', 'SINGLE_CHOICE',
  '["A. x ≥ 3", "B. x ≤ 3", "C. x ≤ −3", "D. x ≥ −3"]', 'B', '−3x ≥ −9 ⇒ x ≤ 3 (chia cho −3 nên đổi chiều).', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-2-3-3', 'topic-9-2', 'lesson-9-2-3', 'Bất phương trình nào là bất phương trình bậc nhất một ẩn?', 'SINGLE_CHOICE',
  '["A. 0x + 3 > 0", "B. x² − 1 < 0", "C. 5x − 2 ≤ 0", "D. 1/x + 1 > 0"]', 'C', '5x − 2 ≤ 0 có dạng ax + b ≤ 0 với a = 5 ≠ 0.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-1-1', 'topic-9-3', 'lesson-9-3-1', 'Các căn bậc hai của 16 là', 'SINGLE_CHOICE',
  '["A. 4", "B. −4", "C. 4 và −4", "D. 8"]', 'C', '16 > 0 nên có hai căn bậc hai là 4 và −4 (còn √16 = 4).', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-1-2', 'topic-9-3', 'lesson-9-3-1', '√((−7)²) bằng', 'SINGLE_CHOICE',
  '["A. −7", "B. 7", "C. 49", "D. ±7"]', 'B', '√(A²) = |A| nên √((−7)²) = 7.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-1-3', 'topic-9-3', 'lesson-9-3-1', '√(2x − 6) xác định khi', 'SINGLE_CHOICE',
  '["A. x ≥ 3", "B. x > 3", "C. x ≤ 3", "D. x ≥ −3"]', 'A', '2x − 6 ≥ 0 ⇔ x ≥ 3.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-2-1', 'topic-9-3', 'lesson-9-3-2', '√12 · √3 bằng', 'SINGLE_CHOICE',
  '["A. 6", "B. 36", "C. √15", "D. 18"]', 'A', '√12 · √3 = √36 = 6.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-2-2', 'topic-9-3', 'lesson-9-3-2', '√(49/64) bằng', 'SINGLE_CHOICE',
  '["A. 7/8", "B. 49/8", "C. 7/64", "D. 8/7"]', 'A', '√(49/64) = √49 / √64 = 7/8.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-2-3', 'topic-9-3', 'lesson-9-3-2', 'Khẳng định nào đúng?', 'SINGLE_CHOICE',
  '["A. √(16 + 9) = 7", "B. √(16·9) = 12", "C. √(16 − 9) = 1", "D. √16 + √9 = 25"]', 'B', '√(16·9) = 4·3 = 12. Còn √(16 + 9) = 5, √(16 − 9) = √7, √16 + √9 = 7.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-3-1', 'topic-9-3', 'lesson-9-3-3', 'Đưa thừa số ra ngoài dấu căn: √50 bằng', 'SINGLE_CHOICE',
  '["A. 5√2", "B. 2√5", "C. 25√2", "D. 10√5"]', 'A', '√50 = √(25·2) = 5√2.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-3-2', 'topic-9-3', 'lesson-9-3-3', 'Trục căn thức ở mẫu của 6/√3 được', 'SINGLE_CHOICE',
  '["A. 2√3", "B. 6√3", "C. √3/6", "D. 3√2"]', 'A', '6/√3 = 6√3/3 = 2√3.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-3-3', 'topic-9-3', 'lesson-9-3-3', 'Rút gọn √8 + √18 được', 'SINGLE_CHOICE',
  '["A. √26", "B. 5√2", "C. 6√2", "D. 13"]', 'B', '√8 = 2√2, √18 = 3√2, tổng 5√2.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-4-1', 'topic-9-3', 'lesson-9-3-4', '∛(−64) bằng', 'SINGLE_CHOICE',
  '["A. 4", "B. −4", "C. ±4", "D. Không xác định"]', 'B', '(−4)³ = −64 nên ∛(−64) = −4.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-4-2', 'topic-9-3', 'lesson-9-3-4', '∛125 + ∛(−8) bằng', 'SINGLE_CHOICE',
  '["A. 3", "B. 7", "C. −3", "D. 117"]', 'A', '∛125 = 5, ∛(−8) = −2, tổng là 3.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-3-4-3', 'topic-9-3', 'lesson-9-3-4', '∛(x − 1) xác định khi', 'SINGLE_CHOICE',
  '["A. x ≥ 1", "B. x > 1", "C. Mọi số thực x", "D. x ≠ 1"]', 'C', 'Căn thức bậc ba xác định với mọi giá trị của biểu thức dưới căn.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-4-1-1', 'topic-9-4', 'lesson-9-4-1', 'Tam giác ABC vuông tại A có AB = 3, AC = 4, BC = 5. Khi đó sin B bằng', 'SINGLE_CHOICE',
  '["A. 3/5", "B. 4/5", "C. 3/4", "D. 4/3"]', 'B', 'Cạnh đối của góc B là AC = 4, cạnh huyền BC = 5 ⇒ sin B = 4/5.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-4-1-2', 'topic-9-4', 'lesson-9-4-1', 'sin 30° bằng', 'SINGLE_CHOICE',
  '["A. 1/2", "B. √3/2", "C. √2/2", "D. 1"]', 'A', 'Giá trị đặc biệt: sin 30° = 1/2.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-4-1-3', 'topic-9-4', 'lesson-9-4-1', 'Biết sin 40° ≈ 0,643. Khi đó cos 50° xấp xỉ', 'SINGLE_CHOICE',
  '["A. 0,643", "B. 0,766", "C. 0,357", "D. 1,643"]', 'A', '40° và 50° phụ nhau nên cos 50° = sin 40°.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-4-2-1', 'topic-9-4', 'lesson-9-4-2', 'Tam giác ABC vuông tại A có BC = 8 và góc B = 30°. Độ dài AC là', 'SINGLE_CHOICE',
  '["A. 4", "B. 4√3", "C. 8√3", "D. 16"]', 'A', 'AC = BC·sin B = 8·1/2 = 4.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-4-2-2', 'topic-9-4', 'lesson-9-4-2', 'Tam giác ABC vuông tại A có AB = 6 và góc C = 45°. Độ dài AC là', 'SINGLE_CHOICE',
  '["A. 6", "B. 6√2", "C. 3", "D. 12"]', 'A', 'Góc C = 45° nên tam giác vuông cân tại A, AC = AB = 6 (hoặc AC = AB·cot C = 6).', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-4-2-3', 'topic-9-4', 'lesson-9-4-2', 'Bóng một cây trên mặt đất dài 12 m, tia nắng tạo với mặt đất góc 45°. Cây cao', 'SINGLE_CHOICE',
  '["A. 12 m", "B. 6 m", "C. 12√2 m", "D. 24 m"]', 'A', 'Chiều cao = 12·tan 45° = 12 m.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-1-1', 'topic-9-5', 'lesson-9-5-1', 'Cho (O; 5 cm) và điểm M với OM = 3 cm. Điểm M nằm', 'SINGLE_CHOICE',
  '["A. Trong đường tròn", "B. Trên đường tròn", "C. Ngoài đường tròn", "D. Không xác định được"]', 'A', 'OM = 3 < 5 = R nên M nằm trong đường tròn.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-1-2', 'topic-9-5', 'lesson-9-5-1', 'Đường tròn có bao nhiêu trục đối xứng?', 'SINGLE_CHOICE',
  '["A. 1", "B. 2", "C. 4", "D. Vô số"]', 'D', 'Mọi đường kính đều là trục đối xứng.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-1-3', 'topic-9-5', 'lesson-9-5-1', 'Dây lớn nhất của đường tròn bán kính 6 cm dài', 'SINGLE_CHOICE',
  '["A. 6 cm", "B. 12 cm", "C. 3 cm", "D. 36 cm"]', 'B', 'Dây lớn nhất là đường kính = 2R = 12 cm.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-2-1', 'topic-9-5', 'lesson-9-5-2', 'Góc ở tâm AOB = 80°. Số đo cung lớn AB là', 'SINGLE_CHOICE',
  '["A. 80°", "B. 280°", "C. 100°", "D. 160°"]', 'B', 'Cung lớn = 360° − 80° = 280°.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-2-2', 'topic-9-5', 'lesson-9-5-2', 'Cho (O; 5 cm) và dây AB = 8 cm. Khoảng cách từ O đến AB là', 'SINGLE_CHOICE',
  '["A. 3 cm", "B. 4 cm", "C. 5 cm", "D. √41 cm"]', 'A', 'Hạ OH ⊥ AB thì HA = 4 cm, OH = √(25 − 16) = 3 cm.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-2-3', 'topic-9-5', 'lesson-9-5-2', 'Số đo của nửa đường tròn là', 'SINGLE_CHOICE',
  '["A. 90°", "B. 180°", "C. 360°", "D. 120°"]', 'B', 'Nửa đường tròn có số đo 180°.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-3-1', 'topic-9-5', 'lesson-9-5-3', 'Độ dài cung 60° của đường tròn bán kính 6 cm là', 'SINGLE_CHOICE',
  '["A. 2π cm", "B. π cm", "C. 6π cm", "D. 12π cm"]', 'A', 'l = π·6·60/180 = 2π cm.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-3-2', 'topic-9-5', 'lesson-9-5-3', 'Diện tích hình quạt tròn 90° của hình tròn bán kính 4 cm là', 'SINGLE_CHOICE',
  '["A. 4π cm²", "B. 2π cm²", "C. 8π cm²", "D. 16π cm²"]', 'A', 'S = π·16·90/360 = 4π cm².', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-3-3', 'topic-9-5', 'lesson-9-5-3', 'Diện tích hình vành khuyên giữa hai đường tròn đồng tâm bán kính 5 cm và 3 cm là', 'SINGLE_CHOICE',
  '["A. 4π cm²", "B. 16π cm²", "C. 34π cm²", "D. 2π cm²"]', 'B', 'S = π(5² − 3²) = 16π cm².', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-4-1', 'topic-9-5', 'lesson-9-5-4', 'Cho (O; 5 cm), khoảng cách từ O đến đường thẳng a là 3 cm. Đường thẳng a và đường tròn', 'SINGLE_CHOICE',
  '["A. Cắt nhau", "B. Tiếp xúc", "C. Không giao nhau", "D. Trùng nhau"]', 'A', 'd = 3 < 5 = R nên cắt nhau tại hai điểm.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-4-2', 'topic-9-5', 'lesson-9-5-4', 'Đường thẳng và (O; R) có đúng một điểm chung khi', 'SINGLE_CHOICE',
  '["A. d < R", "B. d = R", "C. d > R", "D. d = 2R"]', 'B', 'Có đúng 1 điểm chung là trường hợp tiếp xúc: d = R.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-4-3', 'topic-9-5', 'lesson-9-5-4', 'MA, MB là hai tiếp tuyến của (O) cắt nhau tại M. Biết MA = 7 cm thì MB bằng', 'SINGLE_CHOICE',
  '["A. 7 cm", "B. 14 cm", "C. 3,5 cm", "D. Chưa đủ dữ kiện"]', 'A', 'Hai tiếp tuyến cắt nhau thì MA = MB.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-5-1', 'topic-9-5', 'lesson-9-5-5', 'Cho R = 6 cm, r = 2 cm, d = 4 cm. Hai đường tròn', 'SINGLE_CHOICE',
  '["A. Cắt nhau", "B. Tiếp xúc ngoài", "C. Tiếp xúc trong", "D. Ở ngoài nhau"]', 'C', 'd = 4 = R − r nên tiếp xúc trong.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-5-2', 'topic-9-5', 'lesson-9-5-5', 'Cho R = 5 cm, r = 3 cm, d = 6 cm. Hai đường tròn', 'SINGLE_CHOICE',
  '["A. Cắt nhau", "B. Tiếp xúc ngoài", "C. Tiếp xúc trong", "D. Đựng nhau"]', 'A', 'R − r = 2 < 6 < 8 = R + r nên cắt nhau.', 'MEDIUM', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days'),
 ('q-9-5-5-3', 'topic-9-5', 'lesson-9-5-5', 'Hai đường tròn ở ngoài nhau khi', 'SINGLE_CHOICE',
  '["A. d > R + r", "B. d = R + r", "C. d < R − r", "D. d = 0"]', 'A', 'Ở ngoài nhau: d > R + r.', 'EASY', 'MANUAL', 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '7 days');

INSERT INTO weakness (weakness_id, student_id, topic_id, mastery_score, severity, status) VALUES
 ('weak-1', 'stu-1', 'topic-7-2', 45.00, 'HIGH',   'OPEN'),
 ('weak-2', 'stu-1', 'topic-7-3', 68.00, 'MEDIUM', 'IMPROVING');

INSERT INTO quiz (quiz_id, weakness_id, title, quiz_type, time_limit_minutes, generated_by) VALUES
 ('quiz-1', 'weak-1', 'Củng cố: Giá trị tuyệt đối', 'REINFORCEMENT', 10, 'SYSTEM'),
 ('quiz-2', NULL,     'Luyện tập: Số hữu tỉ',      'PRACTICE',      10, 'SYSTEM'),
 ('quiz-3', 'weak-2', 'Củng cố: Góc và đường thẳng song song', 'REINFORCEMENT', 5, 'SYSTEM');

INSERT INTO quiz_question (quiz_id, question_id, order_index, points) VALUES
 ('quiz-1', 'q-7-2-1', 1, 1), ('quiz-1', 'q-7-2-2', 2, 1), ('quiz-1', 'q-7-2-3', 3, 1), ('quiz-1', 'q-7-2-4', 4, 1),
 ('quiz-2', 'q-7-1-1', 1, 1), ('quiz-2', 'q-7-1-2', 2, 1),
 ('quiz-3', 'q-7-3-1', 1, 1), ('quiz-3', 'q-7-3-2', 2, 1);

-- ---------------------------------------------------------------------
-- Module D: tác tử AI
-- ---------------------------------------------------------------------
-- voice_*: giọng Google Text-to-Speech tiếng Việt (A, C: nữ; B, D: nam). Pitch tính theo semitone, rate 0.25 - 4.0
INSERT INTO ai_agent (agent_id, name, agent_type, persona_prompt, model_name, voice_name, voice_pitch, voice_rate, is_active) VALUES
 ('agent-tutor', 'Thầy Minh Trí', 'TUTOR',
  'Bạn là Thầy Minh Trí, giáo viên Toán THCS thân thiện. Giảng giải ngắn gọn, từng bước, CHỈ dựa trên nội dung bài giảng đã được duyệt. Không giải hộ ngay, hãy gợi mở để học sinh tự làm. Mỗi lượt kết thúc bằng một câu hỏi và 2-4 lựa chọn trả lời ngắn để học sinh bấm chọn.',
  'gemini-3.1-flash-lite', 'vi-VN-Wavenet-D', -2.0, 0.95, TRUE),
 ('agent-critic', 'Tuấn Kiệt', 'CRITIC',
  'Bạn là Tuấn Kiệt, bạn học hay phản biện. Thỉnh thoảng cố ý đưa ra MỘT lập luận sai để học sinh phát hiện. Lỗi sai BẮT BUỘC thuộc đúng kiến thức của bài đang học (ưu tiên chọn trong danh sách misconceptions của bài), tuyệt đối không nói sang chủ đề khác. Trả về JSON có cờ has_intended_error và error_note giải thích chỗ sai.',
  'gemini-3.1-flash-lite', 'vi-VN-Wavenet-B', 3.5, 1.10, TRUE),
 ('agent-creative', 'Bảo An', 'CRITIC',
  'Bạn là Bảo An, bạn học nhanh nhạy, hay tìm cách giải khác và soát lỗi lập luận của các bạn. Chỉ bàn về kiến thức của bài đang học.',
  'gemini-3.1-flash-lite', 'vi-VN-Wavenet-C', 2.0, 1.05, TRUE);

-- ---------------------------------------------------------------------
-- Module E: thành tích
-- ---------------------------------------------------------------------
-- metric + target: tự mở khi chỉ số đạt mốc (xem AchievementService.Metric). metric NULL = trao thủ công trong code.
-- Mở huy hiệu được thưởng số xu = points. icon_url = tên icon Material Symbols. tier: 1 đồng, 2 bạc, 3 vàng, 4 kim cương
INSERT INTO achievement (achievement_id, code, name, description, icon_url, criteria, points, category, metric, target, tier, sort_order) VALUES
 ('ach-1',  'FIRST_LESSON', 'Bước chân đầu tiên', 'Qua ải đầu tiên trên bản đồ',              'directions_walk',       'Qua 1 ải',                    10, 'LEARN',   'STAGES',      1,  1, 1),
 ('ach-6',  'STAGES_5',     'Nhà thám hiểm',      'Qua 5 ải trên bản đồ',                      'explore',               'Qua 5 ải',                    20, 'LEARN',   'STAGES',      5,  2, 2),
 ('ach-7',  'STAGES_12',    'Lữ khách bền bỉ',    'Qua 12 ải trên bản đồ',                     'hiking',                'Qua 12 ải',                   40, 'LEARN',   'STAGES',      12, 3, 3),
 ('ach-8',  'STARS_10',     'Nhặt sao',           'Gom đủ 10 ngôi sao',                        'star',                  'Đạt 10 sao',                  15, 'LEARN',   'STARS',       10, 1, 4),
 ('ach-9',  'STARS_30',     'Chòm sao nhỏ',       'Gom đủ 30 ngôi sao',                        'auto_awesome',          'Đạt 30 sao',                  30, 'LEARN',   'STARS',       30, 2, 5),
 ('ach-10', 'STARS_60',     'Dải ngân hà',        'Gom đủ 60 ngôi sao',                        'stars',                 'Đạt 60 sao',                  60, 'LEARN',   'STARS',       60, 4, 6),
 ('ach-11', 'PERFECT_1',    'Hoàn hảo',           'Đạt 3 sao ở một ải',                        'workspace_premium',     '1 ải 3 sao',                  10, 'LEARN',   'PERFECT',     1,  1, 7),
 ('ach-12', 'PERFECT_5',    'Không tì vết',       'Đạt 3 sao ở 5 ải',                          'verified',              '5 ải 3 sao',                  30, 'LEARN',   'PERFECT',     5,  2, 8),
 ('ach-13', 'PERFECT_12',   'Bậc thầy chính xác', 'Đạt 3 sao ở 12 ải',                         'diamond',               '12 ải 3 sao',                 60, 'LEARN',   'PERFECT',     12, 4, 9),
 ('ach-14', 'FIRST_BOSS',   'Hạ gục trùm',        'Thắng ải trùm đầu tiên',                    'swords',                'Thắng 1 ải trùm',             25, 'LEARN',   'BOSSES',      1,  2, 10),
 ('ach-15', 'BOSS_3',       'Thợ săn trùm',       'Thắng 3 ải trùm',                           'shield',                'Thắng 3 ải trùm',             40, 'LEARN',   'BOSSES',      3,  3, 11),
 ('ach-16', 'ALL_BOSS',     'Bá chủ học kỳ I',    'Thắng cả 5 ải trùm của học kỳ',             'castle',                'Thắng 5 ải trùm',             100,'LEARN',   'BOSSES',      5,  4, 12),
 ('ach-17', 'STREAK_3',     'Khởi động',          'Học 3 ngày liên tiếp',                      'local_fire_department', 'Chuỗi 3 ngày',                15, 'STREAK',  'STREAK',      3,  1, 20),
 ('ach-5',  'STREAK_7',     'Chuỗi 7 ngày',       'Học liên tục 7 ngày',                       'local_fire_department', 'Chuỗi 7 ngày',                40, 'STREAK',  'STREAK',      7,  2, 21),
 ('ach-18', 'STREAK_14',    'Ngọn lửa bền bỉ',    'Học liên tục 14 ngày',                      'whatshot',              'Chuỗi 14 ngày',               60, 'STREAK',  'STREAK',      14, 3, 22),
 ('ach-19', 'STREAK_30',    'Lửa không tắt',      'Học liên tục 30 ngày',                      'mode_heat',             'Chuỗi 30 ngày',               120,'STREAK',  'STREAK',      30, 4, 23),
 ('ach-20', 'ARENA_FIRST',  'Võ sĩ tập sự',       'Chơi Đấu trường lần đầu',                   'sports_esports',        'Chơi 1 lượt Đấu trường',      10, 'GAME',    'ARENA_PLAYS', 1,  1, 30),
 ('ach-21', 'ARENA_10',     'Chiến binh',         'Chơi 10 lượt Đấu trường',                   'sports_martial_arts',   'Chơi 10 lượt Đấu trường',     30, 'GAME',    'ARENA_PLAYS', 10, 2, 31),
 ('ach-22', 'COMBO_5',      'Combo x5',           'Trả lời đúng 5 câu liên tiếp ở Đấu trường', 'bolt',                  'Combo 5',                     20, 'GAME',    'ARENA_COMBO', 5,  2, 32),
 ('ach-23', 'COMBO_10',     'Combo hoàn hảo',     'Đúng liền 10 câu ở Đấu trường',             'electric_bolt',         'Combo 10',                    50, 'GAME',    'ARENA_COMBO', 10, 4, 33),
 ('ach-24', 'PET_3',        'Bạn thú nhỏ',        'Sở hữu 3 thú đồng hành',                    'pets',                  'Có 3 thú',                    20, 'COLLECT', 'PETS',        3,  2, 40),
 ('ach-25', 'PET_6',        'Vườn thú mini',      'Sở hữu 6 thú đồng hành',                    'cruelty_free',          'Có 6 thú',                    50, 'COLLECT', 'PETS',        6,  3, 41),
 ('ach-26', 'LEVEL_5',      'Lên cấp 5',          'Đạt cấp 5',                                 'trending_up',           'Cấp 5',                       25, 'COLLECT', 'LEVEL',       5,  2, 42),
 ('ach-27', 'LEVEL_10',     'Cao thủ cấp 10',     'Đạt cấp 10',                                'military_tech',         'Cấp 10',                      60, 'COLLECT', 'LEVEL',       10, 4, 43),
 ('ach-28', 'RICH_300',     'Tiểu phú ông',       'Cùng lúc có 300 xu',                        'savings',               'Có 300 xu',                   20, 'COLLECT', 'COINS',       300,2, 44),
 ('ach-2',  'BUG_HUNTER',   'Thám tử bắt lỗi',    'Phát hiện lập luận sai của bạn AI',         'manage_search',         'Bắt được 1 lỗi sai của bạn AI',20, 'SOCIAL',  NULL,          NULL,2, 50),
 ('ach-29', 'DISCUSS_5',    'Nhà tranh luận',     'Tham gia 5 buổi thảo luận cùng 3 AI',       'forum',                 '5 buổi thảo luận',            25, 'SOCIAL',  'DISCUSSIONS', 5,  2, 51),
 ('ach-3',  'QUIZ_MASTER',  'Cao thủ quiz',       'Đạt điểm tuyệt đối một bài quiz',           'emoji_events',          'Đạt 100% một bài quiz',       30, 'SOCIAL',  NULL,          NULL,3, 52),
 ('ach-4',  'PHOTO_SOLVER', 'Nhà giải ảnh',       'Gửi bài toán đầu tiên bằng ảnh',            'photo_camera',          'Gửi 1 ảnh bài toán',          10, 'SOCIAL',  NULL,          NULL,1, 53);

INSERT INTO student_achievement (student_id, achievement_id, earned_at) VALUES
 ('stu-1', 'ach-1', now() - interval '6 days');

-- ---------------------------------------------------------------------
-- Bài viết trang chủ (banner cho khách)
-- ---------------------------------------------------------------------
INSERT INTO blog_post (post_id, title, summary, content, theme, tag, is_published, is_pinned, author_id, author_name, created_at) VALUES
 ('post-1', 'Ra mắt bản đồ ải Toán 9 - Học kỳ I',
  '17 ải, 5 ải trùm, 10 thú đồng hành. Học như chơi game, bám sát SGK Kết nối tri thức.',
  E'Bản đồ ải Toán 9 học kỳ I đã mở!\n\n• 5 vùng đất tương ứng 5 chương: hệ phương trình, bất phương trình, căn thức, hệ thức lượng, đường tròn.\n• Mỗi ải: học lý thuyết 5 phút, thảo luận cùng 3 AI, rồi vượt 3 câu thử thách.\n• Thắng ải trùm cuối chương để mở rương báu và nhận thú hiếm.\n\nĐăng ký là có ngay 7 ngày dùng thử miễn phí.',
  'BLUE', 'Tính năng mới', TRUE, TRUE, 'u-admin', 'Ban biên tập MAEL', now() - interval '3 days'),
 ('post-2', 'Đấu trường: giải toán siêu tốc, ít chữ, nhiều vui',
  'Mini game mới: bắn bóng đáp án, combo liên tiếp, hạ quái vật bằng kiến thức.',
  E'Đấu trường là mini game giải bài nhanh trong 2 phút.\n\n• Mỗi câu đúng: thú cưng tung chiêu, quái vật mất máu.\n• Đúng liên tiếp để lên combo, nhân điểm.\n• Câu hỏi lấy từ tài liệu do giảng viên tải lên và duyệt.\n\nChơi vừa sức: mỗi lượt tốn 1 thể lực, thể lực tự hồi sau 30 phút.',
  'ORANGE', 'Mini game', TRUE, FALSE, 'u-admin', 'Ban biên tập MAEL', now() - interval '2 days'),
 ('post-3', 'Mẹo giữ chuỗi học mỗi ngày chỉ 10 phút',
  'Học đều mỗi ngày quan trọng hơn học dồn. Dùng khiên giữ chuỗi khi bận.',
  E'Não nhớ lâu hơn khi ôn đều đặn.\n\n• Mỗi ngày qua ít nhất 1 ải hoặc chơi 1 lượt Đấu trường để chuỗi ngày +1.\n• Lỡ một ngày? Khiên giữ chuỗi (60 xu) sẽ tự bảo vệ chuỗi của em.\n• Chuỗi 3, 7, 14, 30 ngày mở huy hiệu và tặng xu.',
  'GREEN', 'Góc học tập', TRUE, FALSE, 'u-lecturer', 'Nguyễn Thị Lan', now() - interval '1 day'),
 ('post-4', 'Gói Trung tâm và Trường học: demo miễn phí',
  'Trung tâm từ 30 học sinh, trường từ 200 học sinh. Đăng ký nhận tư vấn trong 1 phút.',
  E'MAEL hỗ trợ trung tâm và trường học triển khai theo lớp.\n\n• Bảng theo dõi tiến độ từng học sinh.\n• Giáo viên tải tài liệu PDF, AI soạn câu hỏi và mini game, giáo viên duyệt trước khi giao.\n• Hotline: 0342 764 001 • Email: tduyminh0112@gmail.com',
  'PURPLE', 'Hợp tác', TRUE, FALSE, 'u-admin', 'Ban biên tập MAEL', now() - interval '5 hours');

