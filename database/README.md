# Database

Không còn chạy SQL bằng tay. Các script nằm ở `src/main/resources/db/migration` và được **Flyway** tự chạy khi BE khởi động:

- `V1__schema.sql`: tạo bảng
- `V2__seed_demo.sql`: dữ liệu mẫu

Chỉ cần tạo database rỗng `mael`. Đổi cấu trúc DB thì tạo file mới `V3__mo_ta.sql` (không sửa V1, V2 đã chạy).
