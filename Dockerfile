# ==== Bước 1: build file jar bằng Maven ====
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
# Tải thư viện trước (cache), chỉ tải lại khi pom.xml đổi
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ==== Bước 2: chạy bằng JRE gọn nhẹ ====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Thư mục lưu file PDF / ảnh tải lên (gắn Render Disk vào /app/uploads nếu muốn giữ file lâu dài)
RUN mkdir -p /app/uploads
ENV UPLOAD_DIR=/app/uploads
# Giới hạn RAM theo máy Render (512MB vẫn chạy được), múi giờ Việt Nam
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Duser.timezone=Asia/Ho_Chi_Minh"
EXPOSE 8089
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
