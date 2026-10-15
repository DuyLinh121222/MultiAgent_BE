-- =====================================================================
--  MAEL - Dữ liệu mẫu (chạy sau 01_schema.sql)
--  psql -U postgres -d mael -f 02_seed.sql
--
--  Tài khoản demo (mật khẩu đều là: 123456)
--    admin@mael.vn       ADMIN
--    giangvien@mael.vn   LECTURER
--    hocsinh@mael.vn     STUDENT (lớp 7, đang dùng thử 7 ngày)
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Module A: tài khoản, gói, đăng ký
-- ---------------------------------------------------------------------
INSERT INTO user_account (user_id, email, password_hash, role, status) VALUES
 ('u-admin',    'admin@mael.vn',     '$2a$10$Ehxad5PZjSPcP/3gmF1H/.iVknCeRtLRtFiz1a6ROZXzpXZUQFoAu', 'ADMIN',    'ACTIVE'),
 ('u-lecturer', 'giangvien@mael.vn', '$2a$10$Ehxad5PZjSPcP/3gmF1H/.iVknCeRtLRtFiz1a6ROZXzpXZUQFoAu', 'LECTURER', 'ACTIVE'),
 ('u-student',  'hocsinh@mael.vn',   '$2a$10$Ehxad5PZjSPcP/3gmF1H/.iVknCeRtLRtFiz1a6ROZXzpXZUQFoAu', 'STUDENT',  'ACTIVE');

INSERT INTO lecturer (lecturer_id, user_id, full_name, specialization, academic_degree, workplace) VALUES
 ('lec-1', 'u-lecturer', 'Nguyễn Thị Lan', 'Đại số, Hình học THCS', 'Thạc sĩ', 'Trường THCS Nguyễn Du');

INSERT INTO student (student_id, user_id, full_name, phone, grade_level, date_of_birth) VALUES
 ('stu-1', 'u-student', 'Minh Quân', '0901234567', 7, '2014-05-20');

INSERT INTO subscription_plan (plan_id, name, description, price, duration_days, max_ai_sessions, is_active) VALUES
 ('plan-trial',   'Dùng thử 7 ngày', 'Trải nghiệm đầy đủ phòng học 3 AI, giải toán bằng ảnh và quiz củng cố trong 7 ngày.', 0,     7,  NULL, TRUE),
 ('plan-monthly', 'Gói tháng',       'Học không giới hạn cùng Thầy AI và 2 bạn AI, giải toán bằng ảnh, quiz theo điểm yếu.', 99000, 30, NULL, TRUE);

INSERT INTO subscription (subscription_id, student_id, plan_id, start_date, end_date, status, auto_renew) VALUES
 ('sub-1', 'stu-1', 'plan-trial', now() - interval '1 day', now() + interval '6 days', 'ACTIVE', FALSE);

INSERT INTO notification (student_id, type, title, content) VALUES
 ('stu-1', 'SYSTEM', 'Chào mừng bạn đến với MAEL!', 'Bạn có 7 ngày dùng thử miễn phí. Vào Phòng học nhóm AI để bắt đầu nhé.'),
 ('stu-1', 'QUIZ',   'Có bài luyện tập mới', 'Thầy AI đã chuẩn bị bài luyện tập Giá trị tuyệt đối cho bạn.');

