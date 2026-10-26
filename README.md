# MAEL Backend - Multi-Agent Edtech Learning

Web học Toán THCS cùng 3 AI (Thầy AI + 2 bạn AI phản biện). BE viết bằng **Spring Boot 3.5 + PostgreSQL**.

## Yêu cầu
- JDK 17 trở lên
- Maven 3.9+ (hoặc mở bằng IntelliJ, IntelliJ tự có Maven)
- PostgreSQL 13+ (đã tạo database `mael`)

## Chạy lần đầu

### 1. Tạo bảng và dữ liệu mẫu: Flyway tự làm
Chỉ cần tạo database rỗng tên `mael` trong pgAdmin. Khi BE khởi động, Flyway tự chạy các file trong
`src/main/resources/db/migration` theo thứ tự, **mỗi file đúng 1 lần** (ghi lại trong bảng `flyway_schema_history`):
1. `V1__schema.sql`: toàn bộ bảng
2. `V2__seed_demo.sql`: lộ trình lớp 6-9, bài giảng, câu hỏi, 3 AI agent, tài khoản demo, bài viết trang chủ

- DB `mael` cũ tạo bằng tay (có bảng nhưng chưa có `flyway_schema_history`): lần chạy đầu BE tự xoá sạch và tạo lại (log có dòng "Phát hiện DB cũ..."). Muốn giữ DB cũ thì đặt `FLYWAY_RESET_LEGACY=false`.
- Thay đổi cấu trúc DB về sau: **không sửa V1, V2** mà tạo file mới `V3__mo_ta.sql`, `V4__...`. Flyway chỉ chạy file mới.

### 2. Cấu hình `.env`
Mọi key và mật khẩu nằm trong file `.env` ở thư mục gốc (Spring đọc qua `spring.config.import`, không cần thư viện thêm).
```bash
cp .env.example .env
```
Điền `DB_PASSWORD`, `JWT_SECRET` (tạo bằng `openssl rand -base64 48`), sau này thêm `OPENAI_API_KEY`, key MoMo, Cloudinary, Gmail.
File `.env` đã nằm trong `.gitignore`, chỉ `.env.example` được commit.

Khi chạy trong IntelliJ, để **Working directory** là thư mục gốc project (mặc định đã vậy) thì mới đọc được `.env`.

### 3. Chạy
```bash
mvn spring-boot:run
```
hoặc trong IntelliJ chạy `MaelApplication`. BE chạy ở http://localhost:8089 (đổi bằng `SERVER_PORT` trong `.env`)

- Swagger UI: http://localhost:8089/swagger-ui.html (bấm **Authorize**, dán token lấy từ `/api/auth/login`)

### Tài khoản demo (mật khẩu `123456`)
| Email | Vai trò |
|---|---|
| hocsinh@mael.vn | Học sinh lớp 7, đang dùng thử 7 ngày |
| lop9@mael.vn | Học sinh lớp 9, đã qua 2 ải đầu Chương I (bản đồ ải Học kỳ 1) |
| giangvien@mael.vn | Giảng viên duyệt nội dung |
| admin@mael.vn | Quản trị viên |

## Cấu trúc
```
src/main/java/com/mael
├── config        SecurityConfig (JWT, CORS, phân quyền), WebConfig, OpenApiConfig
├── security      JwtService, JwtAuthenticationFilter, UserPrincipal, CurrentUser
├── entity        entity map 1-1 với bảng trong db/migration/V1__schema.sql (+ enums)
├── repository    Spring Data JPA
├── dto           Request / Response (record)
├── service       Nghiệp vụ
│   ├── ai        AiOrchestrator (interface) + MockAiOrchestrator
│   ├── photo     PhotoSolver (interface) + MockPhotoSolver
│   ├── payment   PaymentGateway (interface) + MockPaymentGateway
│   └── storage   FileStorageService (lưu ảnh local ở thư mục uploads/)
├── controller    REST API
└── exception     ApiException + GlobalExceptionHandler (lỗi trả JSON tiếng Việt)
```
`spring.jpa.hibernate.ddl-auto=validate`: schema do script SQL tạo, Hibernate chỉ kiểm tra entity khớp bảng.

