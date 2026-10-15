-- =====================================================================
--  MAEL - Multi-Agent Edtech Learning
--  PostgreSQL schema (theo MAEL Logical ERD - 28 bảng, 6 module)
--  Chạy trên database "mael":  psql -U postgres -d mael -f 01_schema.sql
--  Script chạy lại được nhiều lần (DROP ... CASCADE ở đầu).
-- =====================================================================

-- gen_random_uuid() có sẵn từ PostgreSQL 13. Nếu dùng bản cũ hơn thì bật pgcrypto.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

DROP TABLE IF EXISTS content_report       CASCADE;
DROP TABLE IF EXISTS content_review       CASCADE;
DROP TABLE IF EXISTS solution             CASCADE;
DROP TABLE IF EXISTS photo_problem        CASCADE;
DROP TABLE IF EXISTS student_achievement  CASCADE;
DROP TABLE IF EXISTS achievement          CASCADE;
DROP TABLE IF EXISTS message              CASCADE;
DROP TABLE IF EXISTS session_agent        CASCADE;
DROP TABLE IF EXISTS discussion_session   CASCADE;
DROP TABLE IF EXISTS ai_agent             CASCADE;
DROP TABLE IF EXISTS attempt_answer       CASCADE;
DROP TABLE IF EXISTS quiz_attempt         CASCADE;
DROP TABLE IF EXISTS quiz_question        CASCADE;
DROP TABLE IF EXISTS quiz                 CASCADE;
DROP TABLE IF EXISTS weakness             CASCADE;
DROP TABLE IF EXISTS question             CASCADE;
DROP TABLE IF EXISTS lesson_progress      CASCADE;
DROP TABLE IF EXISTS lesson_material      CASCADE;
DROP TABLE IF EXISTS lesson               CASCADE;
DROP TABLE IF EXISTS topic                CASCADE;
DROP TABLE IF EXISTS learning_path        CASCADE;
DROP TABLE IF EXISTS notification         CASCADE;
DROP TABLE IF EXISTS payment              CASCADE;
DROP TABLE IF EXISTS subscription         CASCADE;
DROP TABLE IF EXISTS subscription_plan    CASCADE;
DROP TABLE IF EXISTS student              CASCADE;
DROP TABLE IF EXISTS lecturer             CASCADE;
DROP TABLE IF EXISTS user_account         CASCADE;

-- =====================================================================
--  MODULE A - Tài khoản, Gói & Thanh toán
-- =====================================================================

