package com.mael;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MaelApplication {

    public static void main(String[] args) {
        // Máy ở VN hay có timezone mặc định "Asia/Saigon", một số bản PostgreSQL không nhận tên này
        // (lỗi: invalid value for parameter "TimeZone"). Đặt tên chuẩn trước khi kết nối DB.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(MaelApplication.class, args);
    }
}