## API chính
| Nhóm | Endpoint |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` |
| Trang chủ / lộ trình | `GET /api/dashboard`, `GET /api/learning-paths`, `GET /api/learning-paths/me`, `GET /api/learning-paths/{id}` |
| Bài giảng | `GET /api/lessons/{id}`, `PUT /api/lessons/{id}/progress` |
| Phòng học 3 AI | `POST /api/discussions`, `GET /api/discussions`, `GET /api/discussions/{id}`, `POST /api/discussions/{id}/messages`, `POST /api/discussions/{id}/messages/{msgId}/flag-error`, `POST /api/discussions/{id}/end` |
| Giải toán bằng ảnh | `POST /api/photo-problems` (multipart: `image`, `note`), `GET /api/photo-problems`, `GET /api/photo-problems/{id}` |
| Quiz & điểm yếu | `GET /api/quizzes`, `POST /api/quizzes/{id}/attempts`, `POST /api/attempts/{id}/submit`, `GET /api/weaknesses` |
| Gói học | `GET /api/plans`, `GET /api/subscriptions/me`, `GET /api/subscriptions/history` |
| Thanh toán | `POST /api/payments`, `GET /api/payments/{id}`, `POST /api/payments/{id}/mock-confirm` (dev), `POST /api/payments/momo/ipn` (TODO) |
| Học sinh | `GET /api/notifications`, `PATCH /api/notifications/{id}/read`, `GET /api/achievements`, `POST /api/reports` |
| Giảng viên | `GET /api/lecturer/lessons/pending`, `GET /api/lecturer/questions/pending`, `POST /api/lecturer/lessons/{id}/review`, `POST /api/lecturer/questions/{id}/review`, `GET /api/lecturer/reports`, `PATCH /api/lecturer/reports/{id}` |
| Admin | `POST /api/admin/lessons`, `POST /api/admin/lessons/{id}/submit`, `POST /api/admin/questions`, `POST /api/admin/questions/{id}/submit` |

## Phần chờ tích hợp
- **AI (OpenAI)**: tạo class implements `AiOrchestrator` và `PhotoSolver`, đổi `AI_PROVIDER=openai` trong `.env`. Mock hiện tại cho Tuấn Kiệt cố ý nói sai (lưu `message.has_intended_error`) để test nút "Bạn ấy nói sai!".
- **MoMo**: tạo class implements `PaymentGateway` (tạo đơn captureWallet, ký HMAC-SHA256), xử lý IPN trong `PaymentController.momoIpn` rồi gọi `paymentService.markSuccess(...)`. Đổi `PAYMENT_PROVIDER=momo`, `PAYMENT_MOCK_CONFIRM_ENABLED=false` trong `.env`; các key MoMo đọc từ `app.momo.*`.
- **Cloudinary / SMTP**: thay phần ghi file trong `FileStorageService`; gửi mail ở TODO trong `AdminContentService.submitLessonForReview`.

## Lưu ý
- Máy ở VN hay có timezone `Asia/Saigon` mà một số bản PostgreSQL báo lỗi `invalid value for parameter "TimeZone"`. `MaelApplication` đã đặt sẵn `Asia/Ho_Chi_Minh` nên không gặp lỗi này.

## Bản đồ ải (gamification) - Toán 9 Học kỳ 1
- Mỗi bài học = 1 ải (lý thuyết + 3 câu thử thách), cuối mỗi chương có **ải trùm** 5 câu ngẫu nhiên, thắng mở rương báu (thú hiếm + 50 xu).
- Sao: đúng hết = 3 sao, >= 2/3 = 2 sao, >= 1/3 = 1 sao (qua ải). Xu: 10 xu / sao mới. XP: 5 / câu đúng, +20 khi qua ải lần đầu, 100 XP lên 1 cấp.
- Nội dung 5 chương Toán 9 HK1 (SGK Kết nối tri thức, Tập 1) nằm trong `db/migration/V2__seed_demo.sql`: 17 bài, 51 câu hỏi, mỗi bài có `misconceptions` (lỗi sai hay gặp). **Nhờ giảng viên chuyên môn duyệt lại trước khi dùng thật.**
- API: `/api/game/map`, `/api/game/stages/{lessonId}`, `/api/game/bosses/{topicId}`, `/api/game/pets`, `/api/game/onboarding`.
- Học sinh mới đăng ký có `onboarded = false`, FE đưa vào màn chào mừng 3 bước (lớp, mục tiêu, thú đồng hành).

## Bạn AI phản biện
`MockAiOrchestrator` chỉ cho Tuấn Kiệt "cố ý sai" trong danh sách `lesson.misconceptions` của đúng bài đang học (không lạc đề), và gắn các nút trả lời nhanh (`message.quick_replies`) để học sinh bấm chọn thay vì gõ.

## Giọng đọc 3 AI
Điền `GOOGLE_TTS_API_KEY` (Google Cloud, bật *Cloud Text-to-Speech API*) để mỗi AI đọc bằng một giọng Wavenet riêng (cấu hình ở cột `voice_name`, `voice_pitch`, `voice_rate` của bảng `ai_agent`). Để trống thì FE dùng giọng tiếng Việt của trình duyệt, vẫn khác cao độ / tốc độ cho từng AI.

## Gói học
`subscription_plan.audience`: `STUDENT` (mua qua MoMo), `CENTER` / `SCHOOL` (giá theo học sinh / tháng, gửi form tư vấn vào bảng `org_lead`, admin xem ở `GET /api/admin/leads`). Trang bảng giá công khai: `http://localhost:5173/plans`.