-- Tài khoản đăng nhập chung cho mọi vai trò (không đặt tên USER vì là từ khóa của PostgreSQL)
CREATE TABLE user_account (
    user_id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('STUDENT', 'LECTURER', 'ADMIN')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOCKED')),
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- Hồ sơ giảng viên chuyên môn (người duyệt tài liệu)
CREATE TABLE lecturer (
    lecturer_id      VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id          VARCHAR(36)  NOT NULL UNIQUE REFERENCES user_account(user_id) ON DELETE CASCADE,
    full_name        VARCHAR(150) NOT NULL,
    specialization   VARCHAR(150),
    academic_degree  VARCHAR(100),
    workplace        VARCHAR(255),
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);

-- Hồ sơ học sinh THCS
CREATE TABLE student (
    student_id     VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id        VARCHAR(36)  NOT NULL UNIQUE REFERENCES user_account(user_id) ON DELETE CASCADE,
    full_name      VARCHAR(150) NOT NULL,
    phone          VARCHAR(20),
    grade_level    INT          NOT NULL CHECK (grade_level BETWEEN 6 AND 9),
    date_of_birth  DATE,
    avatar_url     VARCHAR(500),
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT now()
);

-- Gói dịch vụ bán cho học sinh
CREATE TABLE subscription_plan (
    plan_id          VARCHAR(36)   PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name             VARCHAR(100)  NOT NULL UNIQUE,
    description      TEXT,
    price            NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    duration_days    INT           NOT NULL CHECK (duration_days > 0),
    max_ai_sessions  INT,          -- null = không giới hạn
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE
);

-- Lần đăng ký gói của một học sinh
CREATE TABLE subscription (
    subscription_id  VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id       VARCHAR(36) NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    plan_id          VARCHAR(36) NOT NULL REFERENCES subscription_plan(plan_id),
    start_date       TIMESTAMP,
    end_date         TIMESTAMP,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED')),
    auto_renew       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP   NOT NULL DEFAULT now(),
    CHECK (end_date IS NULL OR start_date IS NULL OR end_date > start_date)
);

-- Giao dịch thanh toán (MoMo). orderId gửi MoMo = payment_id, transId MoMo trả về = transaction_code
CREATE TABLE payment (
    payment_id        VARCHAR(36)   PRIMARY KEY DEFAULT gen_random_uuid()::text,
    subscription_id   VARCHAR(36)   NOT NULL REFERENCES subscription(subscription_id) ON DELETE CASCADE,
    amount            NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    method            VARCHAR(30)   NOT NULL DEFAULT 'MOMO_WALLET',
    request_id        VARCHAR(64)   UNIQUE,
    transaction_code  VARCHAR(64)   UNIQUE,
    pay_url           VARCHAR(1000),
    result_code       INT,
    ipn_payload       JSONB,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    paid_at           TIMESTAMP,
    created_at        TIMESTAMP     NOT NULL DEFAULT now()
);

-- Thông báo gửi cho học sinh
CREATE TABLE notification (
    notification_id  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id       VARCHAR(36)  NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    payment_id       VARCHAR(36)  REFERENCES payment(payment_id) ON DELETE SET NULL,
    type             VARCHAR(20)  NOT NULL CHECK (type IN ('PAYMENT', 'ACHIEVEMENT', 'QUIZ', 'SYSTEM')),
    title            VARCHAR(255) NOT NULL,
    content          TEXT,
    is_read          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);

-- =====================================================================
--  MODULE B - Nội dung học
-- =====================================================================

-- Lộ trình học theo khối lớp
CREATE TABLE learning_path (
    path_id      VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name         VARCHAR(150) NOT NULL,
    grade_level  INT          NOT NULL CHECK (grade_level BETWEEN 6 AND 9),
    description  TEXT,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Chủ đề kiến thức
CREATE TABLE topic (
    topic_id     VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    path_id      VARCHAR(36)  NOT NULL REFERENCES learning_path(path_id) ON DELETE CASCADE,
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    order_index  INT          NOT NULL,
    CONSTRAINT uq_topic_path_order UNIQUE (path_id, order_index)
);

-- Bài giảng
CREATE TABLE lesson (
    lesson_id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    topic_id          VARCHAR(36)  NOT NULL REFERENCES topic(topic_id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    content           TEXT,
    video_url         VARCHAR(500),
    duration_minutes  INT,
    order_index       INT          NOT NULL,
    created_by        VARCHAR(36)  NOT NULL REFERENCES user_account(user_id),
    review_status     VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                      CHECK (review_status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'PUBLISHED', 'ARCHIVED')),
    reviewed_by       VARCHAR(36)  REFERENCES lecturer(lecturer_id) ON DELETE SET NULL,
    reviewed_at       TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_lesson_topic_order UNIQUE (topic_id, order_index)
);

-- File tài liệu đính kèm bài giảng (URL Cloudinary)
CREATE TABLE lesson_material (
    material_id  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    lesson_id    VARCHAR(36)  NOT NULL REFERENCES lesson(lesson_id) ON DELETE CASCADE,
    file_name    VARCHAR(255) NOT NULL,
    file_url     VARCHAR(1000) NOT NULL,
    file_type    VARCHAR(20)  NOT NULL CHECK (file_type IN ('PDF', 'IMAGE', 'VIDEO')),
    file_size    BIGINT,
    uploaded_at  TIMESTAMP    NOT NULL DEFAULT now()
);

-- Tiến độ học từng bài (trung gian STUDENT - LESSON)
CREATE TABLE lesson_progress (
    progress_id       VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id        VARCHAR(36) NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    lesson_id         VARCHAR(36) NOT NULL REFERENCES lesson(lesson_id) ON DELETE CASCADE,
    status            VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED'
                      CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    progress_percent  INT         NOT NULL DEFAULT 0 CHECK (progress_percent BETWEEN 0 AND 100),
    last_accessed_at  TIMESTAMP,
    completed_at      TIMESTAMP,
    CONSTRAINT uq_progress_student_lesson UNIQUE (student_id, lesson_id)
);

-- =====================================================================
--  MODULE C - Câu hỏi, Quiz & Điểm yếu
-- =====================================================================

-- Ngân hàng câu hỏi
CREATE TABLE question (
    question_id     VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    topic_id        VARCHAR(36) NOT NULL REFERENCES topic(topic_id) ON DELETE CASCADE,
    content         TEXT        NOT NULL,
    question_type   VARCHAR(20) NOT NULL CHECK (question_type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'SHORT_ANSWER')),
    options         JSONB,      -- ["A. ...", "B. ..."]
    correct_answer  VARCHAR(255) NOT NULL,
    explanation     TEXT,
    difficulty      VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    source          VARCHAR(10) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'AI')),
    created_by      VARCHAR(36) REFERENCES user_account(user_id) ON DELETE SET NULL,
    review_status   VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                    CHECK (review_status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED', 'PUBLISHED', 'ARCHIVED')),
    reviewed_by     VARCHAR(36) REFERENCES lecturer(lecturer_id) ON DELETE SET NULL,
    reviewed_at     TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT now()
);

