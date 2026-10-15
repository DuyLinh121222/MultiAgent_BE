# MAEL Backend - Multi-Agent Edtech Learning

Web học Toán THCS cùng 3 AI (Thầy AI + 2 bạn AI phản biện). BE viết bằng **Spring Boot 3.5 + PostgreSQL**.

## Yêu cầu
- JDK 17 trở lên
- Maven 3.9+ (hoặc mở bằng IntelliJ, IntelliJ tự có Maven)
- PostgreSQL 13+ (đã tạo database `mael`)

## Chạy lần đầu

### 1. Tạo bảng và dữ liệu mẫu
Trong pgAdmin: chuột phải database `mael` → **Query Tool** → mở và chạy lần lượt:
1. `database/01_schema.sql` (28 bảng theo Logical ERD, chạy lại được nhiều lần)
2. `database/02_seed.sql` (lộ trình lớp 6-9, bài giảng, câu hỏi, 3 AI agent, tài khoản demo)

Hoặc dùng psql:
```bash
psql -U postgres -d mael -f database/01_schema.sql
psql -U postgres -d mael -f database/02_seed.sql
```

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
| giangvien@mael.vn | Giảng viên duyệt nội dung |
| admin@mael.vn | Quản trị viên |

## Cấu trúc
```
src/main/java/com/mael
├── config        SecurityConfig (JWT, CORS, phân quyền), WebConfig, OpenApiConfig
├── security      JwtService, JwtAuthenticationFilter, UserPrincipal, CurrentUser
├── entity        28 entity map 1-1 với bảng trong 01_schema.sql (+ enums)
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