## Tích hợp AI Gemini (4 key cho 4 tác vụ)
Đặt `AI_PROVIDER=gemini` và 4 key trong `.env`:

| Biến | Tác vụ |
|------|--------|
| `GEMINI_KEY_DISCUSSION` | 3 AI trả lời khi học sinh tự gõ câu hỏi, tóm tắt buổi thảo luận |
| `GEMINI_KEY_ANALYSIS` | Phân tích cá nhân hoá (thẻ "Thầy Trí nhận xét" trên bản đồ) |
| `GEMINI_KEY_DOCUMENT` | Đọc PDF giảng viên tải lên, soạn lý thuyết + lỗi sai hay gặp + câu hỏi cho ải / Đấu trường |
| `GEMINI_KEY_PHOTO` | Giải toán từ ảnh chụp |

- Model mặc định `gemini-3.1-flash-lite`, không có thì tự dùng `gemini-2.5-flash-lite` (đổi bằng `GEMINI_MODEL`, `GEMINI_FALLBACK_MODEL`).
- Key nào hết lượt (429) thì tự thử key còn lại. Gemini lỗi thì hệ thống dùng kịch bản / luật dự phòng, web không bị đứng.
- Phần chấm đúng / sai trong phòng thảo luận luôn dựng từ dữ liệu giảng viên đã duyệt (lỗi sai hay gặp + câu luyện tập của bài), AI chỉ trả lời câu hỏi tự do.

## Thể lực, chuỗi ngày, Đấu trường
- Thể lực tối đa 5, mỗi lần nộp thử thách ải / ải trùm / vào Đấu trường tốn 1, hồi 1 điểm mỗi 30 phút.
- Chuỗi ngày: ngày nào qua ải hoặc chơi Đấu trường thì +1. Khiên giữ chuỗi (60 xu) tự dùng khi lỡ 1 ngày.
- Đấu trường: 10 câu ngắn, 2 phút, 3 tim, combo. Xu từ Đấu trường tối đa 40 xu / ngày.
- 29 huy hiệu có thanh tiến độ (bảng `achievement`, cột `metric` + `target`), mở huy hiệu được thưởng xu.

## Chống spam
- Form đăng ký và form tư vấn trung tâm / trường học có captcha hình (`GET /api/captcha`), tắt bằng `CAPTCHA_ENABLED=false` khi test Postman.
- Tư vấn: tối đa 2 yêu cầu / số điện thoại / 24 giờ, 5 yêu cầu / IP / 24 giờ, có ô bẫy bot, chặn link trong ghi chú.
- Đăng nhập sai 8 lần / 15 phút (theo IP + email) thì khoá tạm. Đăng ký tối đa 5 tài khoản / IP / giờ.

## Tài liệu PDF mẫu cho giảng viên
Thư mục `docs/tai-lieu-mau/`: Toán 9 Đại số Chương I, II, mỗi bài 1 file (lý thuyết, ví dụ, lỗi hay gặp, bài tự luyện có đáp án)
và 2 file gộp theo chương. Đăng nhập `giangvien@mael.vn` → **Tài liệu PDF** → tải lên để AI soạn câu hỏi.