-- Điểm yếu của học sinh ở một chủ đề
CREATE TABLE weakness (
    weakness_id    VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id     VARCHAR(36)  NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    topic_id       VARCHAR(36)  NOT NULL REFERENCES topic(topic_id) ON DELETE CASCADE,
    mastery_score  NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (mastery_score BETWEEN 0 AND 100),
    severity       VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH')),
    status         VARCHAR(20)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IMPROVING', 'RESOLVED')),
    detected_at    TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_weakness_student_topic UNIQUE (student_id, topic_id)
);

-- Bộ đề (có thể sinh từ một điểm yếu)
CREATE TABLE quiz (
    quiz_id             VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    weakness_id         VARCHAR(36)  REFERENCES weakness(weakness_id) ON DELETE SET NULL,
    title               VARCHAR(255) NOT NULL,
    quiz_type           VARCHAR(20)  NOT NULL DEFAULT 'PRACTICE'
                        CHECK (quiz_type IN ('PRACTICE', 'REINFORCEMENT', 'PLACEMENT')),
    time_limit_minutes  INT,
    generated_by        VARCHAR(10)  NOT NULL DEFAULT 'SYSTEM' CHECK (generated_by IN ('SYSTEM', 'AI')),
    created_at          TIMESTAMP    NOT NULL DEFAULT now()
);

-- Câu hỏi trong quiz (trung gian QUIZ - QUESTION)
CREATE TABLE quiz_question (
    quiz_question_id  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    quiz_id           VARCHAR(36)  NOT NULL REFERENCES quiz(quiz_id) ON DELETE CASCADE,
    question_id       VARCHAR(36)  NOT NULL REFERENCES question(question_id) ON DELETE CASCADE,
    order_index       INT          NOT NULL,
    points            NUMERIC(5,2) NOT NULL DEFAULT 1,
    CONSTRAINT uq_quiz_question UNIQUE (quiz_id, question_id)
);

-- Một lần làm quiz
CREATE TABLE quiz_attempt (
    attempt_id    VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id    VARCHAR(36)  NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    quiz_id       VARCHAR(36)  NOT NULL REFERENCES quiz(quiz_id) ON DELETE CASCADE,
    started_at    TIMESTAMP    NOT NULL DEFAULT now(),
    submitted_at  TIMESTAMP,
    score         NUMERIC(6,2),
    status        VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS'
                  CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED'))
);