-- ---------------------------------------------------------------------
-- Module B: lộ trình, chủ đề, bài giảng
-- ---------------------------------------------------------------------
INSERT INTO learning_path (path_id, name, grade_level, description) VALUES
 ('path-6', 'Toán lớp 6', 6, 'Số tự nhiên, số nguyên, phân số và hình học trực quan.'),
 ('path-7', 'Toán lớp 7', 7, 'Số hữu tỉ, số thực, tam giác và biểu thức đại số.'),
 ('path-8', 'Toán lớp 8', 8, 'Đa thức, hằng đẳng thức, phân thức và phương trình bậc nhất.'),
 ('path-9', 'Toán lớp 9', 9, 'Hệ phương trình, căn thức, phương trình bậc hai và đường tròn.');

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
 ('topic-8-4', 'path-8', 'Phương trình bậc nhất một ẩn', 'Giải phương trình và giải bài toán bằng cách lập phương trình.', 4),

 ('topic-9-1', 'path-9', 'Hệ hai phương trình bậc nhất hai ẩn', 'Phương pháp thế, phương pháp cộng đại số.', 1),
 ('topic-9-2', 'path-9', 'Căn bậc hai và căn bậc ba', 'Biến đổi, rút gọn biểu thức chứa căn.', 2),
 ('topic-9-3', 'path-9', 'Phương trình bậc hai một ẩn', 'Công thức nghiệm, định lí Viète.', 3),
 ('topic-9-4', 'path-9', 'Đường tròn', 'Góc nội tiếp, tiếp tuyến, tứ giác nội tiếp.', 4);

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
 ('lesson-8-4-1', 'topic-8-4', 'Phương trình bậc nhất một ẩn', E'Phương trình ax + b = 0 (a ≠ 0) có nghiệm duy nhất x = -b/a.', 20, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '30 days'),
 -- Lớp 9
 ('lesson-9-3-1', 'topic-9-3', 'Công thức nghiệm của phương trình bậc hai', E'Phương trình ax² + bx + c = 0 (a ≠ 0), Δ = b² - 4ac.\n• Δ > 0: hai nghiệm phân biệt x = (-b ± √Δ) / 2a\n• Δ = 0: nghiệm kép x = -b / 2a\n• Δ < 0: vô nghiệm', 25, 1, 'u-admin', 'PUBLISHED', 'lec-1', now() - interval '30 days');

INSERT INTO lesson_progress (student_id, lesson_id, status, progress_percent, last_accessed_at, completed_at) VALUES
 ('stu-1', 'lesson-7-1-1', 'COMPLETED',   100, now() - interval '6 days', now() - interval '6 days'),
 ('stu-1', 'lesson-7-1-2', 'COMPLETED',   100, now() - interval '5 days', now() - interval '5 days'),
 ('stu-1', 'lesson-7-2-1', 'COMPLETED',   100, now() - interval '2 days', now() - interval '2 days'),
 ('stu-1', 'lesson-7-2-2', 'IN_PROGRESS',  60, now() - interval '1 hour', NULL);

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
INSERT INTO ai_agent (agent_id, name, agent_type, persona_prompt, model_name, is_active) VALUES
 ('agent-tutor', 'Thầy Minh Trí', 'TUTOR',
  'Bạn là Thầy Minh Trí, giáo viên Toán THCS thân thiện. Giảng giải ngắn gọn, từng bước, dựa trên nội dung bài giảng đã được duyệt. Không giải hộ ngay, hãy gợi mở để học sinh tự làm.',
  'gpt-6-luna', TRUE),
 ('agent-critic', 'Tuấn Kiệt', 'CRITIC',
  'Bạn là Tuấn Kiệt, bạn học hay phản biện. Thỉnh thoảng cố ý đưa ra một lập luận sai logic phổ biến để học sinh phát hiện. Trả về JSON có cờ has_intended_error.',
  'gpt-6-luna', TRUE),
 ('agent-creative', 'Bảo An', 'CRITIC',
  'Bạn là Bảo An, bạn học nhanh nhạy, hay tìm cách giải khác và soát lỗi lập luận của các bạn.',
  'gpt-6-luna', TRUE);

-- ---------------------------------------------------------------------
-- Module E: thành tích
-- ---------------------------------------------------------------------
INSERT INTO achievement (achievement_id, code, name, description, criteria, points) VALUES
 ('ach-1', 'FIRST_LESSON',   'Bước chân đầu tiên', 'Hoàn thành bài học đầu tiên',            'Hoàn thành 1 bài học',               10),
 ('ach-2', 'BUG_HUNTER',     'Thám tử bắt lỗi',     'Phát hiện lập luận sai của bạn AI',      'Chỉ ra 1 câu phản biện cố ý sai',    20),
 ('ach-3', 'QUIZ_MASTER',    'Cao thủ quiz',        'Đạt điểm tuyệt đối một bài quiz',        'Đạt 100% một bài quiz',              30),
 ('ach-4', 'PHOTO_SOLVER',   'Nhà giải ảnh',        'Gửi bài toán đầu tiên bằng ảnh',         'Gửi 1 ảnh bài toán',                 10),
 ('ach-5', 'STREAK_7',       'Chuỗi 7 ngày',        'Học liên tục 7 ngày',                    'Đăng nhập học 7 ngày liên tiếp',     50);

INSERT INTO student_achievement (student_id, achievement_id, earned_at) VALUES
 ('stu-1', 'ach-1', now() - interval '6 days');

COMMIT;
