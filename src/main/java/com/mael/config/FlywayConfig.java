package com.mael.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Flyway chạy migration khi BE khởi động.
 *
 * Trường hợp đặc biệt: database "mael" trước đây được tạo bằng cách chạy tay các file SQL cũ trong thư mục database/
 * (đã có bảng nhưng chưa có bảng lịch sử flyway_schema_history). Flyway sẽ báo lỗi "schema không rỗng".
 * Khi app.flyway.reset-legacy = true, BE xoá sạch schema cũ một lần rồi để Flyway tạo lại từ V1, V2.
 * Từ lần chạy sau đã có flyway_schema_history nên không bao giờ xoá nữa.
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(@Value("${app.flyway.reset-legacy:true}") boolean resetLegacy) {
        return flyway -> {
            if (resetLegacy && isLegacyDatabase(flyway.getConfiguration().getDataSource())) {
                log.warn("Phát hiện DB cũ tạo bằng tay (chưa có flyway_schema_history). Xoá schema cũ và tạo lại bằng Flyway (chỉ 1 lần).");
                Flyway.configure()
                        .configuration(flyway.getConfiguration())
                        .cleanDisabled(false)
                        .load()
                        .clean();
            }
            flyway.migrate();
        };
    }

    /** Có bảng user_account nhưng chưa có flyway_schema_history */
    private static boolean isLegacyDatabase(DataSource dataSource) {
        try (Connection c = dataSource.getConnection()) {
            String schema = c.getSchema() == null ? "public" : c.getSchema();
            return tableExists(c, schema, "user_account") && !tableExists(c, schema, "flyway_schema_history");
        } catch (Exception e) {
            log.warn("Không kiểm tra được DB cũ: {}", e.getMessage());
            return false;
        }
    }

    private static boolean tableExists(Connection c, String schema, String table) throws Exception {
        try (ResultSet rs = c.getMetaData().getTables(null, schema, table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