-- Câu trả lời từng câu trong lần làm
CREATE TABLE attempt_answer (
    answer_id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    attempt_id        VARCHAR(36)  NOT NULL REFERENCES quiz_attempt(attempt_id) ON DELETE CASCADE,
    quiz_question_id  VARCHAR(36)  NOT NULL REFERENCES quiz_question(quiz_question_id) ON DELETE CASCADE,
    selected_answer   TEXT,
    is_correct        BOOLEAN      NOT NULL DEFAULT FALSE,
    earned_points     NUMERIC(5,2) NOT NULL DEFAULT 0,
    answered_at       TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_attempt_answer UNIQUE (attempt_id, quiz_question_id)
);

-- =====================================================================
--  MODULE D - Thảo luận đa tác tử AI
-- =====================================================================

-- Tác tử AI: gia sư hoặc phản biện
CREATE TABLE ai_agent (
    agent_id        VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name            VARCHAR(100) NOT NULL,
    agent_type      VARCHAR(10)  NOT NULL CHECK (agent_type IN ('TUTOR', 'CRITIC')),
    persona_prompt  TEXT,
    model_name      VARCHAR(100),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Phiên thảo luận về một bài giảng
CREATE TABLE discussion_session (
    session_id  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id  VARCHAR(36)  NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    lesson_id   VARCHAR(36)  NOT NULL REFERENCES lesson(lesson_id) ON DELETE CASCADE,
    title       VARCHAR(255),
    status      VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ENDED')),
    summary     TEXT,
    started_at  TIMESTAMP    NOT NULL DEFAULT now(),
    ended_at    TIMESTAMP
);

-- Tác tử tham gia phiên (trung gian SESSION - AI_AGENT)
CREATE TABLE session_agent (
    session_agent_id  VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    session_id        VARCHAR(36) NOT NULL REFERENCES discussion_session(session_id) ON DELETE CASCADE,
    agent_id          VARCHAR(36) NOT NULL REFERENCES ai_agent(agent_id),
    role_in_session   VARCHAR(10) NOT NULL CHECK (role_in_session IN ('TUTOR', 'CRITIC')),
    joined_at         TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_session_agent UNIQUE (session_id, agent_id)
);

-- Tin nhắn trong phiên (học sinh gửi thì session_agent_id = null)
CREATE TABLE message (
    message_id          VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    session_id          VARCHAR(36) NOT NULL REFERENCES discussion_session(session_id) ON DELETE CASCADE,
    session_agent_id    VARCHAR(36) REFERENCES session_agent(session_agent_id) ON DELETE SET NULL,
    reply_to_id         VARCHAR(36) REFERENCES message(message_id) ON DELETE SET NULL,
    sender_type         VARCHAR(10) NOT NULL CHECK (sender_type IN ('STUDENT', 'AGENT')),
    content             TEXT        NOT NULL,
    has_intended_error  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    CHECK ((sender_type = 'STUDENT' AND session_agent_id IS NULL) OR sender_type = 'AGENT')
);

-- =====================================================================
--  MODULE E - Thành tích & Giải bài qua ảnh
-- =====================================================================

CREATE TABLE achievement (
    achievement_id  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    icon_url        VARCHAR(500),
    criteria        TEXT,
    points          INT          NOT NULL DEFAULT 0
);

CREATE TABLE student_achievement (
    student_achievement_id  VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id              VARCHAR(36) NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    achievement_id          VARCHAR(36) NOT NULL REFERENCES achievement(achievement_id) ON DELETE CASCADE,
    earned_at               TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_achievement UNIQUE (student_id, achievement_id)
);

-- Bài toán học sinh chụp ảnh gửi lên
CREATE TABLE photo_problem (
    photo_problem_id  VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id        VARCHAR(36)  NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    topic_id          VARCHAR(36)  REFERENCES topic(topic_id) ON DELETE SET NULL,
    image_url         VARCHAR(1000) NOT NULL,
    extracted_text    TEXT,
    status            VARCHAR(10)  NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SOLVED', 'FAILED')),
    uploaded_at       TIMESTAMP    NOT NULL DEFAULT now()
);

-- Lời giải cho bài toán ảnh (1 : 0..1)
CREATE TABLE solution (
    solution_id       VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    photo_problem_id  VARCHAR(36)  NOT NULL UNIQUE REFERENCES photo_problem(photo_problem_id) ON DELETE CASCADE,
    final_answer      TEXT,
    steps             TEXT,
    generated_by      VARCHAR(100),
    created_at        TIMESTAMP    NOT NULL DEFAULT now()
);

-- =====================================================================
--  MODULE F - Duyệt nội dung & Báo lỗi
-- =====================================================================

-- Lịch sử duyệt: mỗi lần duyệt / từ chối là một dòng. Mỗi dòng trỏ đúng 1 trong (lesson, question)
CREATE TABLE content_review (
    review_id    VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    lecturer_id  VARCHAR(36) NOT NULL REFERENCES lecturer(lecturer_id) ON DELETE CASCADE,
    lesson_id    VARCHAR(36) REFERENCES lesson(lesson_id) ON DELETE CASCADE,
    question_id  VARCHAR(36) REFERENCES question(question_id) ON DELETE CASCADE,
    decision     VARCHAR(10) NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED')),
    comment      TEXT,
    reviewed_at  TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT ck_review_one_target CHECK (num_nonnulls(lesson_id, question_id) = 1)
);

-- Học sinh báo nội dung sai. Mỗi dòng trỏ đúng 1 đối tượng
CREATE TABLE content_report (
    report_id        VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    student_id       VARCHAR(36) NOT NULL REFERENCES student(student_id) ON DELETE CASCADE,
    lesson_id        VARCHAR(36) REFERENCES lesson(lesson_id) ON DELETE CASCADE,
    question_id      VARCHAR(36) REFERENCES question(question_id) ON DELETE CASCADE,
    message_id       VARCHAR(36) REFERENCES message(message_id) ON DELETE CASCADE,
    solution_id      VARCHAR(36) REFERENCES solution(solution_id) ON DELETE CASCADE,
    reason           VARCHAR(30) NOT NULL CHECK (reason IN ('WRONG_ANSWER', 'WRONG_EXPLANATION', 'TYPO', 'OTHER')),
    description      TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED')),
    handled_by       VARCHAR(36) REFERENCES lecturer(lecturer_id) ON DELETE SET NULL,
    resolution_note  TEXT,
    created_at       TIMESTAMP   NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMP,
    CONSTRAINT ck_report_one_target CHECK (num_nonnulls(lesson_id, question_id, message_id, solution_id) = 1)
);

-- =====================================================================
--  INDEX cho các truy vấn hay dùng
-- =====================================================================
CREATE INDEX idx_subscription_student   ON subscription(student_id, status);
CREATE INDEX idx_payment_subscription   ON payment(subscription_id);
CREATE INDEX idx_notification_student   ON notification(student_id, is_read);
CREATE INDEX idx_topic_path             ON topic(path_id);
CREATE INDEX idx_lesson_topic_status    ON lesson(topic_id, review_status);
CREATE INDEX idx_progress_student       ON lesson_progress(student_id);
CREATE INDEX idx_question_topic_status  ON question(topic_id, review_status);
CREATE INDEX idx_attempt_student        ON quiz_attempt(student_id);
CREATE INDEX idx_session_student        ON discussion_session(student_id);
CREATE INDEX idx_message_session        ON message(session_id, created_at);
CREATE INDEX idx_photo_student          ON photo_problem(student_id);
CREATE INDEX idx_report_status          ON content_report(status);
